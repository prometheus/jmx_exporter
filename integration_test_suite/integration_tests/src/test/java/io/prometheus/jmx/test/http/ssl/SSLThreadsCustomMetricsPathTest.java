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

package io.prometheus.jmx.test.http.ssl;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.altcontainers.api.Network;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class SSLThreadsCustomMetricsPathTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments =
                JmxExporterTestEnvironment.createTestEnvironments(SSLThreadsCustomMetricsPathTest.class).stream()
                        .filter(environment -> !environment.getJavaDockerImage().contains("eclipse-temurin:8-alpine"))
                        .map(environment -> environment.setBaseUrl("https://localhost"))
                        .collect(Collectors.toList());

        return Each.parallel(
                        SSLThreadsCustomMetricsPathTest.class.getName(),
                        environments,
                        environment -> instance(
                                        environment.name(), () -> new SSLThreadsCustomMetricsPathTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        SSLThreadsCustomMetricsPathTest.class,
                                                        SSLThreadsCustomMetricsPathTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                SSLThreadsCustomMetricsPathTest.class,
                                                                SSLThreadsCustomMetricsPathTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                SSLThreadsCustomMetricsPathTest.class,
                                                                SSLThreadsCustomMetricsPathTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                SSLThreadsCustomMetricsPathTest.class,
                                                                SSLThreadsCustomMetricsPathTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                SSLThreadsCustomMetricsPathTest.class,
                                                                SSLThreadsCustomMetricsPathTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                SSLThreadsCustomMetricsPathTest.class,
                                                                SSLThreadsCustomMetricsPathTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        SSLThreadsCustomMetricsPathTest.class,
                                                        SSLThreadsCustomMetricsPathTest::tearDown)))))
                .build();
    }

    private SSLThreadsCustomMetricsPathTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
    }

    public void tearDown() {
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    public void testHealthy() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.CUSTOM_METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.CUSTOM_METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.CUSTOM_METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.CUSTOM_METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(SSLThreadsCustomMetricsPathTest.class, javaDockerImage, mode, metrics);
    }
}
