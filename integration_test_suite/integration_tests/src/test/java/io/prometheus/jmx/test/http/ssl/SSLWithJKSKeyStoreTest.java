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

package io.prometheus.jmx.test.http.ssl;

import io.prometheus.jmx.test.BaseTest;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.TestArgument;
import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsRequest;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import org.antublue.test.engine.api.TestEngine;

import java.util.Collection;
import java.util.stream.Stream;

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;

public class SSLWithJKSKeyStoreTest extends BaseTest implements ContentConsumer {

    private static final String BASE_URL = "https://localhost";

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    protected static Stream<TestArgument> arguments() {
        // Filter eclipse-temurin:8 based Alpine images due to missing TLS cipher suites
        // https://github.com/adoptium/temurin-build/issues/3002
        return BaseTest
                .arguments()
                .filter(testArgument -> !testArgument.dockerImageName().contains("eclipse-temurin:8-alpine"));
    }

    @TestEngine.Prepare
    protected void setBaseUrl() {
        testState.baseUrl(BASE_URL);
    }

    @TestEngine.Test
    public void testHealthy() {
        assertThatResponseForRequest(new HealthyRequest(testState.httpClient()))
                .isSuperset(HealthyResponse.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequest(testState.httpClient()))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatResponseForRequest(new OpenMetricsRequest(testState.httpClient()))
                .isSuperset(OpenMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatResponseForRequest(new PrometheusMetricsRequest(testState.httpClient()))
                .isSuperset(PrometheusMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metrics = MetricsParser.parse(content);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent ? "jmx_prometheus_javaagent" : "jmx_prometheus_httpserver";

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
                .exists(testArgument.mode() == Mode.JavaAgent);
    }
}
