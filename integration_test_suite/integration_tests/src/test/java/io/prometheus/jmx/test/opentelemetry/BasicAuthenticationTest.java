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
import io.prometheus.jmx.test.support.http.HttpRequest;
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

public class BasicAuthenticationTest {

    private static final String VALID_USER = "Prometheus";
    private static final String VALUE_PASSWORD = "secret";

    private final OpenTelemetryTestEnvironment environment;
    private PrometheusQueryHelper prometheusQueryHelper;
    private Network network;

    private BasicAuthenticationTest(OpenTelemetryTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        BasicAuthenticationTest.class.getName(),
                        OpenTelemetryTestEnvironment.createTestEnvironments(BasicAuthenticationTest.class),
                        environment -> {
                            environment.withPrometheusReadyBasicAuthentication(VALID_USER, VALUE_PASSWORD);
                            return instance(environment.name(), () -> new BasicAuthenticationTest(environment))
                                    .body(scope("scenario")
                                            .before(step(
                                                    "setUp()",
                                                    withInstance(
                                                            BasicAuthenticationTest.class,
                                                            BasicAuthenticationTest::setUp)))
                                            .body(sequential("tests")
                                                    .child(step(
                                                            "testHealthy()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest::testHealthy)))
                                                    .child(step(
                                                            "testDefaultTextMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest::testDefaultTextMetrics)))
                                                    .child(step(
                                                            "testOpenMetricsTextMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest
                                                                            ::testOpenMetricsTextMetrics)))
                                                    .child(step(
                                                            "testPrometheusTextMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest
                                                                            ::testPrometheusTextMetrics)))
                                                    .child(step(
                                                            "testPrometheusProtobufMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest
                                                                            ::testPrometheusProtobufMetrics)))
                                                    .child(step(
                                                            "testPrometheusHasMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest
                                                                            ::testPrometheusHasMetrics))))
                                            .after(step(
                                                    "tearDown()",
                                                    withInstance(
                                                            BasicAuthenticationTest.class,
                                                            BasicAuthenticationTest::tearDown))))
                                    .build();
                        })
                .build();
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
        prometheusQueryHelper = PrometheusQueryHelper.builder(environment.prometheusTestEnvironment())
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build();
    }

    public void testHealthy() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(HttpRequest.builder()
                .url(url)
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(HttpRequest.builder()
                .url(url)
                .header(HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString())
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(HttpRequest.builder()
                .url(url)
                .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString())
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.exporterTestEnvironment().getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(HttpRequest.builder()
                .url(url)
                .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString())
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void testPrometheusHasMetrics() throws Throwable {
        String mode = environment.exporterTestEnvironment().getJmxExporterMode().name();
        String javaDockerImage = environment.exporterTestEnvironment().getJavaDockerImage();

        Set<String> metricNames = loadMetricNames(BasicAuthenticationTest.class, mode, javaDockerImage);
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

        assertMetrics(BasicAuthenticationTest.class, javaDockerImage, mode, metrics);
    }
}
