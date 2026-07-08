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
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.support.environment.IsolatorExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.altcontainers.api.Network;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class BasicIsolatorTest {

    private static final int JAVA_AGENT_COUNT = 3;

    private static final int DEFAULT_TEST = 0;

    private static final int LOWER_CASE_TEST = 1;

    private static final int FAILED_AUTHENTICATION_TEST = 2;

    /**
     * Mode strings used to differentiate metrics assertion files for each exporter instance.
     * Without this differentiation, the DEFAULT and LOWER_CASE instances would write conflicting
     * expectations to the same file because they use the same {@code JmxExporterMode.JavaAgent}
     * but produce different metric names.
     */
    private static final String MODE_DEFAULT = "JavaAgent";

    private static final String MODE_LOWERCASE = "JavaAgent_LowerCase";

    private final IsolatorExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        BasicIsolatorTest.class.getName(),
                        IsolatorExporterTestEnvironment.createTestEnvironments(BasicIsolatorTest.class),
                        environment -> instance(environment.name(), () -> new BasicIsolatorTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(BasicIsolatorTest.class, BasicIsolatorTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                BasicIsolatorTest.class,
                                                                BasicIsolatorTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                BasicIsolatorTest.class,
                                                                BasicIsolatorTest::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                BasicIsolatorTest.class,
                                                                BasicIsolatorTest::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                BasicIsolatorTest.class,
                                                                BasicIsolatorTest::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                BasicIsolatorTest.class,
                                                                BasicIsolatorTest::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(BasicIsolatorTest.class, BasicIsolatorTest::tearDown)))))
                .build();
    }

    private BasicIsolatorTest(IsolatorExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
    }

    public void testHealthy() throws IOException {
        for (int test = DEFAULT_TEST; test < FAILED_AUTHENTICATION_TEST; test++) {
            String url = environment.getUrl(test, JmxExporterPath.HEALTHY);
            HttpResponse httpResponse = HttpClient.sendRequest(url);
            assertHealthyResponse(httpResponse);
        }
    }

    public void testDefaultTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(url);

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT, MODE_DEFAULT);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT, MODE_LOWERCASE);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS, MODE_DEFAULT);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS, MODE_LOWERCASE);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testPrometheusTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS, MODE_DEFAULT);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS, MODE_LOWERCASE);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS, MODE_DEFAULT);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS, MODE_LOWERCASE);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void tearDown() {
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType, String mode) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(BasicIsolatorTest.class, javaDockerImage, mode, metrics);
    }
}
