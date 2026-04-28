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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.paramixel.core.Action;
import org.paramixel.core.ConsoleRunner;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.action.StrictSequential;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class StartupDelayIsolatorTest {

    private static final int JAVA_AGENT_COUNT = 2;

    private static final int REPEAT_COUNT = 10;

    private static final long REPEAT_INTERVAL_MILLISECONDS = 1000;

    private static class Attachment {
        public Network network;
        public IsolatorExporterTestEnvironment environment;

        public Attachment() {}
    }

    public static void main(String[] args) {
        ConsoleRunner.runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                StartupDelayIsolatorTest.class.getName(),
                IsolatorExporterTestEnvironment.createEnvironments()
                        .map(StartupDelayIsolatorTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
                String url = attachment.environment.getUrl(test, JmxExporterPath.HEALTHY);

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
        });

        Action testDefaultTextMetrics = Direct.of("testDefaultTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
                String url = attachment.environment.getUrl(test, JmxExporterPath.METRICS);

                HttpResponse httpResponse = sendRequestWithRetry(url);

                assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
            }
        });

        Action testOpenMetricsTextMetrics = Direct.of("testOpenMetricsTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
                String url = attachment.environment.getUrl(test, JmxExporterPath.METRICS);

                HttpResponse httpResponse = sendRequestWithRetry(
                        url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

                assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
            }
        });

        Action testPrometheusTextMetrics = Direct.of("testPrometheusTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
                String url = attachment.environment.getUrl(test, JmxExporterPath.METRICS);

                HttpResponse httpResponse = sendRequestWithRetry(
                        url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

                assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
            }
        });

        Action testPrometheusProtobufMetrics = Direct.of("testPrometheusProtobufMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            for (int test = 0; test < JAVA_AGENT_COUNT; test++) {
                String url = attachment.environment.getUrl(test, JmxExporterPath.METRICS);

                HttpResponse httpResponse = sendRequestWithRetry(
                        url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

                assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
            }
        });

        Action tests = StrictSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                isolatorExporterTestEnvironment.getName(),
                Direct.of("setUp", context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    isolatorExporterTestEnvironment.initialize(StartupDelayIsolatorTest.class, network);
                    Attachment attachment = new Attachment();
                    attachment.network = network;
                    attachment.environment = isolatorExporterTestEnvironment;
                    context.setAttachment(attachment);
                }),
                tests,
                Direct.of("tearDown", context -> {
                    Attachment attachment = context.removeAttachment()
                            .flatMap(a -> a.to(Attachment.class))
                            .orElse(null);

                    if (attachment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(attachment.network)
                                .runAndThrow();
                    }
                }));
    }

    private static HttpResponse sendRequestWithRetry(String url) throws IOException, Throwable {
        return sendRequestWithRetry(url, null, null);
    }

    private static HttpResponse sendRequestWithRetry(String url, String header, String value)
            throws IOException, Throwable {
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

    private static void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
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
