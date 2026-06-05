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
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.time.Duration;
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
import org.paramixel.api.support.Retry;
import org.testcontainers.containers.Network;

public class StartDelayWithHttpAuthTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    private static final String VALID_USERNAME = "Prometheus";
    private static final String VALID_PASSWORD = "secret";
    private static final String[] TEST_USERNAMES = new String[] {VALID_USERNAME, "prometheus", "bad", "", null};
    private static final String[] TEST_PASSWORDS = new String[] {VALID_PASSWORD, "Secret", "bad", "", null};

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(StartDelayWithHttpAuthTest.class).stream()
                .filter(env -> env.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                .collect(Collectors.toList());

        return Each.parallel(
                        StartDelayWithHttpAuthTest.class.getName(),
                        environments,
                        environment -> Instance.builder(
                                        environment.name(), () -> new StartDelayWithHttpAuthTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        StartDelayWithHttpAuthTest.class,
                                                        StartDelayWithHttpAuthTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                StartDelayWithHttpAuthTest.class,
                                                                StartDelayWithHttpAuthTest::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                StartDelayWithHttpAuthTest.class,
                                                                StartDelayWithHttpAuthTest::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                StartDelayWithHttpAuthTest.class,
                                                                StartDelayWithHttpAuthTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                StartDelayWithHttpAuthTest.class,
                                                                StartDelayWithHttpAuthTest::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                StartDelayWithHttpAuthTest.class,
                                                                StartDelayWithHttpAuthTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        StartDelayWithHttpAuthTest.class,
                                                        StartDelayWithHttpAuthTest::tearDown)))))
                .build();
    }

    private StartDelayWithHttpAuthTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = NetworkSupport.create();
        environment.initialize(network);
    }

    public void testHealthy() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                final String u = username;
                final String p = password;
                final int statusCode = expectedStatusCode;

                Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                        .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                        .runAndThrow(() -> {
                            HttpRequest httpRequest = HttpRequest.builder()
                                    .url(url)
                                    .basicAuthentication(u, p)
                                    .build();

                            HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                            assertThat(httpResponse.statusCode()).isEqualTo(statusCode);
                        });
            }
        }
    }

    public void testDefaultTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                final String u = username;
                final String p = password;
                final int statusCode = expectedStatusCode;

                Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                        .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                        .runAndThrow(() -> {
                            HttpRequest httpRequest = HttpRequest.builder()
                                    .url(url)
                                    .basicAuthentication(u, p)
                                    .build();

                            HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                            if (statusCode == 401) {
                                assertThat(httpResponse.statusCode()).isEqualTo(401);
                            } else {
                                assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
                            }
                        });
            }
        }
    }

    public void testOpenMetricsTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                final String u = username;
                final String p = password;
                final int statusCode = expectedStatusCode;

                Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                        .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                        .runAndThrow(() -> {
                            HttpRequest httpRequest = HttpRequest.builder()
                                    .url(url)
                                    .basicAuthentication(u, p)
                                    .header(HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString())
                                    .build();

                            HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                            if (statusCode == 401) {
                                assertThat(httpResponse.statusCode()).isEqualTo(401);
                            } else {
                                assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                            }
                        });
            }
        }
    }

    public void testPrometheusTextMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                final String u = username;
                final String p = password;
                final int statusCode = expectedStatusCode;

                Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                        .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                        .runAndThrow(() -> {
                            HttpRequest httpRequest = HttpRequest.builder()
                                    .url(url)
                                    .basicAuthentication(u, p)
                                    .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString())
                                    .build();

                            HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                            if (statusCode == 401) {
                                assertThat(httpResponse.statusCode()).isEqualTo(401);
                            } else {
                                assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                            }
                        });
            }
        }
    }

    public void testPrometheusProtobufMetrics() throws Throwable {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                final String u = username;
                final String p = password;
                final int statusCode = expectedStatusCode;

                Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(10)))
                        .retryOn(t -> t instanceof AssertionError || t instanceof Exception)
                        .runAndThrow(() -> {
                            HttpRequest httpRequest = HttpRequest.builder()
                                    .url(url)
                                    .basicAuthentication(u, p)
                                    .header(
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString())
                                    .build();

                            HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                            if (statusCode == 401) {
                                assertThat(httpResponse.statusCode()).isEqualTo(401);
                            } else {
                                assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                            }
                        });
            }
        }
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

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresent();

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

        boolean hasJavaMetrics = false;

        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("java_lang_")) {
                hasJavaMetrics = true;
                break;
            }
        }

        assertThat(hasJavaMetrics).as("No java_lang_* metrics found").isTrue();
    }
}
