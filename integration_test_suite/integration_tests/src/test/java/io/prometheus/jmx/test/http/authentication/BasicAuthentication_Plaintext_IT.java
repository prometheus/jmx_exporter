/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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

import io.prometheus.jmx.test.Abstract_IT;
import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.credentials.BasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import io.prometheus.jmx.test.support.Response;
import org.antublue.test.engine.api.Parameter;
import org.antublue.test.engine.api.ParameterMap;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicAuthentication_Plaintext_IT extends Abstract_IT implements ContentConsumer {

    private static final String VALID_USERNAME = "Prometheus";
    private static final String VALID_PASSWORD = "secret";
    private static final String[] TEST_USERNAMES = new String[] { VALID_USERNAME, "prometheus", "bad", "", null };
    private static final String[] TEST_PASSWORDS = new String[] { VALID_PASSWORD, "Secret", "bad", "", null };
    private static final String BASE_URL = "http://localhost";

    private static Network network;

    private String testName;
    private String dockerImageName;
    private Mode mode;

    private GenericContainer<?> applicationContainer;
    private GenericContainer<?> exporterContainer;
    private HttpClient httpClient;

    @TestEngine.ParameterSupplier
    protected static Stream<Parameter> parameters() {
        // Filter specific Docker images that don't support PBKDF2WithHmacSHA256
        Set<String> filteredDockerImages = new HashSet<>();
        filteredDockerImages.add("ibmjava:8");
        filteredDockerImages.add("ibmjava:8-jre");
        filteredDockerImages.add("ibmjava:8-sdk");
        filteredDockerImages.add("ibmjava:8-sfj");

        return Abstract_IT
                .parameters()
                .filter(parameter -> {
                    ParameterMap parameterMap = parameter.value();
                    String dockerImageName = parameterMap.get(DOCKER_IMAGE_NAME);
                    return !filteredDockerImages.contains(dockerImageName);
                });
    }

    @TestEngine.Parameter
    public void parameter(Parameter parameter) {
        ParameterMap parameterMap = parameter.value();
        testName = getClass().getName();
        dockerImageName = parameterMap.get(DOCKER_IMAGE_NAME);
        mode = parameterMap.get(MODE);
    }

    @TestEngine.BeforeClass
    public static void beforeClass() {
        network = createNetwork();
    }

    @TestEngine.BeforeAll
    public void beforeAll() {
        switch (mode) {
            case JavaAgent: {
                applicationContainer = createJavaAgentApplicationContainer(network, dockerImageName, testName);
                applicationContainer.start();
                httpClient = createHttpClient(applicationContainer, BASE_URL);
                break;
            }
            case Standalone: {
                applicationContainer = createStandaloneApplicationContainer(network, dockerImageName, testName);
                applicationContainer.start();
                exporterContainer = createStandaloneExporterContainer(network, dockerImageName, testName);
                exporterContainer.start();
                httpClient = createHttpClient(exporterContainer, BASE_URL);
                break;
            }
        }
    }

    @TestEngine.Test
    public void testHealthy() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                Response expectedHealthyResponse = HealthyResponse.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedHealthyResponse = HealthyResponse.RESULT_200;
                }

                assertThatResponseForRequest(
                        new HealthyRequest(httpClient)
                                .withCredentials(new BasicAuthenticationCredentials(username, password)))
                        .isSuperset(expectedHealthyResponse);
            }
        }
    }

    @TestEngine.Test
    public void testMetrics() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                Response expectedMetricsResponse = MetricsResponse.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponse = MetricsResponse.RESULT_200;
                }

                Response actualMetricsResponse =
                        new MetricsRequest(httpClient)
                                .withCredentials(new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponse.isSuperset(expectedMetricsResponse));

                if (actualMetricsResponse.code() == 200) {
                    actualMetricsResponse.dispatch(this);
                }
            }
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                Response expectedMetricsResponse = OpenMetricsResponse.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponse = OpenMetricsResponse.RESULT_200;
                }

                Response actualMetricsResponse =
                        new MetricsRequest(httpClient)
                                .withCredentials(new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponse.isSuperset(expectedMetricsResponse));

                if (actualMetricsResponse.code() == 200) {
                    actualMetricsResponse.dispatch(this);
                }
            }
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                Response expectedMetricsResponse = PrometheusMetricsResponse.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponse = PrometheusMetricsResponse.RESULT_200;
                }

                Response actualMetricsResponse =
                        new MetricsRequest(httpClient)
                                .withCredentials(new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponse.isSuperset(expectedMetricsResponse));

                if (actualMetricsResponse.code() == 200) {
                    actualMetricsResponse.dispatch(this);
                }
            }
        }
    }

    @TestEngine.AfterAll
    public void afterAll() {
        destroy(applicationContainer);
        destroy(exporterContainer);
        httpClient = null;
    }

    @TestEngine.AfterClass
    public static void afterClass() {
        destroy(network);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metrics = MetricsParser.parse(content);

        String buildInfoName = mode == Mode.JavaAgent ? "jmx_prometheus_javaagent" : "jmx_prometheus_httpserver";

        assertThatMetricIn(metrics)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .exists();

        assertThatMetricIn(metrics)
                .withName("java_lang_Memory_NonHeapMemoryUsage_committed")
                .exists();

        assertThatMetricIn(metrics)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9)
                .exists();

        assertThatMetricIn(metrics)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8)
                .exists();

        assertThatMetricIn(metrics)
                .withName("jvm_threads_state")
                .exists(mode == Mode.JavaAgent);
    }
}
