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
import io.prometheus.jmx.test.support.util.TestSupport;
import java.util.Collection;
import java.util.List;
import org.paramixel.core.Action;
import org.paramixel.core.ConsoleRunner;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.action.StrictSequential;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

// Disabled as these tests are not intended to be run as part of the regular test suite
public class RMISSLFromYamlTest {

    public static void main(String[] args) {
        ConsoleRunner.runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    @Paramixel.Disabled
    public static Action actionFactory() {
        return Parallel.of(
                RMISSLFromYamlTest.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .filter(exporterTestEnvironment ->
                                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone)
                        .filter(exporterTestEnvironment ->
                                !exporterTestEnvironment.getJavaDockerImage().contains("graalvm/jdk:java8"))
                        .filter(exporterTestEnvironment ->
                                !exporterTestEnvironment.getJavaDockerImage().contains("ibmjava"))
                        .map(RMISSLFromYamlTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            String url = attachment.jmxExporterTestEnvironment.getUrl(JmxExporterPath.HEALTHY);

            HttpResponse httpResponse = HttpClient.sendRequest(url);

            assertHealthyResponse(httpResponse);
        });

        Action testDefaultTextMetrics = Direct.of("testDefaultTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            String url = attachment.jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(url);

            assertMetricsResponse(attachment.jmxExporterTestEnvironment, httpResponse, MetricsContentType.DEFAULT);
        });

        Action testOpenMetricsTextMetrics = Direct.of("testOpenMetricsTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            String url = attachment.jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

            assertMetricsResponse(
                    attachment.jmxExporterTestEnvironment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
        });

        Action testPrometheusTextMetrics = Direct.of("testPrometheusTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            String url = attachment.jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

            assertMetricsResponse(
                    attachment.jmxExporterTestEnvironment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
        });

        Action testPrometheusProtobufMetrics = Direct.of("testPrometheusProtobufMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            String url = attachment.jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

            assertMetricsResponse(
                    attachment.jmxExporterTestEnvironment,
                    httpResponse,
                    MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
        });

        Action tests = StrictSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                jmxExporterTestEnvironment.getName(),
                Direct.of("setUp", context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    jmxExporterTestEnvironment.initialize(RMISSLFromYamlTest.class, network);
                    Attachment attachment = new Attachment();
                    attachment.network = network;
                    attachment.jmxExporterTestEnvironment = jmxExporterTestEnvironment;
                    context.setAttachment(attachment);
                }),
                tests,
                Direct.of("tearDown", context -> {
                    Attachment attachment = context.removeAttachment()
                            .flatMap(a -> a.to(Attachment.class))
                            .orElse(null);

                    if (attachment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(attachment.jmxExporterTestEnvironment)
                                .addCloseable(attachment.network)
                                .runAndThrow();
                    }
                }));
    }

    private static class Attachment {
        public Network network;
        public JmxExporterTestEnvironment jmxExporterTestEnvironment;

        public Attachment() {}
    }

    private static void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        boolean isJmxExporterModeJavaAgent =
                jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName = TestSupport.getBuildInfoName(jmxExporterTestEnvironment.getJmxExporterMode());

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
