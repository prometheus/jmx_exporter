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

package io.prometheus.jmx.test.core;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.IsolatorExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.jmx.test.support.util.Repeater;
import io.prometheus.jmx.test.support.util.TestSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Verifyica;
import org.verifyica.api.util.CleanupExecutor;

public class StartupDelayIsolatorTest {

    private static final int JAVA_AGENT_COUNT = 2;

    private static final int REPEAT_COUNT = 10;

    private static final long REPEAT_INTERVAL_MILLISECONDS = 1000;

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<IsolatorExporterTestEnvironment> arguments() {
        return IsolatorExporterTestEnvironment.createEnvironments();
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.getClassContext().getTestClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeIsolatorExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testHealthy(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment) throws Throwable {
        for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
            String url = isolatorExporterTestEnvironment.getUrl(test, JmxExporterPath.HEALTHY);

            new Repeater(REPEAT_COUNT)
                    .throttle(REPEAT_INTERVAL_MILLISECONDS)
                    .test(() -> {
                        HttpResponse httpResponse = HttpClient.sendRequest(url);
                        assertHealthyResponse(httpResponse);
                    })
                    .accept((iteration, throwable) -> {
                        if (throwable == null) {
                            Repeater.abort();
                        }
                    })
                    .run();
        }
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment)
            throws Throwable {
        for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
            String url = isolatorExporterTestEnvironment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = sendRequestWithRetry(url);

            assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
        }
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment)
            throws Throwable {
        for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
            String url = isolatorExporterTestEnvironment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = sendRequestWithRetry(
                    url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

            assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
        }
    }

    @Verifyica.Test
    public void testPrometheusTextMetrics(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment)
            throws Throwable {
        for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
            String url = isolatorExporterTestEnvironment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse =
                    sendRequestWithRetry(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

            assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
        }
    }

    @Verifyica.Test
    public void testPrometheusProtobufMetrics(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment)
            throws Throwable {
        for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
            String url = isolatorExporterTestEnvironment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = sendRequestWithRetry(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

            assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
        }
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        new CleanupExecutor()
                .addTask(() -> TestSupport.destroyIsolatorExporterTestEnvironment(argumentContext))
                .addTask(() -> TestSupport.destroyNetwork(argumentContext))
                .execute()
                .throwIfFailed();
    }

    private HttpResponse sendRequestWithRetry(String url) throws Throwable {
        return sendRequestWithRetry(url, null, null);
    }

    private HttpResponse sendRequestWithRetry(String url, String header, String value) throws Throwable {
        final HttpResponse[] responseHolder = new HttpResponse[1];

        new Repeater(REPEAT_COUNT)
                .throttle(REPEAT_INTERVAL_MILLISECONDS)
                .test(() -> {
                    HttpResponse httpResponse =
                            (header != null) ? HttpClient.sendRequest(url, header, value) : HttpClient.sendRequest(url);
                    responseHolder[0] = httpResponse;
                })
                .accept((iteration, throwable) -> {
                    if (throwable == null && responseHolder[0] != null) {
                        Repeater.abort();
                    }
                })
                .run();

        return responseHolder[0];
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
                .withLabel("name", "jmx_prometheus_javaagent")
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
                .withLabel("area", "nonheap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap");

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
    }
}
