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

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.legacy.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.credentials.BasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.Label;
import io.prometheus.jmx.test.support.legacy.ContentConsumer;
import io.prometheus.jmx.test.support.legacy.HealthyRequestLegacy;
import io.prometheus.jmx.test.support.legacy.HealthyResponseLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.ResponseLegacy;
import java.util.Collection;
import org.antublue.test.engine.api.TestEngine;

public class BasicAuthenticationSHA512Test extends BasicAuthenticationBaseTest
        implements ContentConsumer {

    @TestEngine.Test
    public void testHealthy() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                ResponseLegacy expectedHealthyResponseLegacy = HealthyResponseLegacy.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedHealthyResponseLegacy = HealthyResponseLegacy.RESULT_200;
                }

                assertThatResponseForRequest(
                                new HealthyRequestLegacy(testContext.httpClient())
                                        .withCredentials(
                                                new BasicAuthenticationCredentials(
                                                        username, password)))
                        .isSuperset(expectedHealthyResponseLegacy);
            }
        }
    }

    @TestEngine.Test
    public void testMetrics() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                ResponseLegacy expectedMetricsResponseLegacy = MetricsResponseLegacy.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponseLegacy = MetricsResponseLegacy.RESULT_200;
                }

                ResponseLegacy actualMetricsResponseLegacy =
                        new MetricsRequestLegacy(testContext.httpClient())
                                .withCredentials(
                                        new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponseLegacy.isSuperset(expectedMetricsResponseLegacy));

                if (actualMetricsResponseLegacy.code() == 200) {
                    actualMetricsResponseLegacy.dispatch(this);
                }
            }
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                ResponseLegacy expectedMetricsResponseLegacy = OpenMetricsResponseLegacy.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponseLegacy = OpenMetricsResponseLegacy.RESULT_200;
                }

                ResponseLegacy actualMetricsResponseLegacy =
                        new MetricsRequestLegacy(testContext.httpClient())
                                .withCredentials(
                                        new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponseLegacy.isSuperset(expectedMetricsResponseLegacy));

                if (actualMetricsResponseLegacy.code() == 200) {
                    actualMetricsResponseLegacy.dispatch(this);
                }
            }
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                ResponseLegacy expectedMetricsResponseLegacy =
                        PrometheusMetricsResponseLegacy.RESULT_401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedMetricsResponseLegacy = PrometheusMetricsResponseLegacy.RESULT_200;
                }

                ResponseLegacy actualMetricsResponseLegacy =
                        new MetricsRequestLegacy(testContext.httpClient())
                                .withCredentials(
                                        new BasicAuthenticationCredentials(username, password))
                                .execute();

                assertThat(actualMetricsResponseLegacy.isSuperset(expectedMetricsResponseLegacy));

                if (actualMetricsResponseLegacy.code() == 200) {
                    actualMetricsResponseLegacy.dispatch(this);
                }
            }
        }
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metrics = MetricsParser.parseString(content);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        assertThatMetricIn(metrics)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .exists();

        assertThatMetricIn(metrics)
                .withName("jvm_memory_used_bytes")
                .withLabel(Label.of("area", "nonheap"))
                .exists(testArgument.mode() == Mode.JavaAgent ? true : false);

        assertThatMetricIn(metrics)
                .withName("jvm_threads_current")
                .exists(testArgument.mode() == Mode.JavaAgent ? true : false);

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
    }
}
