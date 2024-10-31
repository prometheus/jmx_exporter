/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.http.authentication;

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.TestEnvironmentFactory;
import io.prometheus.jmx.test.support.TestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class BasicAuthenticationSHA1Test {

    private final String VALID_USERNAME = "Prometheus";

    private final String VALID_PASSWORD = "secret";

    private final String[] TEST_USERNAMES =
            new String[] {VALID_USERNAME, "prometheus", "bad", "", null};

    private final String[] TEST_PASSWORDS =
            new String[] {VALID_PASSWORD, "Secret", "bad", "", null};

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<ExporterTestEnvironment> arguments() {
        return TestEnvironmentFactory.createExporterTestEnvironments();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        TestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/-/healthy";

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest =
                        HttpRequest.builder()
                                .url(url)
                                .basicAuthentication(username, password)
                                .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                assertThat(httpResponse.statusCode()).isEqualTo(expectedStatusCode);
            }
        }
    }

    @Verifyica.Test
    public void testMetrics(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest =
                        HttpRequest.builder()
                                .url(url)
                                .basicAuthentication(username, password)
                                .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(exporterTestEnvironment, httpResponse);
                }
            }
        }
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest =
                        HttpRequest.builder()
                                .url(url)
                                .basicAuthentication(username, password)
                                .header(
                                        "CONTENT-TYPE",
                                        "application/openmetrics-text; version=1.0.0;"
                                                + " charset=utf-8")
                                .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(exporterTestEnvironment, httpResponse);
                }
            }
        }
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest =
                        HttpRequest.builder()
                                .url(url)
                                .basicAuthentication(username, password)
                                .header("CONTENT-TYPE", "text/plain; version=0.0.4; charset=utf-8")
                                .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(exporterTestEnvironment, httpResponse);
                }
            }
        }
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest =
                        HttpRequest.builder()
                                .url(url)
                                .basicAuthentication(username, password)
                                .header(
                                        "CONTENT-TYPE",
                                        "application/vnd.google.protobuf;"
                                                + " proto=io.prometheus.client.MetricFamily;"
                                                + " encoding=delimited")
                                .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(exporterTestEnvironment, httpResponse);
                }
            }
        }
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> TestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> TestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        new Trap(() -> TestSupport.destroyNetwork(classContext)).assertEmpty();
    }

    private void assertMetricsResponse(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertCommonMetricsResponse(httpResponse);

        Map<String, Collection<Metric>> metrics = MetricsParser.parseMap(httpResponse);

        boolean isJmxExporterModeJavaAgent =
                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName =
                isJmxExporterModeJavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_standalone";

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
    }
}
