/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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
import io.prometheus.jmx.test.support.util.TestSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class ExcludeJvmMetricsTest {

    private static final int ITERATIONS = 10;

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<JmxExporterTestEnvironment> arguments() throws Throwable {
        return JmxExporterTestEnvironment.createEnvironments()
                .filter(
                        exporterTestEnvironment ->
                                exporterTestEnvironment.getJmxExporterMode()
                                        == JmxExporterMode.JavaAgent);
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        TestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testHealthy(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.HEALTHY);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws Throwable {
        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(
                        () -> {
                            String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

                            HttpResponse httpResponse = HttpClient.sendRequest(url);

                            assertMetricsResponse(
                                    jmxExporterTestEnvironment,
                                    httpResponse,
                                    MetricsContentType.DEFAULT);
                        })
                .run();
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws Throwable {
        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(
                        () -> {
                            String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            url,
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.OPEN_METRICS_TEXT_METRICS
                                                    .toString());

                            assertMetricsResponse(
                                    jmxExporterTestEnvironment,
                                    httpResponse,
                                    MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                        })
                .run();
    }

    @Verifyica.Test
    public void testPrometheusTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws Throwable {
        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(
                        () -> {
                            String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            url,
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

                            assertMetricsResponse(
                                    jmxExporterTestEnvironment,
                                    httpResponse,
                                    MetricsContentType.PROMETHEUS_TEXT_METRICS);
                        })
                .run();
    }

    @Verifyica.Test
    public void testPrometheusProtobufMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws Throwable {
        new Repeater(ITERATIONS)
                .throttle(new Repeater.RandomThrottle(0, 100))
                .test(
                        () -> {
                            String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            url,
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.PROMETHEUS_PROTOBUF_METRICS
                                                    .toString());

                            assertMetricsResponse(
                                    jmxExporterTestEnvironment,
                                    httpResponse,
                                    MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                        })
                .run();
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> TestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> TestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        new Trap(() -> TestSupport.destroyNetwork(classContext)).assertEmpty();
    }

    private void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType)
            throws IOException {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        // Validate no duplicate metrics (metrics with the same name and labels)
        // and build a Metrics Map for subsequent processing

        Set<String> compositeNameSet = new HashSet<>();
        MetricsParser.parseCollection(httpResponse)
                .forEach(
                        metric -> {
                            String name = metric.name();
                            Map<String, String> labels = metric.labels();
                            String compositeName = name + " " + labels;
                            assertThat(compositeNameSet).doesNotContain(compositeName);
                            compositeNameSet.add(compositeName);
                            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
                        });

        // Validate common / known metrics (and potentially values)

        String buildInfoName =
                TestSupport.getBuildInfoName(jmxExporterTestEnvironment.getJmxExporterMode());

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

        // Validate JVM metrics are not present

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
