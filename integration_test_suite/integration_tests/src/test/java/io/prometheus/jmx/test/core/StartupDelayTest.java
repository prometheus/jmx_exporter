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
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.altcontainers.api.Network;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.support.Retry;

public class StartupDelayTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(StartupDelayTest.class).stream()
                .filter(environment -> environment.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                .collect(Collectors.toList());

        return Each.parallel(
                        StartupDelayTest.class.getName(),
                        environments,
                        environment -> instance(environment.name(), () -> new StartupDelayTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(StartupDelayTest.class, StartupDelayTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                StartupDelayTest.class, StartupDelayTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                StartupDelayTest.class,
                                                                StartupDelayTest::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                StartupDelayTest.class,
                                                                StartupDelayTest::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                StartupDelayTest.class,
                                                                StartupDelayTest::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                StartupDelayTest.class,
                                                                StartupDelayTest::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(StartupDelayTest.class, StartupDelayTest::tearDown)))))
                .build();
    }

    private StartupDelayTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
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
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    private HttpResponse sendRequestWithRetry(String url) throws Throwable {
        return sendRequestWithRetry(url, null, null);
    }

    private HttpResponse sendRequestWithRetry(String url, String header, String value) throws Throwable {
        final HttpResponse[] responseHolder = new HttpResponse[1];

        Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                .retryOn(t -> t instanceof Exception)
                .runAndThrow(() -> responseHolder[0] =
                        (header != null) ? HttpClient.sendRequest(url, header, value) : HttpClient.sendRequest(url));

        return responseHolder[0];
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(StartupDelayTest.class, javaDockerImage, mode, metrics);
    }
}
