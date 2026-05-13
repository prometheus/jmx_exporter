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

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.environment.PrometheusTestEnvironment;
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
import java.util.stream.Collectors;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Container;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.paramixel.core.support.Retry;
import org.testcontainers.containers.Network;

public class CombinedModeTest {

    private static final String NETWORK_KEY = "network";

    private static final String JMX_EXPORTER_TEST_ENVIRONMENT = "jmxExporterTestEnvironment";

    private static final String PROMETHEUS_TEST_ENVIRONMENT = "prometheusTestEnvironment";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(CombinedModeTest.class.getName());
        for (OpenTelemetryTestEnvironment environment :
                OpenTelemetryTestEnvironment.createEnvironments().toList()) {
            parallelBuilder.child(argument(environment));
        }
        return parallelBuilder.build();
    }

    private static Action argument(OpenTelemetryTestEnvironment openTelemetryTestEnvironment) {
        Action setUp = setUp(openTelemetryTestEnvironment);
        Action testHealthy = testHealthy();
        Action testDefaultTextMetrics = testDefaultTextMetrics();
        Action testOpenMetricsTextMetrics = testOpenMetricsTextMetrics();
        Action testPrometheusTextMetrics = testPrometheusTextMetrics();
        Action testPrometheusProtobufMetrics = testPrometheusProtobufMetrics();
        Action testPrometheusHasMetrics = testPrometheusHasMetrics();
        Action tearDown = tearDown();

        return Container.builder(openTelemetryTestEnvironment.getName())
                .before(setUp)
                .child(testHealthy)
                .child(testDefaultTextMetrics)
                .child(testOpenMetricsTextMetrics)
                .child(testPrometheusTextMetrics)
                .child(testPrometheusProtobufMetrics)
                .child(testPrometheusHasMetrics)
                .after(tearDown)
                .build();
    }

    private static Action setUp(OpenTelemetryTestEnvironment openTelemetryTestEnvironment) {
        return Direct.builder("setUp")
                .runnable(context -> {
                    JmxExporterTestEnvironment jmxExporterTestEnvironment =
                            openTelemetryTestEnvironment.exporterTestEnvironment();
                    PrometheusTestEnvironment prometheusTestEnvironment =
                            openTelemetryTestEnvironment.prometheusTestEnvironment();
                    Network network = Network.newNetwork();
                    network.getId();
                    prometheusTestEnvironment.initialize(CombinedModeTest.class, network);
                    prometheusTestEnvironment.waitForReady();
                    jmxExporterTestEnvironment.initialize(CombinedModeTest.class, network);
                    var store = context.getStore();
                    store.put(NETWORK_KEY, network);
                    store.put(JMX_EXPORTER_TEST_ENVIRONMENT, jmxExporterTestEnvironment);
                    store.put(PROMETHEUS_TEST_ENVIRONMENT, prometheusTestEnvironment);
                })
                .build();
    }

    private static Action testHealthy() {
        return Direct.builder("testHealthy")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    String url = currentJmxExporterTestEnvironment.getUrl(JmxExporterPath.HEALTHY);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertHealthyResponse(httpResponse);
                })
                .build();
    }

    private static Action testDefaultTextMetrics() {
        return Direct.builder("testDefaultTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    String url = currentJmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertMetricsResponse(currentJmxExporterTestEnvironment, httpResponse, MetricsContentType.DEFAULT);
                })
                .build();
    }

    private static Action testOpenMetricsTextMetrics() {
        return Direct.builder("testOpenMetricsTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    String url = currentJmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
                    assertMetricsResponse(
                            currentJmxExporterTestEnvironment,
                            httpResponse,
                            MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusTextMetrics() {
        return Direct.builder("testPrometheusTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    String url = currentJmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
                    assertMetricsResponse(
                            currentJmxExporterTestEnvironment,
                            httpResponse,
                            MetricsContentType.PROMETHEUS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusProtobufMetrics() {
        return Direct.builder("testPrometheusProtobufMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    String url = currentJmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
                    assertMetricsResponse(
                            currentJmxExporterTestEnvironment,
                            httpResponse,
                            MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                })
                .build();
    }

    private static Action testPrometheusHasMetrics() {
        return Direct.builder("testPrometheusHasMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    PrometheusTestEnvironment currentPrometheusTestEnvironment = getPrometheusTestEnvironment(context);

                    boolean isJmxExporterModeJavaStandalone =
                            currentJmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

                    for (String metricName : ExpectedMetricsNames.getMetricsNames().stream()
                            .filter(metricName -> !isJmxExporterModeJavaStandalone
                                    || (!metricName.startsWith("jvm_") && !metricName.startsWith("process_")))
                            .collect(Collectors.toList())) {
                        Double value = getPrometheusMetric(currentPrometheusTestEnvironment, metricName);

                        assertThat(value).as("metricName [%s]", metricName).isNotNull();
                        assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
                    }
                })
                .build();
    }

    private static Action tearDown() {
        return Direct.builder("tearDown")
                .runnable(context -> {
                    var store = context.getStore();
                    Network network = store.remove(NETWORK_KEY, Network.class).orElse(null);
                    JmxExporterTestEnvironment jmxExporterTestEnvironment = store.remove(
                                    JMX_EXPORTER_TEST_ENVIRONMENT, JmxExporterTestEnvironment.class)
                            .orElse(null);
                    PrometheusTestEnvironment prometheusTestEnvironment = store.remove(
                                    PROMETHEUS_TEST_ENVIRONMENT, PrometheusTestEnvironment.class)
                            .orElse(null);

                    if (network != null && jmxExporterTestEnvironment != null && prometheusTestEnvironment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(jmxExporterTestEnvironment)
                                .addCloseable(prometheusTestEnvironment)
                                .addCloseable(network)
                                .runAndThrow();
                    }
                })
                .build();
    }

    private static JmxExporterTestEnvironment getJmxExporterTestEnvironment(Context context) {
        return context.getParent()
                .getStore()
                .get(JMX_EXPORTER_TEST_ENVIRONMENT, JmxExporterTestEnvironment.class)
                .orElseThrow();
    }

    private static PrometheusTestEnvironment getPrometheusTestEnvironment(Context context) {
        return context.getParent()
                .getStore()
                .get(PROMETHEUS_TEST_ENVIRONMENT, PrometheusTestEnvironment.class)
                .orElseThrow();
    }

    private static void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
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
                jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName = jmxExporterTestEnvironment.getJmxExporterMode().getBuildInfoName();

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

    protected static Double getPrometheusMetric(PrometheusTestEnvironment prometheusTestEnvironment, String metricName)
            throws Throwable {
        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(5)))
                .retryOn(t -> t instanceof RuntimeException)
                .run(() -> {
                    HttpResponse httpResponse = sendPrometheusQuery(prometheusTestEnvironment, metricName);

                    assertThat(httpResponse.statusCode()).isEqualTo(200);
                    assertThat(httpResponse.body()).isNotNull();
                    assertThat(httpResponse.body().string()).isNotNull();

                    if (httpResponse.body().string().contains(metricName)) {
                        return;
                    }

                    throw new RuntimeException("metric not found yet: " + metricName);
                });

        return result.isPass() ? 1.0 : null;
    }

    protected static HttpResponse sendPrometheusQuery(PrometheusTestEnvironment prometheusTestEnvironment, String query)
            throws IOException {
        return sendRequest(
                prometheusTestEnvironment, "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    protected static HttpResponse sendRequest(PrometheusTestEnvironment prometheusTestEnvironment, String path)
            throws IOException {
        return HttpClient.sendRequest(prometheusTestEnvironment.getPrometheusUrl(path));
    }
}
