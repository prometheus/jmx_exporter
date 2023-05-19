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

import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.TestParameter;
import io.prometheus.jmx.test.credentials.BasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import io.prometheus.jmx.test.support.Response;
import org.antublue.test.engine.api.TestEngine;

import java.util.Collection;
import java.util.stream.Stream;

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

public class BasicAuthenticationPBKDF2WithHmacSHA512Test extends BasicAuthenticationBaseTest implements ContentConsumer {

    /**
     * Method to get the list of TestParameters
     *
     * @return the return value
     */
    @TestEngine.ParameterSupplier
    protected static Stream<TestParameter> parameters() {
        return BasicAuthenticationBaseTest.parameters().filter(PBKDF2WITHHMAC_TEST_PARAMETER_FILTER);
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
                        new HealthyRequest(testState.httpClient())
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
                        new MetricsRequest(testState.httpClient())
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
                        new MetricsRequest(testState.httpClient())
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
                        new MetricsRequest(testState.httpClient())
                                .withCredentials(new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponse.isSuperset(expectedMetricsResponse));

                if (actualMetricsResponse.code() == 200) {
                    actualMetricsResponse.dispatch(this);
                }
            }
        }
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metrics = MetricsParser.parse(content);

        String buildInfoName =
                testParameter.mode() == Mode.JavaAgent ? "jmx_prometheus_javaagent" : "jmx_prometheus_httpserver";

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
                .exists(testParameter.mode() == Mode.JavaAgent);
    }
}
