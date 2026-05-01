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
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
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

public class WhitelistAndBlacklistObjectNamesTest {

    private static class Attachment {
        public Network network;
        public JmxExporterTestEnvironment environment;

        public Attachment() {}
    }

    public static void main(String[] args) {
        ConsoleRunner.runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                WhitelistAndBlacklistObjectNamesTest.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .map(WhitelistAndBlacklistObjectNamesTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.HEALTHY);
            HttpResponse httpResponse = HttpClient.sendRequest(url);
            assertHealthyResponse(httpResponse);
        });

        Action testDefaultTextMetrics = Direct.of("testDefaultTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);
            HttpResponse httpResponse = HttpClient.sendRequest(url);
            assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
        });

        Action testOpenMetricsTextMetrics = Direct.of("testOpenMetricsTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);
            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
            assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
        });

        Action testPrometheusTextMetrics = Direct.of("testPrometheusTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);
            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
            assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
        });

        Action testPrometheusProtobufMetrics = Direct.of("testPrometheusProtobufMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);
            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
            assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
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
                    jmxExporterTestEnvironment.initialize(WhitelistAndBlacklistObjectNamesTest.class, network);
                    Attachment attachment = new Attachment();
                    attachment.network = network;
                    attachment.environment = jmxExporterTestEnvironment;
                    context.setAttachment(attachment);
                }),
                tests,
                Direct.of("tearDown", context -> {
                    Attachment attachment = context.removeAttachment()
                            .flatMap(a -> a.to(Attachment.class))
                            .orElse(null);

                    if (attachment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(attachment.environment)
                                .addCloseable(attachment.network)
                                .runAndThrow();
                    }
                }));
    }

    private static void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        metrics.forEach(metric -> assertThat(metric.name().toLowerCase()).doesNotStartWith("java_lang"));
    }
}
