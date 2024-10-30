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

import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.common.ExporterTestEnvironmentFactory;
import io.prometheus.jmx.test.common.ExporterTestSupport;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class SSLWithJKSKeyStoreTest2 {

    private static final String BASE_URL = "https://localhost";

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<ExporterTestEnvironment> arguments() {
        // Filter eclipse-temurin:8 based Alpine images due to missing TLS cipher suites
        // https://github.com/adoptium/temurin-build/issues/3002
        // https://bugs.openjdk.org/browse/JDK-8306037
        return ExporterTestEnvironmentFactory.createExporterTestEnvironments()
                .filter(
                        exporterTestEnvironment ->
                                !exporterTestEnvironment
                                        .getJavaDockerImage()
                                        .contains("eclipse-temurin:8-alpine"))
                .map(exporterTestEnvironment -> exporterTestEnvironment.setBaseUrl(BASE_URL));
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        ExporterTestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = ExporterTestSupport.getOrCreateNetwork(argumentContext);
        ExporterTestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/-/healthy";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testMetrics(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/openmetrics-text; version=1.0.0; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, "CONTENT-TYPE", "text/plain; version=0.0.4; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily;"
                                + " encoding=delimited");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(
                new Trap(
                        () -> ExporterTestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> ExporterTestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        ExporterTestSupport.destroyNetwork(classContext);
    }

    private void assertMetricsResponse(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        // Validate no duplicate metrics (metrics with the same name and labels)
        // and build a Metrics Map for subsequent processing

        Set<String> compositeSet = new LinkedHashSet<>();
        MetricsParser.parseCollection(httpResponse)
                .forEach(
                        metric -> {
                            String name = metric.name();
                            Map<String, String> labels = metric.labels();
                            String composite = name + " " + labels;
                            assertThat(compositeSet).doesNotContain(composite);
                            compositeSet.add(composite);
                            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
                        });

        // Validate common / known metrics (and potentially values)

        boolean isJmxExporterModeJavaAgent =
                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName =
                isJmxExporterModeJavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
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
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

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
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions")
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps")
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred")
                .withValue(6.0d)
                .isPresent();
    }
}
