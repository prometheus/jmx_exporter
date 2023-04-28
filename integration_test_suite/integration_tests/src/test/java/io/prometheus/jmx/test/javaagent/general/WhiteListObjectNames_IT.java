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

package io.prometheus.jmx.test.javaagent.general;

import io.prometheus.jmx.test.DockerImageNameParameters;
import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.javaagent.BaseJavaAgent_IT;
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

public class WhiteListObjectNames_IT extends BaseJavaAgent_IT implements ContentConsumer {

    private static Network network;

    private String dockerImageName;
    private GenericContainer<?> applicationContainer;
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
        
        httpClient = createHttpClient(applicationContainer, "http://localhost");
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
         * name = java_lang_Memory_NonHeapMemoryUsage_committed
         */
        Optional<Metric> optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("java_lang_Memory_NonHeapMemoryUsage_committed"))
                        .findFirst();

        assertThat(optional).isPresent();

        /*
         * Assert that we don't have a metric...
         *
         * name = io_prometheus_jmx*
         */
        metricList.forEach(m -> assertThat(m.getName()).doesNotStartWith("io_prometheus_jmx"));
    }
}
