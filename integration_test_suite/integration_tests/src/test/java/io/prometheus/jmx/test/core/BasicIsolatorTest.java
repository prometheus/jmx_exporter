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

package io.prometheus.jmx.test.core;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.IsolatorExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Lifecycle;
import org.paramixel.api.action.Parallel;
import org.paramixel.api.action.Spec;

public class BasicIsolatorTest {

    private static final int JAVA_AGENT_COUNT = 3;

    private static final int DEFAULT_TEST = 0;

    private static final int LOWER_CASE_TEST = 1;

    private static final int FAILED_AUTHENTICATION_TEST = 2;

    private final IsolatorExporterTestEnvironment environment;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Spec<?> factory() throws Throwable {
        return Parallel.of(BasicIsolatorTest.class.getName())
                .each(
                        IsolatorExporterTestEnvironment.createTestEnvironments(BasicIsolatorTest.class),
                        environment -> Instance.of(environment.name(), () -> new BasicIsolatorTest(environment))
                                .child(Lifecycle.<BasicIsolatorTest>of("lifecycle")
                                        .before("setUp()", BasicIsolatorTest::setUp)
                                        .child("testHealthy()", BasicIsolatorTest::testHealthy)
                                        .child("testDefaultTextMetrics()", BasicIsolatorTest::testDefaultTextMetrics)
                                        .child(
                                                "testOpenMetricsTextMetrics()",
                                                BasicIsolatorTest::testOpenMetricsTextMetrics)
                                        .child(
                                                "testPrometheusTextMetrics()",
                                                BasicIsolatorTest::testPrometheusTextMetrics)
                                        .child(
                                                "testPrometheusProtobufMetrics()",
                                                BasicIsolatorTest::testPrometheusProtobufMetrics)
                                        .after("tearDown()", BasicIsolatorTest::tearDown)));
    }

    private BasicIsolatorTest(IsolatorExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        environment.initialize();
    }

    public void testHealthy() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.HEALTHY);

            switch (test) {
                case LOWER_CASE_TEST:
                case DEFAULT_TEST: {
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertHealthyResponse(httpResponse);
                }
            }
        }
    }

    public void testDefaultTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(url);

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponseLowerCase(httpResponse, MetricsContentType.DEFAULT);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponseLowerCase(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testPrometheusTextMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponseLowerCase(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        for (int test = DEFAULT_TEST; test < JAVA_AGENT_COUNT; test++) {
            String url = environment.getUrl(test, JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

            switch (test) {
                case DEFAULT_TEST: {
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                    break;
                }
                case LOWER_CASE_TEST: {
                    assertMetricsResponseLowerCase(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                    break;
                }
                case FAILED_AUTHENTICATION_TEST: {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                    break;
                }
            }
        }
    }

    public void tearDown() {
        environment.close();
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
                .withLabel("name", "jmx_prometheus_javaagent")
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
                .withLabel("area", "nonheap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap");

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
    }

    private void assertMetricsResponseLowerCase(HttpResponse httpResponse, MetricsContentType metricsContentType) {
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
                .withLabel("name", "jmx_prometheus_javaagent")
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
                .withLabel("area", "nonheap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap");

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap");

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size".toLowerCase())
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent".toLowerCase())
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions"
                        .toLowerCase())
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps".toLowerCase())
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred"
                        .toLowerCase())
                .withValue(6.0d)
                .isPresent();
    }
}
