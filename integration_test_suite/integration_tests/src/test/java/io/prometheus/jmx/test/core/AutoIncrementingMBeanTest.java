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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.opentest4j.AssertionFailedError;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.Value;
import org.paramixel.core.action.Container;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class AutoIncrementingMBeanTest {

    private static final String ENVIRONMENT_KEY = "environment";

    private static final String NETWORK_KEY = "network";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(AutoIncrementingMBeanTest.class.getName());
        for (JmxExporterTestEnvironment environment :
                JmxExporterTestEnvironment.createEnvironments().toList()) {
            parallelBuilder.child(argument(environment));
        }
        return parallelBuilder.build();
    }

    private static Action argument(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action setUp = setUp(jmxExporterTestEnvironment);
        Action testHealthy = testHealthy();
        Action testDefaultTextMetrics = testDefaultTextMetrics();
        Action testOpenMetricsTextMetrics = testOpenMetricsTextMetrics();
        Action testPrometheusTextMetrics = testPrometheusTextMetrics();
        Action testPrometheusProtobufMetrics = testPrometheusProtobufMetrics();
        Action testAutoIncrementingMBean = testAutoIncrementingMBean();
        Action tearDown = tearDown();

        return Container.builder(jmxExporterTestEnvironment.getName())
                .before(setUp)
                .child(testHealthy)
                .child(testDefaultTextMetrics)
                .child(testOpenMetricsTextMetrics)
                .child(testPrometheusTextMetrics)
                .child(testPrometheusProtobufMetrics)
                .child(testAutoIncrementingMBean)
                .after(tearDown)
                .build();
    }

    private static Action setUp(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        return Direct.builder("setUp")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    jmxExporterTestEnvironment.initialize(AutoIncrementingMBeanTest.class, network);
                    context.getStore().put(NETWORK_KEY, Value.of(network));
                    context.getStore().put(ENVIRONMENT_KEY, Value.of(jmxExporterTestEnvironment));
                })
                .build();
    }

    private static Action testHealthy() {
        return Direct.builder("testHealthy")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.HEALTHY);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertHealthyResponse(httpResponse);
                })
                .build();
    }

    private static Action testDefaultTextMetrics() {
        return Direct.builder("testDefaultTextMetrics")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.DEFAULT);
                })
                .build();
    }

    private static Action testOpenMetricsTextMetrics() {
        return Direct.builder("testOpenMetricsTextMetrics")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusTextMetrics() {
        return Direct.builder("testPrometheusTextMetrics")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusProtobufMetrics() {
        return Direct.builder("testPrometheusProtobufMetrics")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                })
                .build();
    }

    private static Action testAutoIncrementingMBean() {
        return Direct.builder("testAutoIncrementingMBean")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    double value1 = collect(url);
                    double value2 = collect(url);
                    double value3 = collect(url);

                    assertThat(value2).isGreaterThan(value1);
                    assertThat(value2).isEqualTo(value1 + 1);
                    assertThat(value3).isGreaterThan(value2);
                    assertThat(value3).isEqualTo(value2 + 1);
                })
                .build();
    }

    private static Action tearDown() {
        return Direct.builder("tearDown")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
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
                })
                .build();
    }

    private static JmxExporterTestEnvironment getEnvironment(Context context) {
        return context.getStore().get(ENVIRONMENT_KEY).orElseThrow().cast(JmxExporterTestEnvironment.class);
    }

    private static void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
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

        boolean isJmxExporterModeJavaAgent =
                jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

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

    private static double collect(String url) throws IOException {
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsContentType(httpResponse, MetricsContentType.DEFAULT);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        java.util.Optional<Double> optionalValue = metrics.stream()
                .filter(metric -> metric.name().startsWith("io_prometheus_jmx_autoIncrementing"))
                .map(Metric::value)
                .limit(1)
                .findFirst();

        if (optionalValue.isPresent()) {
            return optionalValue.get();
        }

        throw new AssertionFailedError("Metric name [io_prometheus_jmx_autoIncrementing] s not present");
    }
}
