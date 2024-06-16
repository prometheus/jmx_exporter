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
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;

import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpBasicAuthenticationCredentials;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class BasicAuthenticationSHA512Test extends AbstractBasicAuthenticationTest
        implements Consumer<HttpResponse> {

    @TestEngine.Test
    public void testHealthy() {
        getAuthenticationTestCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpHealthyRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(testEnvironment.getHttpClient())
                                    .accept(
                                            response ->
                                                    assertHttpResponseCode(response, code.get()));
                        });
    }

    @TestEngine.Test
    public void testMetrics() {
        getAuthenticationTestCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(testEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(response);
                                                }
                                            });
                        });
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        getAuthenticationTestCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpOpenMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(testEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(response);
                                                }
                                            });
                        });
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        getAuthenticationTestCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpPrometheusMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(testEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(response);
                                                }
                                            });
                        });
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        getAuthenticationTestCredentials()
                .forEach(
                        authenticationTestArguments -> {
                            final AtomicInteger code =
                                    new AtomicInteger(
                                            authenticationTestArguments.isValid()
                                                    ? HttpResponse.OK
                                                    : HttpResponse.UNAUTHORIZED);
                            new HttpPrometheusProtobufMetricsRequest()
                                    .credentials(
                                            new HttpBasicAuthenticationCredentials(
                                                    authenticationTestArguments.getUsername(),
                                                    authenticationTestArguments.getPassword()))
                                    .send(testEnvironment.getHttpClient())
                                    .accept(
                                            response -> {
                                                assertHttpResponseCode(response, code.get());
                                                if (code.get() == HttpResponse.OK) {
                                                    accept(response);
                                                }
                                            });
                        });
    }

    @Override
    public void accept(HttpResponse httpResponse) {
        assertHttpMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parse(httpResponse);

        String buildInfoName =
                testArguments.getJmxExporterMode() == JmxExporterMode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType("COUNTER")
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresent(testArguments.getJmxExporterMode() == JmxExporterMode.JavaAgent);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresent(testArguments.getJmxExporterMode() == JmxExporterMode.JavaAgent);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isNotPresent(testArguments.getJmxExporterMode() == JmxExporterMode.Standalone);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isNotPresent(testArguments.getJmxExporterMode() == JmxExporterMode.Standalone);

        assertMetric(metrics)
                .ofType("UNTYPED")
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType("UNTYPED")
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();
    }
}
