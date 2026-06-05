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

package io.prometheus.jmx.test.opentelemetry;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.NetworkSupport;
import io.prometheus.jmx.test.support.environment.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.paramixel.api.support.Retry;
import org.testcontainers.containers.Network;

public class CombinedModeWithSSLTest {

    private final OpenTelemetryTestEnvironment environment;
    private Network network;

    private CombinedModeWithSSLTest(OpenTelemetryTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        CombinedModeWithSSLTest.class.getName(),
                        OpenTelemetryTestEnvironment.createTestEnvironments(CombinedModeWithSSLTest.class),
                        environment -> Instance.builder(
                                        environment.exporterTestEnvironment().name(),
                                        () -> new CombinedModeWithSSLTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        CombinedModeWithSSLTest.class, CombinedModeWithSSLTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest
                                                                        ::testPrometheusProtobufMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusHasMetrics()",
                                                        withInstance(
                                                                CombinedModeWithSSLTest.class,
                                                                CombinedModeWithSSLTest::testPrometheusHasMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        CombinedModeWithSSLTest.class,
                                                        CombinedModeWithSSLTest::tearDown)))))
                .build();
    }

    public void setUp() throws Throwable {
        environment.exporterTestEnvironment().setBaseUrl("https://localhost");
        network = NetworkSupport.create();
        environment.initialize(network);
    }

    public void testHealthy() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void testPrometheusHasMetrics() throws Throwable {
        boolean isJmxExporterModeJavaStandalone =
                environment.exporterTestEnvironment().getJmxExporterMode() == JmxExporterMode.Standalone;

        boolean isKonaJdk8 =
                environment.exporterTestEnvironment().getJavaDockerImage().startsWith("konajdk/konajdk:8");

        for (String metricName : ExpectedMetricsNames.getMetricsNames().stream()
                .filter(name ->
                        !isJmxExporterModeJavaStandalone || (!name.startsWith("jvm_") && !name.startsWith("process_")))
                .filter(name -> !isKonaJdk8 || !name.equals("jvm_memory_pool_allocated_bytes_total"))
                .toList()) {
            Double value = getPrometheusMetric(metricName);

            assertThat(value).as("metricName [%s]", metricName).isNotNull();
            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
        }
    }

    public void tearDown() throws Throwable {
        environment.close();
        NetworkSupport.close(network);
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

        boolean isJmxExporterModeJavaAgent =
                environment.exporterTestEnvironment().getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName =
                environment.exporterTestEnvironment().getJmxExporterMode().getBuildInfoName();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
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
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

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

    private Double getPrometheusMetric(String metricName) throws Throwable {
        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(5)))
                .retryOn(t -> t instanceof RuntimeException)
                .run(() -> {
                    HttpResponse httpResponse = sendPrometheusQuery(metricName);

                    assertThat(httpResponse.statusCode()).isEqualTo(200);
                    assertThat(httpResponse.body()).isNotNull();
                    assertThat(httpResponse.body().string()).isNotNull();

                    if (httpResponse.body().string().contains(metricName)) {
                        return;
                    }

                    throw new RuntimeException("metric not found yet: " + metricName);
                });

        return result.isSuccessful() ? 1.0 : null;
    }

    private HttpResponse sendPrometheusQuery(String query) throws IOException {
        return sendRequest("/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    private HttpResponse sendRequest(String path) throws IOException {
        return HttpClient.sendRequest(environment.prometheusTestEnvironment().getPrometheusUrl(path));
    }
}
