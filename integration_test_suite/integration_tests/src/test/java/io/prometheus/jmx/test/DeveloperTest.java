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
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;

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
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;

public class DeveloperTest {

    private static final String BASE_URL = "http://localhost:";
    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    private HTTPServer httpServer;
    private String baseUrl;

    private DeveloperTest() {
        // Intentionally empty
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    public static Action factory() throws Throwable {
        return Instance.builder(DeveloperTest.class.getName(), DeveloperTest::new)
                .body(Scope.builder("scenario")
                        .before(Step.of("setUp()", withInstance(DeveloperTest.class, DeveloperTest::setUp)))
                        .body(Sequence.builder("tests")
                                .child(Step.of(
                                        "testHealthy()", withInstance(DeveloperTest.class, DeveloperTest::testHealthy)))
                                .child(Step.of(
                                        "testDefaultTextMetrics()",
                                        withInstance(DeveloperTest.class, DeveloperTest::testDefaultTextMetrics)))
                                .child(Step.of(
                                        "testOpenMetricsTextMetrics()",
                                        withInstance(DeveloperTest.class, DeveloperTest::testOpenMetricsTextMetrics)))
                                .child(Step.of(
                                        "testPrometheusTextMetrics()",
                                        withInstance(DeveloperTest.class, DeveloperTest::testPrometheusTextMetrics)))
                                .child(Step.of(
                                        "testPrometheusProtobufMetrics()",
                                        withInstance(
                                                DeveloperTest.class, DeveloperTest::testPrometheusProtobufMetrics))))
                        .after(Step.of("tearDown()", withInstance(DeveloperTest.class, DeveloperTest::tearDown))))
                .build();
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
        String url = baseUrl + JmxExporterPath.HEALTHY;
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = baseUrl + JmxExporterPath.METRICS;
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        Set<String> compositeNameSet = new HashSet<>();
        MetricsParser.parseCollection(httpResponse).forEach(metric -> {
            String name = metric.name();
            Map<String, String> labels = metric.labels();
            String compositeName = name + " " + labels;
            assertThat(compositeNameSet).doesNotContain(compositeName);
            compositeNameSet.add(compositeName);
            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
        });

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", "unknown")
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.COUNTER)
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions")
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps")
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred")
                .withValue(6.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "1")
                .withLabel("key_path", "/db/query1.xq")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "2")
                .withLabel("key_path", "/db/query2.xq")
                .isPresent();

        boolean hasJavaMetrics = false;
        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("java_lang_")) {
                hasJavaMetrics = true;
                break;
            }
        }
        assertThat(hasJavaMetrics).as("No java_lang_* metrics found").isTrue();

        boolean hasJvmMetrics = false;
        for (String metricName : metrics.keySet()) {
            if (metricName.startsWith("jvm_")) {
                hasJvmMetrics = true;
                break;
            }
        }
        assertThat(hasJvmMetrics).as("No jvm_* metrics found").isTrue();
    }
}
