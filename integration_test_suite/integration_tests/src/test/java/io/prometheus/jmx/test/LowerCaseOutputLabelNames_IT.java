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

package io.prometheus.jmx.test;

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
import org.antublue.test.engine.api.ParameterMap;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.Collection;

import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

public class LowerCaseOutputLabelNames_IT extends Abstract_IT implements ContentConsumer {

    private static final String BASE_URL = "http://localhost";

    private static Network network;

    private String testName;
    private String dockerImageName;
    private boolean isJava6;
    private Mode mode;

    private GenericContainer<?> applicationContainer;
    private GenericContainer<?> exporterContainer;
    private HttpClient httpClient;

    @TestEngine.Parameter
    public void parameter(Parameter parameter) {
        ParameterMap parameterMap = parameter.value();
        testName = getClass().getName();
        dockerImageName = parameterMap.get(DOCKER_IMAGE_NAME);
        isJava6 = parameterMap.get(IS_JAVA_6);
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
        assertThatResponseForRequest(new HealthyRequest(httpClient))
                .isSuperset(HealthyResponse.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequest(httpClient))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatResponseForRequest(new OpenMetricsRequest(httpClient))
                .isSuperset(OpenMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatResponseForRequest(new PrometheusMetricsRequest(httpClient))
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
        Collection<Metric> metricCollection = MetricsParser.parse(content);

        /*
         * Assert that all metrics have lower case label names
         */
        metricCollection
                .forEach(metric -> {
                    metric
                            .getLabels()
                            .entrySet()
                            .stream()
                            .forEach(entry -> assertThat(entry.getKey()).isEqualTo(entry.getKey().toLowerCase()));
                });
    }
}
