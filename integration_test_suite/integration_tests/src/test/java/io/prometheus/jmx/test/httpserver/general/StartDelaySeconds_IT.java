/*
 * Copyright 2022-2023 Douglas Hoard
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

package io.prometheus.jmx.test.httpserver.general;

import io.prometheus.jmx.test.DockerImageNameParameters;
import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.httpserver.BaseHttpServer_IT;
import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsRequest;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import org.antublue.test.engine.api.Parameter;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.prometheus.jmx.test.support.AssertThatRequestResponse.assertThatRequestResponse;
import static org.assertj.core.api.Assertions.assertThat;

public class StartDelaySeconds_IT extends BaseHttpServer_IT implements ContentConsumer {

    private static Network network;

    private String dockerImageName;
    private GenericContainer<?> applicationContainer;
    private GenericContainer<?> exporterContainer;
    private HttpClient httpClient;

    @TestEngine.ParameterSupplier
    public static Stream<Parameter> parameters() {
        return DockerImageNameParameters.parameters();
    }

    @TestEngine.Parameter
    public void parameter(Parameter parameter) {
        dockerImageName = parameter.value();
    }

    @TestEngine.BeforeClass
    public static void beforeClass() {
        network = createNetwork();
    }

    @TestEngine.BeforeAll
    public void beforeAll() {
        applicationContainer = createApplicationContainer(this, dockerImageName, network);
        applicationContainer.start();

        exporterContainer = createExporterContainer(this, dockerImageName, network);

        long startTimeMilliseconds = System.currentTimeMillis();
        exporterContainer.start();
        long endTimeMillisecond = System.currentTimeMillis();

        assertThat(endTimeMillisecond - startTimeMilliseconds).isGreaterThanOrEqualTo(5000);

        httpClient = createHttpClient(exporterContainer, "http://localhost");
    }

    @TestEngine.Test
    public void testHealthy() {
        assertThatRequestResponse(new HealthyRequest(httpClient))
                .isSuperset(HealthyResponse.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatRequestResponse(new MetricsRequest(httpClient))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatRequestResponse(new OpenMetricsRequest(httpClient))
                .isSuperset(OpenMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatRequestResponse(new PrometheusMetricsRequest(httpClient))
                .isSuperset(PrometheusMetricsResponse.RESULT_200)
                .dispatch(this);
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
        List<Metric> metricList = MetricsParser.parse(content);
        assertThat(metricList).isNotNull();
        assertThat(metricList).isNotEmpty();

        /*
         * Assert that we have a metric...
         *
         * name = jmx_exporter_build_info
         */
        Optional<Metric> optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("jmx_exporter_build_info"))
                        .findFirst();
        assertThat(optional).isPresent();
        Metric metric = optional.get();
        assertThat(metric.getLabels().containsKey("name")).isTrue();

        if (DockerImageNameParameters.isJava6(dockerImageName)) {
            assertThat(metric.getLabels().get("name")).isEqualTo("jmx_prometheus_httpserver_java6");
        } else {
            assertThat(metric.getLabels().get("name")).isEqualTo("jmx_prometheus_httpserver");
        }

        assertThat(metric.getLine().indexOf("unknown")).isEqualTo(-1);

        /* Assert that we have a metric...
         *
         * name = java_lang_memory_nonheapmemoryusage_committed
         */
        optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("java_lang_Memory_NonHeapMemoryUsage_committed"))
                        .findFirst();
        assertThat(optional).isPresent();

        /*
         * Assert that we have a metric...
         *
         * name = io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size
         * label = source
         * label value = /dev/sda1
         * value = 7.516192768E9
         */
        optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                                        && "/dev/sda1".equals(m.getLabels().get("source")))
                        .findFirst();
        assertThat(optional).isPresent();

        // Assert the specific metric's value
        metric = optional.get();
        assertThat(metric.getValue()).isEqualTo(7.516192768E9);
    }
}
