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
import io.prometheus.jmx.test.HttpHeader;
import io.prometheus.jmx.test.Metric;
import io.prometheus.jmx.test.MetricsParser;
import io.prometheus.jmx.test.javaagent.BaseJavaAgent_IT;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.antublue.test.engine.api.Parameter;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static io.prometheus.jmx.test.HttpClient.basicAuthentication;
import static org.assertj.core.api.Assertions.assertThat;

@TestEngine.Disabled
@TestEngine.Tag("/basicAuthentication/")
public class BasicAuthentication_IT extends BaseJavaAgent_IT {

    private static final String USERNAME = "prometheus";
    private static final String PASSWORD = "secret";

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
    public void beforeAll() throws Exception {
        applicationContainer = createApplicationContainer(this, dockerImageName, network);
        applicationContainer.start();

        httpClient = createHttpClient(applicationContainer, "http://localhost");
    }

    @TestEngine.Test
    public void testHealthy() throws Exception {
        String path = "/-/healthy";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, PASSWORD));
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
    public void testHealthyWithoutUsernameWithoutPassword() throws Exception {
        String path = "/-/healthy";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testHealthyWithoutUsernameWithPassword() throws Exception {
        String path = "/-/healthy";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(null, PASSWORD));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testHealthyWithUsernameWithoutPassword() throws Exception {
        String path = "/-/healthy";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, null));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetrics() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, PASSWORD));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header(HttpHeader.CONTENT_TYPE)).isEqualTo("text/plain; version=0.0.4; charset=utf-8");
            assertMetricsResponse(response);
        }
    }

    @TestEngine.Test
    public void testMetricsWithoutUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsWithUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, null));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsWithoutUsernameWithPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(null, PASSWORD));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, PASSWORD));
        requestBuilder.addHeader(HttpHeader.ACCEPT, "application/openmetrics-text; version=1.0.0; charset=utf-8");
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertMetricsResponse(response);
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormatWithoutUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormatWithUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, null));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormatWithoutUsernameWithPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(null, PASSWORD));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, PASSWORD));
        requestBuilder.addHeader(HttpHeader.ACCEPT, "text/plain; version=0.0.4; charset=utf-8");
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header(HttpHeader.CONTENT_TYPE)).isEqualTo("text/plain; version=0.0.4; charset=utf-8");
            assertMetricsResponse(response);
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormatWithoutUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormatWithUsernameWithoutPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(USERNAME, null));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormatWithoutUsernameWithPassword() throws Exception {
        String path = "/";
        Request.Builder requestBuilder = httpClient.createRequest(path);
        requestBuilder.addHeader(HttpHeader.AUTHORIZATION, basicAuthentication(null, PASSWORD));
        try (Response response = httpClient.execute(requestBuilder)) {
            assertThat(response).isNotNull();
            assertThat(response.code()).isEqualTo(401);
        }
    }

    @TestEngine.AfterClass
    public static void afterClass() {
        destroy(network);
    }

    private static void assertMetricsResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        assertThat(body).isNotNull();

        String content = body.string();
        assertThat(content).isNotNull();

        List<Metric> metricList = MetricsParser.parse(content);
        assertThat(metricList).isNotNull();
        assertThat(metricList).isNotEmpty();

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
