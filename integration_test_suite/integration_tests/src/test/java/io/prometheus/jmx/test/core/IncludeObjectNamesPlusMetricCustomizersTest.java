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
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.NetworkSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
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
import org.testcontainers.containers.Network;

public class IncludeObjectNamesPlusMetricCustomizersTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    @Paramixel.Disabled
    public static Action factory() throws Throwable {
        return Each.parallel(
                        IncludeObjectNamesPlusMetricCustomizersTest.class.getName(),
                        JmxExporterTestEnvironment.createTestEnvironments(
                                IncludeObjectNamesPlusMetricCustomizersTest.class),
                        environment -> Instance.builder(
                                        environment.name(),
                                        () -> new IncludeObjectNamesPlusMetricCustomizersTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                        IncludeObjectNamesPlusMetricCustomizersTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                                IncludeObjectNamesPlusMetricCustomizersTest
                                                                        ::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                                IncludeObjectNamesPlusMetricCustomizersTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                                IncludeObjectNamesPlusMetricCustomizersTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                                IncludeObjectNamesPlusMetricCustomizersTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                                IncludeObjectNamesPlusMetricCustomizersTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        IncludeObjectNamesPlusMetricCustomizersTest.class,
                                                        IncludeObjectNamesPlusMetricCustomizersTest::tearDown)))))
                .build();
    }

    private IncludeObjectNamesPlusMetricCustomizersTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = NetworkSupport.create();
        environment.initialize(network);
    }

    public void testHealthy() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void tearDown() {
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

        String buildInfoName = environment.getJmxExporterMode().getBuildInfoName();

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

        boolean hasJavaMetrics = false;

        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("java_lang_")) {
                hasJavaMetrics = true;
                break;
            }
        }

        assertThat(hasJavaMetrics).as("No java_lang_* metrics found").isFalse();

        boolean hasJvmMetrics = false;

        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("jvm_")) {
                hasJvmMetrics = true;
                break;
            }
        }

        assertThat(hasJvmMetrics).as("No jvm_* metrics found").isFalse();

        boolean hasTabularDataMetrics = false;

        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("io_prometheus_jmx_tabularData_")) {
                hasTabularDataMetrics = true;
                break;
            }
        }

        assertThat(hasTabularDataMetrics)
                .as("No io_prometheus_jmx_tabularData_* metrics found")
                .isFalse();

        boolean hasPerformanceMetrics = false;

        for (String metricName : metrics.keySet()) {
            if (metricName.contains("PerformanceMetrics")) {
                hasPerformanceMetrics = true;
                break;
            }
        }

        assertThat(hasPerformanceMetrics)
                .as("No PerformanceMetrics metrics found")
                .isFalse();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_customValue_Value")
                .withValue(345d)
                .isPresent();

        metrics.entrySet().stream()
                .filter(entry -> entry.getKey().equals("io_prometheus_jmx_customValue_Value"))
                .flatMap(entry -> entry.getValue().stream())
                .forEach(metric -> assertThat(metric.labels()).containsEntry("Text", "value"));
    }
}
