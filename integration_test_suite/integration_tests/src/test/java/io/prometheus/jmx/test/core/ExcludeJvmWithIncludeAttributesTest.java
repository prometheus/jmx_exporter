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

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
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
import java.util.stream.Collectors;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.testcontainers.containers.Network;

public class ExcludeJvmWithIncludeAttributesTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    @Paramixel.Disabled
    public static Action factory() throws Throwable {
        var environments =
                JmxExporterTestEnvironment.createTestEnvironments(ExcludeJvmWithIncludeAttributesTest.class).stream()
                        .filter(exporterTestEnvironment ->
                                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                        .collect(Collectors.toList());

        return Each.parallel(
                        ExcludeJvmWithIncludeAttributesTest.class.getName(),
                        environments,
                        environment -> Instance.builder(
                                        environment.name(), () -> new ExcludeJvmWithIncludeAttributesTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        ExcludeJvmWithIncludeAttributesTest.class,
                                                        ExcludeJvmWithIncludeAttributesTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                ExcludeJvmWithIncludeAttributesTest.class,
                                                                ExcludeJvmWithIncludeAttributesTest::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                ExcludeJvmWithIncludeAttributesTest.class,
                                                                ExcludeJvmWithIncludeAttributesTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                ExcludeJvmWithIncludeAttributesTest.class,
                                                                ExcludeJvmWithIncludeAttributesTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                ExcludeJvmWithIncludeAttributesTest.class,
                                                                ExcludeJvmWithIncludeAttributesTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                ExcludeJvmWithIncludeAttributesTest.class,
                                                                ExcludeJvmWithIncludeAttributesTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        ExcludeJvmWithIncludeAttributesTest.class,
                                                        ExcludeJvmWithIncludeAttributesTest::tearDown)))))
                .build();
    }

    private ExcludeJvmWithIncludeAttributesTest(JmxExporterTestEnvironment environment) {
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

    public void testDefaultTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void tearDown() {
        environment.close();
        NetworkSupport.close(network);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType)
            throws Exception {
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

        for (String metricName : metrics.keySet()) {
            String lowerCaseMetricName = metricName.toLowerCase();
            assertThat(lowerCaseMetricName).doesNotStartWith("jvm_");
        }

        Set<String> includedMetrics = metrics.keySet().stream()
                .filter(metricName -> !metricName.startsWith("jmx_"))
                .collect(Collectors.toSet());

        assertThat(includedMetrics).hasSize(2);

        assertThat(includedMetrics).contains("java_lang_Threading_ThreadCount");
        assertThat(includedMetrics).contains("java_lang_Threading_TotalStartedThreadCount");
    }
}
