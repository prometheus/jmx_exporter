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

package io.prometheus.jmx.test.rmi.ssl;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Lifecycle;
import org.paramixel.api.action.Parallel;
import org.paramixel.api.action.Spec;

public class MinimalRMISSLTest {

    private final JmxExporterTestEnvironment environment;

    private MinimalRMISSLTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Spec<?> factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(MinimalRMISSLTest.class).stream()
                .filter(e -> e.getJmxExporterMode() == JmxExporterMode.Standalone)
                .filter(e -> !e.getJavaDockerImage().contains("graalvm/jdk:java8"))
                .filter(e -> !e.getJavaDockerImage().contains("ibmjava"))
                .collect(Collectors.toList());

        return Parallel.of(MinimalRMISSLTest.class.getName())
                .each(
                        environments,
                        env -> Instance.of(env.name(), () -> new MinimalRMISSLTest(env))
                                .child(Lifecycle.<MinimalRMISSLTest>of("lifecycle")
                                        .before("setUp()", MinimalRMISSLTest::setUp)
                                        .child("testHealthy()", MinimalRMISSLTest::testHealthy)
                                        .child("testDefaultTextMetrics()", MinimalRMISSLTest::testDefaultTextMetrics)
                                        .child(
                                                "testOpenMetricsTextMetrics()",
                                                MinimalRMISSLTest::testOpenMetricsTextMetrics)
                                        .child(
                                                "testPrometheusTextMetrics()",
                                                MinimalRMISSLTest::testPrometheusTextMetrics)
                                        .child(
                                                "testPrometheusProtobufMetrics()",
                                                MinimalRMISSLTest::testPrometheusProtobufMetrics)
                                        .after("tearDown()", MinimalRMISSLTest::tearDown)));
    }

    public void setUp() throws Throwable {
        environment.initialize();
    }

    public void testHealthy() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void tearDown() throws Throwable {
        environment.close();
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        boolean isJmxExporterModeJavaAgent = environment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName = environment.getJmxExporterMode().getBuildInfoName();

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
    }
}
