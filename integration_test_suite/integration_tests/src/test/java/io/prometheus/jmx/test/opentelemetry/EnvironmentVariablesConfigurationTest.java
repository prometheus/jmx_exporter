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
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.loadMetricNames;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.prometheus.PrometheusQueryHelper;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.altcontainers.api.Network;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class EnvironmentVariablesConfigurationTest {

    private final OpenTelemetryTestEnvironment environment;
    private PrometheusQueryHelper prometheusQueryHelper;
    private Network network;

    private EnvironmentVariablesConfigurationTest(OpenTelemetryTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        EnvironmentVariablesConfigurationTest.class.getName(),
                        OpenTelemetryTestEnvironment.createTestEnvironments(
                                EnvironmentVariablesConfigurationTest.class),
                        environment -> instance(
                                        environment.exporterTestEnvironment().name(),
                                        () -> new EnvironmentVariablesConfigurationTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        EnvironmentVariablesConfigurationTest.class,
                                                        EnvironmentVariablesConfigurationTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest
                                                                        ::testPrometheusProtobufMetrics)))
                                                .child(step(
                                                        "testPrometheusHasMetrics()",
                                                        withInstance(
                                                                EnvironmentVariablesConfigurationTest.class,
                                                                EnvironmentVariablesConfigurationTest
                                                                        ::testPrometheusHasMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        EnvironmentVariablesConfigurationTest.class,
                                                        EnvironmentVariablesConfigurationTest::tearDown)))))
                .build();
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
        prometheusQueryHelper = PrometheusQueryHelper.builder(environment.prometheusTestEnvironment())
                .build();
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
        String mode = environment.exporterTestEnvironment().getJmxExporterMode().name();
        String javaDockerImage = environment.exporterTestEnvironment().getJavaDockerImage();

        Set<String> metricNames = loadMetricNames(EnvironmentVariablesConfigurationTest.class, mode, javaDockerImage);
        assertThat(metricNames).isNotEmpty();

        for (String metricName : metricNames) {
            Double value = prometheusQueryHelper.waitForMetric(metricName);

            assertThat(value).as("metricName [%s]", metricName).isNotNull();
            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
        }
    }

    public void tearDown() throws Throwable {
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.exporterTestEnvironment().getJmxExporterMode().name();
        String javaDockerImage = environment.exporterTestEnvironment().getJavaDockerImage();

        assertMetrics(EnvironmentVariablesConfigurationTest.class, javaDockerImage, mode, metrics);
    }
}
