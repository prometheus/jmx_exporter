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

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpMetricsResponse;
import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpResponseCode;

import io.prometheus.jmx.test.support.Mode;
import io.prometheus.jmx.test.support.TestArgument;
import io.prometheus.jmx.test.support.http.HttpBasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufCounterMetricAssertion;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufGaugeMetricAssertion;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufMetricsParser;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufUntypedMetricAssertion;
import io.prometheus.jmx.test.support.metrics.text.TextCounterMetricAssertion;
import io.prometheus.jmx.test.support.metrics.text.TextGaugeMetricAssertion;
import io.prometheus.jmx.test.support.metrics.text.TextMetric;
import io.prometheus.jmx.test.support.metrics.text.TextMetricsParser;
import io.prometheus.jmx.test.support.metrics.text.TextUntypedMetricAssertion;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

public class BasicAuthenticationPBKDF2WithHmacSHA1Test extends AbstractBasicAuthenticationTest
        implements Consumer<HttpResponse> {

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    protected static Stream<TestArgument> arguments() {
        return AbstractBasicAuthenticationTest.arguments()
                .filter(PBKDF2WITHHMAC_TEST_ARGUMENT_FILTER);
    }

    @TestEngine.Test
    public void testHealthy() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                final AtomicInteger code = new AtomicInteger(HttpResponse.UNAUTHORIZED);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    code.set(HttpResponse.OK);
                }

                new HttpHealthyRequest()
                        .credentials(new HttpBasicAuthenticationCredentials(username, password))
                        .send(testContext.httpClient())
                        .accept(response -> assertHttpResponseCode(response, code.get()));
            }
        }
    }

    @TestEngine.Test
    public void testMetrics() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                final AtomicInteger code = new AtomicInteger(HttpResponse.UNAUTHORIZED);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    code.set(HttpResponse.OK);
                }

                new HttpMetricsRequest()
                        .credentials(new HttpBasicAuthenticationCredentials(username, password))
                        .send(testContext.httpClient())
                        .accept(
                                response -> {
                                    assertHttpResponseCode(response, code.get());
                                    if (code.get() == HttpResponse.OK) {
                                        accept(response);
                                    }
                                });
            }
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                final AtomicInteger code = new AtomicInteger(HttpResponse.UNAUTHORIZED);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    code.set(HttpResponse.OK);
                }

                new HttpOpenMetricsRequest()
                        .credentials(new HttpBasicAuthenticationCredentials(username, password))
                        .send(testContext.httpClient())
                        .accept(
                                response -> {
                                    assertHttpResponseCode(response, code.get());
                                    if (code.get() == HttpResponse.OK) {
                                        accept(response);
                                    }
                                });
            }
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                final AtomicInteger code = new AtomicInteger(HttpResponse.UNAUTHORIZED);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    code.set(HttpResponse.OK);
                }

                new HttpPrometheusMetricsRequest()
                        .credentials(new HttpBasicAuthenticationCredentials(username, password))
                        .send(testContext.httpClient())
                        .accept(
                                response -> {
                                    assertHttpResponseCode(response, code.get());
                                    if (code.get() == HttpResponse.OK) {
                                        accept(response);
                                    }
                                });
            }
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                final AtomicInteger code = new AtomicInteger(HttpResponse.UNAUTHORIZED);

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    code.set(HttpResponse.OK);
                }

                new HttpPrometheusProtobufMetricsRequest()
                        .credentials(new HttpBasicAuthenticationCredentials(username, password))
                        .send(testContext.httpClient())
                        .accept(
                                response -> {
                                    assertHttpResponseCode(response, code.get());
                                    if (code.get() == HttpResponse.OK) {
                                        accept(response);
                                    }
                                });
            }
        }
    }

    @Override
    public void accept(HttpResponse httpResponse) {
        assertHttpMetricsResponse(httpResponse);

        if (isProtoBufFormat(httpResponse)) {
            assertProtobufFormatResponse(httpResponse);
        } else {
            assertTextFormatResponse(httpResponse);
        }
    }

    private void assertTextFormatResponse(HttpResponse httpResponse) {
        Collection<TextMetric> metrics = TextMetricsParser.parse(httpResponse);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        new TextGaugeMetricAssertion(metrics)
                .name("jmx_exporter_build_info")
                .label("name", buildInfoName)
                .value(1d)
                .isPresent();

        new TextGaugeMetricAssertion(metrics).name("jmx_scrape_error").value(0d).isPresent();

        new TextCounterMetricAssertion(metrics)
                .name("jmx_config_reload_success_total")
                .value(0d)
                .isPresent();

        new TextGaugeMetricAssertion(metrics)
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new TextGaugeMetricAssertion(metrics)
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new TextGaugeMetricAssertion(metrics)
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new TextGaugeMetricAssertion(metrics)
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new TextUntypedMetricAssertion(metrics)
                .name("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .label("source", "/dev/sda1")
                .value(7.516192768E9d)
                .isPresent();

        new TextUntypedMetricAssertion(metrics)
                .name("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .label("source", "/dev/sda2")
                .value(0.8d)
                .isPresent();

        new TextCounterMetricAssertion(metrics)
                .name("service_time_seconds_total")
                .value(.2d)
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new TextGaugeMetricAssertion(metrics)
                .name("temperature_celsius")
                .label("location", "Berlin")
                .value(22.3)
                .isPresent(testArgument.mode() == Mode.JavaAgent);
    }

    private void assertProtobufFormatResponse(HttpResponse httpResponse) {
        Collection<Metrics.MetricFamily> metricsFamilies =
                ProtobufMetricsParser.parse(httpResponse);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jmx_exporter_build_info")
                .label("name", buildInfoName)
                .value(1d)
                .isPresent();

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jmx_scrape_error")
                .value(0d)
                .isPresent();

        new ProtobufCounterMetricAssertion(metricsFamilies)
                .name("jmx_config_reload_success_total")
                .value(0d)
                .isPresent();

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new ProtobufUntypedMetricAssertion(metricsFamilies)
                .name("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .label("source", "/dev/sda1")
                .value(7.516192768E9d)
                .isPresent();

        new ProtobufUntypedMetricAssertion(metricsFamilies)
                .name("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .label("source", "/dev/sda2")
                .value(0.8d)
                .isPresent();

        new ProtobufCounterMetricAssertion(metricsFamilies)
                .name("service_time_seconds_total")
                .value(.2d)
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new ProtobufGaugeMetricAssertion(metricsFamilies)
                .name("temperature_celsius")
                .label("location", "Berlin")
                .value(22.3)
                .isPresent(testArgument.mode() == Mode.JavaAgent);
    }
}
