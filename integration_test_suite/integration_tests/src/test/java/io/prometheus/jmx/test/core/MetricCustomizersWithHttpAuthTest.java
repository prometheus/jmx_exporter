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

import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.NetworkSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.Collection;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.testcontainers.containers.Network;

public class MetricCustomizersWithHttpAuthTest {

    private static final String VALID_USERNAME = "Prometheus";
    private static final String VALID_PASSWORD = "secret";
    private static final String[] TEST_USERNAMES = new String[] {VALID_USERNAME, "prometheus", "bad", "", null};
    private static final String[] TEST_PASSWORDS = new String[] {VALID_PASSWORD, "Secret", "bad", "", null};

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        MetricCustomizersWithHttpAuthTest.class.getName(),
                        JmxExporterTestEnvironment.createTestEnvironments(MetricCustomizersWithHttpAuthTest.class),
                        environment -> Instance.builder(
                                        environment.name(), () -> new MetricCustomizersWithHttpAuthTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        MetricCustomizersWithHttpAuthTest.class,
                                                        MetricCustomizersWithHttpAuthTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                MetricCustomizersWithHttpAuthTest.class,
                                                                MetricCustomizersWithHttpAuthTest::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                MetricCustomizersWithHttpAuthTest.class,
                                                                MetricCustomizersWithHttpAuthTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                MetricCustomizersWithHttpAuthTest.class,
                                                                MetricCustomizersWithHttpAuthTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                MetricCustomizersWithHttpAuthTest.class,
                                                                MetricCustomizersWithHttpAuthTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                MetricCustomizersWithHttpAuthTest.class,
                                                                MetricCustomizersWithHttpAuthTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        MetricCustomizersWithHttpAuthTest.class,
                                                        MetricCustomizersWithHttpAuthTest::tearDown)))))
                .build();
    }

    private MetricCustomizersWithHttpAuthTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = NetworkSupport.create();
        environment.initialize(network);
    }

    public void testHealthy() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                assertThat(httpResponse.statusCode()).isEqualTo(expectedStatusCode);
            }
        }
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
                }
            }
        }
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                }
            }
        }
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                }
            }
        }
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                }
            }
        }
    }

    public void tearDown() {
        environment.close();
        NetworkSupport.close(network);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        metrics.stream()
                .filter(metric -> metric.name().contains("io_prometheus_jmx_customValue_Value"))
                .forEach(metric -> assertThat(metric.labels()).containsEntry("Text", "value"));
    }
}
