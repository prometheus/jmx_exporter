/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.AutoIncrementing;
import io.prometheus.jmx.BuildInfoMetrics;
import io.prometheus.jmx.CustomValue;
import io.prometheus.jmx.ExistDb;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.PerformanceMetrics;
import io.prometheus.jmx.StringValue;
import io.prometheus.jmx.TabularData;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DeveloperStressTest {

    private static final Duration TEST_DURATION = Duration.ofHours(1);
    private static final Duration TEST_STATISTICS_REPORT_INTERVAL = Duration.ofSeconds(10);

    private static final int CLIENT_COUNT = 50;
    private static final int CLIENT_MIN_SLEEP_MILLIS = 0;
    private static final int CLIENT_MAX_SLEEP_MILLIS = 1000;

    private static final String BASE_URL = "http://localhost:";
    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;
    private static final StressRequest[] STRESS_REQUESTS = {
        StressRequest.HEALTH,
        StressRequest.DEFAULT_METRICS,
        StressRequest.OPEN_METRICS_TEXT,
        StressRequest.PROMETHEUS_TEXT,
        StressRequest.PROMETHEUS_PROTOBUF
    };

    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong successCount = new AtomicLong();
    private final AtomicLong failureCount = new AtomicLong();
    private final AtomicLong assertionFailureCount = new AtomicLong();
    private final AtomicLong ioFailureCount = new AtomicLong();
    private final AtomicLong timeoutFailureCount = new AtomicLong();
    private final AtomicLong interruptedFailureCount = new AtomicLong();
    private final AtomicLong otherFailureCount = new AtomicLong();
    private final AtomicLong lastStatisticsRequestCount = new AtomicLong();
    private final AtomicLong lastStatisticsNanos = new AtomicLong();
    private final AtomicReference<Throwable> firstFailure = new AtomicReference<>();

    private HTTPServer httpServer;
    private String baseUrl;

    private DeveloperStressTest() {
        // Intentionally empty
    }

    public static void main(String[] args) throws Exception {
        DeveloperStressTest developerStressTest = new DeveloperStressTest();
        try {
            developerStressTest.setUp();
            developerStressTest.testStressMetrics();
        } finally {
            developerStressTest.tearDown();
        }
    }

    public void setUp() {
        try {
            String resource = (DeveloperStressTest.class.getName().replace(".", "/") + "/exporter.yaml");
            Path tempDirectory = Files.createTempDirectory("jmx-exporter-stress-test");
            File exporterYamlFile = tempDirectory.resolve("exporter.yaml").toFile();
            ResourceSupport.export(resource, exporterYamlFile);

            new TabularData().register();
            new AutoIncrementing().register();
            new ExistDb().register();
            new PerformanceMetrics().register();
            new CustomValue().register();
            new StringValue().register();

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);
            JvmMetrics.builder().register(DEFAULT_REGISTRY);
            new JmxCollector(exporterYamlFile).register(DEFAULT_REGISTRY);

            httpServer = HTTPServerFactory.createAndStartHTTPServer(DEFAULT_REGISTRY, exporterYamlFile);

            baseUrl = BASE_URL + httpServer.getPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up DeveloperStressTest", e);
        }
    }

    public void testStressMetrics() throws Exception {
        validateConfiguration();

        long startedNanos = System.nanoTime();
        long endNanos = startedNanos + TEST_DURATION.toNanos();
        long awaitSeconds = TEST_DURATION.getSeconds()
                + TimeUnit.MILLISECONDS.toSeconds(
                        HttpClient.CONNECT_TIMEOUT + HttpClient.WRITE_TIMEOUT + HttpClient.READ_TIMEOUT)
                + TimeUnit.MILLISECONDS.toSeconds(CLIENT_MAX_SLEEP_MILLIS)
                + 30;

        ExecutorService executorService = Executors.newFixedThreadPool(CLIENT_COUNT);
        ScheduledExecutorService statisticsReporter = null;
        try {
            for (int clientId = 0; clientId < CLIENT_COUNT; clientId++) {
                final int id = clientId;
                executorService.submit(() -> runClient(id, endNanos));
            }

            statisticsReporter = startStatisticsReporter(startedNanos, endNanos);

            executorService.shutdown();
            if (!executorService.awaitTermination(awaitSeconds, TimeUnit.SECONDS)) {
                AssertionError assertionError = new AssertionError("Stress test clients did not terminate promptly");
                firstFailure.compareAndSet(null, assertionError);
                executorService.shutdownNow();
                executorService.awaitTermination(30, TimeUnit.SECONDS);
            }
        } finally {
            if (statisticsReporter != null) {
                statisticsReporter.shutdownNow();
            }
            executorService.shutdownNow();
        }

        long finishedNanos = System.nanoTime();
        printSummary(startedNanos, finishedNanos);

        assertThat(firstFailure.get()).as("First stress test failure").isNull();
    }

    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    private void validateConfiguration() {
        assertThat(CLIENT_COUNT).as("CLIENT_COUNT").isPositive();
        assertThat(TEST_DURATION).as("DURATION").isPositive();
        assertThat(CLIENT_MIN_SLEEP_MILLIS).as("MIN_SLEEP_MILLIS").isNotNegative();
        assertThat(CLIENT_MAX_SLEEP_MILLIS).as("MAX_SLEEP_MILLIS").isGreaterThanOrEqualTo(CLIENT_MIN_SLEEP_MILLIS);
        assertThat(TEST_STATISTICS_REPORT_INTERVAL).as("STATISTICS_INTERVAL").isPositive();
    }

    private ScheduledExecutorService startStatisticsReporter(long startedNanos, long endNanos) {
        lastStatisticsRequestCount.set(requestCount.get());
        lastStatisticsNanos.set(startedNanos);

        ScheduledExecutorService statisticsReporter = Executors.newSingleThreadScheduledExecutor();
        statisticsReporter.scheduleAtFixedRate(
                () -> printCurrentStatistics(startedNanos, endNanos),
                TEST_STATISTICS_REPORT_INTERVAL.toNanos(),
                TEST_STATISTICS_REPORT_INTERVAL.toNanos(),
                TimeUnit.NANOSECONDS);
        return statisticsReporter;
    }

    private void printCurrentStatistics(long startedNanos, long endNanos) {
        long currentNanos = System.nanoTime();
        long previousNanos = lastStatisticsNanos.getAndSet(currentNanos);
        long currentRequestCount = requestCount.get();
        long previousRequestCount = lastStatisticsRequestCount.getAndSet(currentRequestCount);

        double elapsedSeconds = (currentNanos - startedNanos) / 1_000_000_000d;
        double remainingSeconds = Math.max(0d, (endNanos - currentNanos) / 1_000_000_000d);
        double cumulativeRequestsPerSecond = elapsedSeconds == 0 ? 0 : currentRequestCount / elapsedSeconds;
        double intervalSeconds = (currentNanos - previousNanos) / 1_000_000_000d;
        double intervalRequestsPerSecond =
                intervalSeconds == 0 ? 0 : (currentRequestCount - previousRequestCount) / intervalSeconds;

        System.out.println(String.format(
                "elapsed=%.1fs remaining=%.1fs requests=%d successes=%d failures=%d assertionFailures=%d ioFailures=%d timeoutFailures=%d interruptedFailures=%d otherFailures=%d cumulativeRps=%.1f intervalRps=%.1f",
                elapsedSeconds,
                remainingSeconds,
                currentRequestCount,
                successCount.get(),
                failureCount.get(),
                assertionFailureCount.get(),
                ioFailureCount.get(),
                timeoutFailureCount.get(),
                interruptedFailureCount.get(),
                otherFailureCount.get(),
                cumulativeRequestsPerSecond,
                intervalRequestsPerSecond));
    }

    private void runClient(int clientId, long endNanos) {
        long clientRequestCount = 0;
        while (System.nanoTime() < endNanos) {
            long requestNumber = requestCount.incrementAndGet();
            StressRequest stressRequest = nextStressRequest(clientId, clientRequestCount);
            try {
                HttpResponse httpResponse = sendStressRequest(stressRequest);
                assertStressResponse(httpResponse, stressRequest);
                successCount.incrementAndGet();

                clientRequestCount++;
                sleepBetweenRequests();
            } catch (Throwable t) {
                recordFailure(clientId, requestNumber, stressRequest, t);
                return;
            }
        }
    }

    private void recordFailure(int clientId, long requestNumber, StressRequest stressRequest, Throwable throwable) {
        failureCount.incrementAndGet();
        classifyFailure(throwable);
        firstFailure.compareAndSet(
                null,
                new AssertionError(
                        "Client " + clientId + " failed on request " + requestNumber + " (" + stressRequest + ")",
                        throwable));
        if (throwable instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private void classifyFailure(Throwable throwable) {
        if (throwable instanceof InterruptedException) {
            interruptedFailureCount.incrementAndGet();
        } else if (throwable instanceof SocketTimeoutException || throwable instanceof InterruptedIOException) {
            timeoutFailureCount.incrementAndGet();
        } else if (throwable instanceof IOException) {
            ioFailureCount.incrementAndGet();
        } else if (throwable instanceof AssertionError) {
            assertionFailureCount.incrementAndGet();
        } else {
            otherFailureCount.incrementAndGet();
        }
    }

    private StressRequest nextStressRequest(int clientId, long clientRequestCount) {
        int requestIndex = Math.floorMod(clientId + clientRequestCount, STRESS_REQUESTS.length);
        return STRESS_REQUESTS[requestIndex];
    }

    private HttpResponse sendStressRequest(StressRequest stressRequest) throws IOException {
        if (stressRequest == StressRequest.HEALTH) {
            return HttpClient.sendRequest(baseUrl + JmxExporterPath.HEALTHY);
        }

        String url = baseUrl + JmxExporterPath.METRICS;
        String acceptHeaderValue = stressRequest.acceptHeaderValue();
        if (acceptHeaderValue == null) {
            return HttpClient.sendRequest(url);
        }

        return HttpClient.sendRequest(url, HttpHeader.ACCEPT, acceptHeaderValue);
    }

    private void assertStressResponse(HttpResponse httpResponse, StressRequest stressRequest) {
        if (stressRequest == StressRequest.HEALTH) {
            assertHealthyResponse(httpResponse);
        } else {
            assertMetricsResponse(httpResponse, stressRequest.expectedContentType());
        }
    }

    private void sleepBetweenRequests() throws InterruptedException {
        if (CLIENT_MAX_SLEEP_MILLIS == 0) {
            return;
        }

        int sleepMillis = ThreadLocalRandom.current().nextInt(CLIENT_MIN_SLEEP_MILLIS, CLIENT_MAX_SLEEP_MILLIS + 1);
        if (sleepMillis > 0) {
            Thread.sleep(sleepMillis);
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        Set<String> compositeNameSet = new HashSet<>();
        MetricsParser.parseCollection(httpResponse).forEach(metric -> {
            String name = metric.name();
            Map<String, String> labels = metric.labels();
            String compositeName = name + " " + labels;
            assertThat(compositeNameSet).doesNotContain(compositeName);
            compositeNameSet.add(compositeName);
            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
        });

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", "unknown")
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.COUNTER)
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions")
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps")
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred")
                .withValue(6.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "1")
                .withLabel("key_path", "/db/query1.xq")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "2")
                .withLabel("key_path", "/db/query2.xq")
                .isPresent();

        boolean hasJavaMetrics = false;
        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("java_lang_")) {
                hasJavaMetrics = true;
                break;
            }
        }
        assertThat(hasJavaMetrics).as("No java_lang_* metrics found").isTrue();

        boolean hasJvmMetrics = false;
        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("jvm_")) {
                hasJvmMetrics = true;
                break;
            }
        }
        assertThat(hasJvmMetrics).as("No jvm_* metrics found").isTrue();
    }

    private void printSummary(long startedNanos, long finishedNanos) {
        double elapsedSeconds = (finishedNanos - startedNanos) / 1_000_000_000d;
        double requestsPerSecond = elapsedSeconds == 0 ? 0 : requestCount.get() / elapsedSeconds;
        Throwable failure = firstFailure.get();

        System.out.println("DeveloperStressTest summary");
        System.out.println("  clients: " + CLIENT_COUNT);
        System.out.println("  duration: " + TEST_DURATION);
        System.out.println("  sleep millis: " + CLIENT_MIN_SLEEP_MILLIS + "-" + CLIENT_MAX_SLEEP_MILLIS);
        System.out.println("  statistics interval: " + TEST_STATISTICS_REPORT_INTERVAL);
        System.out.println("  requests: " + requestCount.get());
        System.out.println("  successes: " + successCount.get());
        System.out.println("  failures: " + failureCount.get());
        System.out.println("  I/O failures: " + ioFailureCount.get());
        System.out.println("  timeout failures: " + timeoutFailureCount.get());
        System.out.println("  assertion failures: " + assertionFailureCount.get());
        System.out.println("  interrupted failures: " + interruptedFailureCount.get());
        System.out.println("  other failures: " + otherFailureCount.get());
        System.out.println("  elapsed seconds: " + elapsedSeconds);
        System.out.println("  requests/second: " + requestsPerSecond);
        if (failure != null) {
            System.out.println("  first failure: " + failure.getClass().getName() + ": " + failure.getMessage());
        }
    }

    private enum StressRequest {
        HEALTH(null, null),
        DEFAULT_METRICS(MetricsContentType.DEFAULT, null),
        OPEN_METRICS_TEXT(
                MetricsContentType.OPEN_METRICS_TEXT_METRICS, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString()),
        PROMETHEUS_TEXT(
                MetricsContentType.PROMETHEUS_TEXT_METRICS, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString()),
        PROMETHEUS_PROTOBUF(
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

        private final MetricsContentType expectedContentType;
        private final String acceptHeaderValue;

        StressRequest(MetricsContentType expectedContentType, String acceptHeaderValue) {
            this.expectedContentType = expectedContentType;
            this.acceptHeaderValue = acceptHeaderValue;
        }

        private MetricsContentType expectedContentType() {
            return expectedContentType;
        }

        private String acceptHeaderValue() {
            return acceptHeaderValue;
        }
    }
}
