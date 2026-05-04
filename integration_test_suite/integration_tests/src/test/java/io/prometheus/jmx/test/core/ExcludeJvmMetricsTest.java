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

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.jmx.test.support.util.Repeater;
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

public class ExcludeJvmMetricsTest {

    private static final int ENVIRONMENT_LEVEL = 2;

    private static final String ENVIRONMENT_KEY = "environment";

    private static final String NETWORK_KEY = "network";

    private static final int ITERATIONS = 10;

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                ExcludeJvmMetricsTest.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .filter(exporterTestEnvironment ->
                                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                        .map(ExcludeJvmMetricsTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", ExcludeJvmMetricsTest::testHealthy);

        Action testDefaultTextMetrics =
                Direct.of("testDefaultTextMetrics", ExcludeJvmMetricsTest::testDefaultTextMetrics);

        Action testOpenMetricsTextMetrics =
                Direct.of("testOpenMetricsTextMetrics", ExcludeJvmMetricsTest::testOpenMetricsTextMetrics);

        Action testPrometheusTextMetrics =
                Direct.of("testPrometheusTextMetrics", ExcludeJvmMetricsTest::testPrometheusTextMetrics);

        Action testPrometheusProtobufMetrics =
                Direct.of("testPrometheusProtobufMetrics", ExcludeJvmMetricsTest::testPrometheusProtobufMetrics);

        Action tests = DependentSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                jmxExporterTestEnvironment.getName(),
                Direct.of("setUp", context -> setUp(context, jmxExporterTestEnvironment)),
                tests,
                Direct.of("tearDown", ExcludeJvmMetricsTest::tearDown));
    }

    private static void setUp(Context context, JmxExporterTestEnvironment jmxExporterTestEnvironment) throws Throwable {
        Network network = Network.newNetwork();
        network.getId();
        jmxExporterTestEnvironment.initialize(ExcludeJvmMetricsTest.class, network);
        context.getStore().put(NETWORK_KEY, Value.of(network));
        context.getStore().put(ENVIRONMENT_KEY, Value.of(jmxExporterTestEnvironment));
    }

    private static void testHealthy(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    private static void testDefaultTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(() -> {
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.DEFAULT);
                })
                .run();
    }

    private static void testOpenMetricsTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(() -> {
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                })
                .run();
    }

    private static void testPrometheusTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(() -> {
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                })
                .run();
    }

    private static void testPrometheusProtobufMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(() -> {
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                })
                .run();
    }

    private static void tearDown(Context context) throws Throwable {
        Network network = context.getStore()
                .remove(NETWORK_KEY)
                .map(value -> value.cast(Network.class))
                .orElse(null);
        JmxExporterTestEnvironment environment = context.getStore()
                .remove(ENVIRONMENT_KEY)
                .map(value -> value.cast(JmxExporterTestEnvironment.class))
                .orElse(null);

        if (network != null && environment != null) {
            Cleanup.of(Cleanup.Mode.FORWARD)
                    .addCloseable(environment)
                    .addCloseable(network)
                    .runAndThrow();
        }
    }

    private static JmxExporterTestEnvironment getEnvironment(Context context) {
        return context.findAncestor(ENVIRONMENT_LEVEL)
                .orElseThrow()
                .getStore()
                .get(ENVIRONMENT_KEY)
                .orElseThrow()
                .cast(JmxExporterTestEnvironment.class);
    }

    private static void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType)
            throws Exception {
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

        String buildInfoName = jmxExporterTestEnvironment.getJmxExporterMode().getBuildInfoName();

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

        for (String metricName : metrics.keySet()) {
            String lowerCaseMetricName = metricName.toLowerCase();

            assertThat(lowerCaseMetricName).doesNotStartWith("com_sun_");
            assertThat(lowerCaseMetricName).doesNotStartWith("java_lang");
            assertThat(lowerCaseMetricName).doesNotStartWith("java_nio");
            assertThat(lowerCaseMetricName).doesNotStartWith("java_util_logging");
            assertThat(lowerCaseMetricName).doesNotStartWith("javax_management");
            assertThat(lowerCaseMetricName).doesNotStartWith("jdk_internal");
            assertThat(lowerCaseMetricName).doesNotStartWith("jdk_management");
            assertThat(lowerCaseMetricName).doesNotStartWith("jdk_management_flr");
            assertThat(lowerCaseMetricName).doesNotStartWith("jvm_");
        }
    }
}
