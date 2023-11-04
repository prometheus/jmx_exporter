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

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.legacy.RequestResponseAssertions.assertThatResponseForRequest;

import io.prometheus.jmx.test.BaseTest;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.TestArgument;
import io.prometheus.jmx.test.support.legacy.ContentConsumer;
import io.prometheus.jmx.test.support.legacy.HealthyRequestLegacy;
import io.prometheus.jmx.test.support.legacy.HealthyResponseLegacy;
import io.prometheus.jmx.test.support.Label;
import io.prometheus.jmx.test.support.legacy.MetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsResponseLegacy;
import java.util.Collection;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

public class SSLWithJKSKeyStoreTest2 extends BaseTest implements ContentConsumer {

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
        return BaseTest.arguments()
                .filter(
                        testArgument ->
                                !testArgument
                                        .dockerImageName()
                                        .contains("eclipse-temurin:8-alpine"));
    }

    @TestEngine.Prepare
    protected void setBaseUrl() {
        testState.baseUrl(BASE_URL);
    }

    @TestEngine.Test
    public void testHealthy() {
        assertThatResponseForRequest(new HealthyRequestLegacy(testState.httpClient()))
                .isSuperset(HealthyResponseLegacy.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequestLegacy(testState.httpClient()))
                .isSuperset(MetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatResponseForRequest(new OpenMetricsRequestLegacy(testState.httpClient()))
                .isSuperset(OpenMetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatResponseForRequest(new PrometheusMetricsRequestLegacy(testState.httpClient()))
                .isSuperset(PrometheusMetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metrics = MetricsParser.parse(content);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        assertThatMetricIn(metrics)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .exists();

        assertThatMetricIn(metrics).withName("jmx_scrape_error").exists().withValue(0d);

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
