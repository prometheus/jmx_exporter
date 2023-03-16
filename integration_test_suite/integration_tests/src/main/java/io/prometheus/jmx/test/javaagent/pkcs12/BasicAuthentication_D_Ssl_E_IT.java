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

package io.prometheus.jmx.test.javaagent.pkcs12;

import io.prometheus.jmx.test.DockerImageNameParameters;
import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.HttpHeader;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.TestUtils;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.devopology.test.engine.api.Parameter;
import org.devopology.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestEngine.Disabled
@TestEngine.Tag("/https/")
public class BasicAuthentication_D_Ssl_E_IT {

    private static final String USERNAME = "prometheus";
    private static final String PASSWORD = "secret";

    private static Network network;

    private String dockerImageName;
    private GenericContainer<?> applicationContainer;
    private HttpClient httpClient;

    @TestEngine.ParameterSupplier
    public static Stream<Parameter> parameters() {
        return DockerImageNameParameters.parameters(DockerImageNameParameters.OMIT_JAVA_6_VERSIONS);
    }

    @TestEngine.ParameterSetter
    public void setParameter(Parameter parameter) {
        dockerImageName = parameter.value();
    }

    @TestEngine.BeforeClass
    public static void beforeClass() {
        // Shared network
        network = Network.newNetwork();

        // Get the id to force the network creation
        network.getId();
    }

    @TestEngine.BeforeAll
    public void beforeAll() throws Exception {
        // Application container
        applicationContainer = new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forLogMessage(".*Running.*", 2))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(getClass().getName().replace(".", "/"), "/temp", BindMode.READ_ONLY)
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(8888)
                .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                .withNetwork(network)                
                .withNetworkAliases("application")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp");

        if (DockerImageNameParameters.isJava6(dockerImageName)) {
            applicationContainer.withCommand("/bin/sh application_java6.sh");
        }

        applicationContainer.start();

        // HTTP client
        httpClient = new HttpClient("https://localhost:" + applicationContainer.getMappedPort(8888));
    }

    @TestEngine.Test
    public void testHealthy() throws Exception {
        String path = "/-/healthy";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            ResponseBody responseBody = response.body();
            assertThat(responseBody).isNotNull();
            String content = responseBody.string();
            assertThat(content).isNotNull();
            assertThat(content).isEqualTo("Exporter is Healthy.");
        }
    }

    @TestEngine.Test
    public void testMetrics() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header(HttpHeader.CONTENT_TYPE)).isEqualTo("text/plain; version=0.0.4; charset=utf-8");
            assertMetricsResponse(response);
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.ACCEPT, "application/openmetrics-text; version=1.0.0; charset=utf-8");
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertMetricsResponse(response);
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.ACCEPT, "text/plain; version=0.0.4; charset=utf-8");
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header(HttpHeader.CONTENT_TYPE)).isEqualTo("text/plain; version=0.0.4; charset=utf-8");
            assertMetricsResponse(response);
        }
    }

    @TestEngine.AfterAll
    public void afterAll() {
        applicationContainer = TestUtils.close(applicationContainer);
        httpClient = null;
    }

    @TestEngine.AfterClass
    public static void afterClass() {
        network = TestUtils.close(network);
    }

    private static void assertMetricsResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        assertThat(body).isNotNull();

        String content = body.string();
        assertThat(content).isNotNull();

        List<Metric> metricList = MetricsParser.parse(content);
        assertThat(metricList).isNotNull();
        assertThat(metricList.size()).isGreaterThan(0);

        // Assert that we have a metric...
        //
        // name = java_lang_memory_nonheapmemoryusage_committed
        Optional<Metric> optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("java_lang_Memory_NonHeapMemoryUsage_committed"))
                        .findFirst();
        assertThat(optional).isPresent();

        // Assert that we have a metric...
        //
        // name = io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size
        // label = source
        // label value = /dev/sda1
        // value = 7.516192768E9
        optional =
                metricList
                        .stream()
                        .filter(m ->
                                m.getName().equals("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                                        && "/dev/sda1".equals(m.getLabels().get("source")))
                        .findFirst();
        assertThat(optional).isPresent();

        // Assert the specific metrics value
        Metric metric = optional.get();
        assertThat(metric.getValue()).isEqualTo(7.516192768E9);
    }
}
