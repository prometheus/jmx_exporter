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
import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;

import io.prometheus.jmx.test.BaseTest;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.Mode;
import io.prometheus.jmx.test.TestArgument;
import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.Label;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsRequest;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

public class SSLWithPKCS12KeyStoreMultipleCertificatesTest extends BaseTest
        implements ContentConsumer {

    private static final String BASE_URL = "https://localhost";

    protected static final Predicate<TestArgument> PKCS12_KEYSTORE_TEST_ARGUMENT_FILTER =
            new PKCS12KeyStoreTestArgumentFilter();

    private static class PKCS12KeyStoreTestArgumentFilter implements Predicate<TestArgument> {

        private final Set<String> filteredDockerImages;

        public PKCS12KeyStoreTestArgumentFilter() {
            filteredDockerImages = new HashSet<>();
            filteredDockerImages.add("eclipse-temurin:8-alpine");
            filteredDockerImages.add("ghcr.io/graalvm/jdk:java8");
            filteredDockerImages.add("ibmjava:8");
            filteredDockerImages.add("ibmjava:8-jre");
            filteredDockerImages.add("ibmjava:8-sdk");
            filteredDockerImages.add("ibmjava:8-sfj");
            filteredDockerImages.add("ibmjava:11");
        }

        /**
         * Evaluates this predicate on the given argument.
         *
         * @param testArgument the input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         */
        @Override
        public boolean test(TestArgument testArgument) {
            return !filteredDockerImages.contains(testArgument.dockerImageName());
        }
    }

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    protected static Stream<TestArgument> arguments() {
        // Filter Java versions that don't support the PKCS12 keystore
        // format or don't support the required TLS cipher suites
        return BaseTest.arguments().filter(PKCS12_KEYSTORE_TEST_ARGUMENT_FILTER);
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
