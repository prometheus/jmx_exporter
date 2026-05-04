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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.Value;
import org.paramixel.core.action.DependentSequential;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class BasicIsolatorTest {

    private static final int ENVIRONMENT_LEVEL = 2;

    private static final String ENVIRONMENT_KEY = "environment";

    private static final String NETWORK_KEY = "network";

    private static final int JAVA_AGENT_COUNT = 3;

    private static final int DEFAULT_TEST = 0;

    private static final int LOWER_CASE_TEST = 1;

    private static final int FAILED_AUTHENTICATION_TEST = 2;

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                BasicIsolatorTest.class.getName(),
                IsolatorExporterTestEnvironment.createEnvironments()
                        .map(BasicIsolatorTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(IsolatorExporterTestEnvironment isolatorExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", BasicIsolatorTest::testHealthy);

        Action testDefaultTextMetrics = Direct.of("testDefaultTextMetrics", BasicIsolatorTest::testDefaultTextMetrics);

        Action testOpenMetricsTextMetrics =
                Direct.of("testOpenMetricsTextMetrics", BasicIsolatorTest::testOpenMetricsTextMetrics);

        Action testPrometheusTextMetrics =
                Direct.of("testPrometheusTextMetrics", BasicIsolatorTest::testPrometheusTextMetrics);

        Action testPrometheusProtobufMetrics =
                Direct.of("testPrometheusProtobufMetrics", BasicIsolatorTest::testPrometheusProtobufMetrics);

        Action tests = DependentSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                isolatorExporterTestEnvironment.getName(),
                Direct.of("setUp", context -> setUp(context, isolatorExporterTestEnvironment)),
                tests,
                Direct.of("tearDown", BasicIsolatorTest::tearDown));
    }

    private static void setUp(Context context, IsolatorExporterTestEnvironment isolatorExporterTestEnvironment)
            throws Throwable {
        Network network = Network.newNetwork();
        network.getId();
        isolatorExporterTestEnvironment.initialize(BasicIsolatorTest.class, network);
        context.getStore().put(NETWORK_KEY, Value.of(network));
        context.getStore().put(ENVIRONMENT_KEY, Value.of(isolatorExporterTestEnvironment));
    }

    private static void testHealthy(Context context) throws Throwable {
        IsolatorExporterTestEnvironment environment = getEnvironment(context);
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

    private static void testDefaultTextMetrics(Context context) throws Throwable {
        IsolatorExporterTestEnvironment environment = getEnvironment(context);
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

    private static void testOpenMetricsTextMetrics(Context context) throws Throwable {
        IsolatorExporterTestEnvironment environment = getEnvironment(context);
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

    private static void testPrometheusTextMetrics(Context context) throws Throwable {
        IsolatorExporterTestEnvironment environment = getEnvironment(context);
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

    private static void testPrometheusProtobufMetrics(Context context) throws Throwable {
        IsolatorExporterTestEnvironment environment = getEnvironment(context);
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

    private static void tearDown(Context context) throws Throwable {
        Network network = context.getStore()
                .remove(NETWORK_KEY)
                .map(value -> value.cast(Network.class))
                .orElse(null);
        IsolatorExporterTestEnvironment environment = context.getStore()
                .remove(ENVIRONMENT_KEY)
                .map(value -> value.cast(IsolatorExporterTestEnvironment.class))
                .orElse(null);

        if (network != null && environment != null) {
            try {
                environment.destroy();
            } finally {
                Cleanup.of(Cleanup.Mode.FORWARD).addCloseable(network).runAndThrow();
            }
        }
    }

    private static IsolatorExporterTestEnvironment getEnvironment(Context context) {
        return context.findAncestor(ENVIRONMENT_LEVEL)
                .orElseThrow()
                .getStore()
                .get(ENVIRONMENT_KEY)
                .orElseThrow()
                .cast(IsolatorExporterTestEnvironment.class);
    }

    private static void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
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

    private static void assertMetricsResponseLowerCase(
            HttpResponse httpResponse, MetricsContentType metricsContentType) {
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
