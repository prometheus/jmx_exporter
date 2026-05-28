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

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Lifecycle;
import org.paramixel.api.action.Parallel;
import org.paramixel.api.action.Spec;
import org.paramixel.api.support.Retry;

public class StartupDelayTest {

    private final JmxExporterTestEnvironment environment;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Spec<?> factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(StartupDelayTest.class).stream()
                .filter(env -> env.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                .collect(Collectors.toList());

        return Parallel.of(StartupDelayTest.class.getName())
                .each(
                        environments,
                        environment -> Instance.of(environment.name(), () -> new StartupDelayTest(environment))
                                .child(Lifecycle.<StartupDelayTest>of("lifecycle")
                                        .before("setUp()", StartupDelayTest::setUp)
                                        .child("testHealthy()", StartupDelayTest::testHealthy)
                                        .child("testDefaultTextMetrics()", StartupDelayTest::testDefaultTextMetrics)
                                        .child(
                                                "testOpenMetricsTextMetrics()",
                                                StartupDelayTest::testOpenMetricsTextMetrics)
                                        .child(
                                                "testPrometheusTextMetrics()",
                                                StartupDelayTest::testPrometheusTextMetrics)
                                        .child(
                                                "testPrometheusProtobufMetrics()",
                                                StartupDelayTest::testPrometheusProtobufMetrics)
                                        .after("tearDown()", StartupDelayTest::tearDown)));
    }

    private StartupDelayTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        environment.initialize();
    }

    public void testHealthy() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);

        Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                .runAndThrow(() -> {
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertHealthyResponse(httpResponse);
                });
    }

    public void testDefaultTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse = sendRequestWithRetry(url);

        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                sendRequestWithRetry(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                sendRequestWithRetry(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                sendRequestWithRetry(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void tearDown() {
        environment.close();
    }

    private HttpResponse sendRequestWithRetry(String url) throws Throwable {
        return sendRequestWithRetry(url, null, null);
    }

    private HttpResponse sendRequestWithRetry(String url, String header, String value) throws Throwable {
        final HttpResponse[] responseHolder = new HttpResponse[1];

        Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                .retryOn(t -> t instanceof Exception)
                .runAndThrow(() -> {
                    responseHolder[0] =
                            (header != null) ? HttpClient.sendRequest(url, header, value) : HttpClient.sendRequest(url);
                });

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
}
