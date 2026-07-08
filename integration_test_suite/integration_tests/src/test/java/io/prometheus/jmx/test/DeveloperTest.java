/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;

import io.prometheus.jmx.AutoIncrementing;
import io.prometheus.jmx.BuildInfoMetrics;
import io.prometheus.jmx.CustomValue;
import io.prometheus.jmx.ExistDb;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.PerformanceMetrics;
import io.prometheus.jmx.StringValue;
import io.prometheus.jmx.TabularData;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public class DeveloperTest {

    private static final String BASE_URL = "http://localhost:";
    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    private HTTPServer httpServer;
    private String baseUrl;

    private DeveloperTest() {
        // Intentionally empty
    }

    public static void main(String[] args) throws Exception {
        DeveloperTest developerTest = new DeveloperTest();
        try {
            developerTest.setUp();
            developerTest.testHealthy();
            developerTest.testDefaultTextMetrics();
            developerTest.testOpenMetricsTextMetrics();
            developerTest.testPrometheusTextMetrics();
            developerTest.testPrometheusProtobufMetrics();
        } finally {
            developerTest.tearDown();
        }
    }

    public void setUp() {
        try {
            String resource = (DeveloperTest.class.getName().replace(".", "/") + "/exporter.yaml");
            Path tempDirectory = Files.createTempDirectory("jmx-exporter-test");
            File exporterYamlFile = tempDirectory.resolve("exporter.yaml").toFile();
            ResourceSupport.export(resource, exporterYamlFile);

            new TabularData().register();
            new AutoIncrementing().register();
            new ExistDb().register();
            new PerformanceMetrics().register();
            new CustomValue().register();
            new StringValue().register();

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);
            JvmMetrics.builder().register(DEFAULT_REGISTRY);
            new JmxCollector(exporterYamlFile).register(DEFAULT_REGISTRY);

            httpServer = HTTPServerFactory.createAndStartHTTPServer(DEFAULT_REGISTRY, exporterYamlFile);

            baseUrl = BASE_URL + httpServer.getPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up LocalTest", e);
        }
    }

    public void testHealthy() throws IOException {
        log("TEST testHealthy()");
        String url = baseUrl + JmxExporterPath.HEALTHY;
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
        log("PASS testHealthy()");
    }

    public void testDefaultTextMetrics() throws IOException {
        log("TEST testDefaultTextMetrics()");
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
        log("PASS testDefaultTextMetrics()");
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        log("TEST testOpenMetricsTextMetrics()");
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
        log("PASS testOpenMetricsTextMetrics()");
    }

    public void testPrometheusTextMetrics() throws IOException {
        log("TEST testPrometheusTextMetrics()");
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
        log("PASS testPrometheusTextMetrics()");
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        log("TEST testPrometheusProtobufMetrics()");
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
        log("PASS testPrometheusProtobufMetrics()");
    }

    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = "Standalone";
        String javaDockerImage = "local";

        assertMetrics(DeveloperTest.class, javaDockerImage, mode, metrics);
    }

    private static void log(Object object) {
        System.out.println(object);
    }
}
