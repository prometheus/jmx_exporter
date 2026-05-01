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

package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.environment.PrometheusTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.throttle.ExponentialBackoffThrottle;
import io.prometheus.jmx.test.support.throttle.Throttle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.paramixel.core.Action;
import org.paramixel.core.ConsoleRunner;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.action.StrictSequential;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class MixedConfigurationTest {

    private static class Attachment {
        public Network network;
        public OpenTelemetryTestEnvironment openTelemetryTestEnvironment;
        public JmxExporterTestEnvironment jmxExporterTestEnvironment;
        public PrometheusTestEnvironment prometheusTestEnvironment;

        public Attachment() {}
    }

    public static void main(String[] args) {
        ConsoleRunner.runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                MixedConfigurationTest.class.getName(),
                OpenTelemetryTestEnvironment.createEnvironments()
                        .map(MixedConfigurationTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(OpenTelemetryTestEnvironment openTelemetryTestEnvironment) {
        JmxExporterTestEnvironment jmxExporterTestEnvironment = openTelemetryTestEnvironment.exporterTestEnvironment();
        PrometheusTestEnvironment prometheusTestEnvironment = openTelemetryTestEnvironment.prometheusTestEnvironment();

        Action testPrometheusHasMetrics = Direct.of("testPrometheusHasMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();

            boolean isJmxExporterModeJavaStandalone =
                    attachment.jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

            for (String metricName : ExpectedMetricsNames.getMetricsNames().stream()
                    .filter(metricName -> !isJmxExporterModeJavaStandalone
                            || (!metricName.startsWith("jvm_") && !metricName.startsWith("process_")))
                    .collect(Collectors.toList())) {
                Double value = getPrometheusMetric(attachment.prometheusTestEnvironment, metricName);

                assertThat(value).as("metricName [%s]", metricName).isNotNull();
                assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
            }
        });

        Action tests = StrictSequential.of("tests", List.of(testPrometheusHasMetrics));

        return Lifecycle.of(
                openTelemetryTestEnvironment.getName(),
                Direct.of("setUp", context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    prometheusTestEnvironment.initialize(MixedConfigurationTest.class, network);
                    prometheusTestEnvironment.waitForReady();
                    jmxExporterTestEnvironment.initialize(MixedConfigurationTest.class, network);
                    Attachment attachment = new Attachment();
                    attachment.network = network;
                    attachment.openTelemetryTestEnvironment = openTelemetryTestEnvironment;
                    attachment.jmxExporterTestEnvironment = jmxExporterTestEnvironment;
                    attachment.prometheusTestEnvironment = prometheusTestEnvironment;
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
                                .addCloseable(attachment.prometheusTestEnvironment)
                                .addCloseable(attachment.network)
                                .runAndThrow();
                    }
                }));
    }

    protected static Double getPrometheusMetric(PrometheusTestEnvironment prometheusTestEnvironment, String metricName)
            throws IOException {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        Double value = null;

        for (int i = 0; i < 10; i++) {
            HttpResponse httpResponse = sendPrometheusQuery(prometheusTestEnvironment, metricName);

            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body()).isNotNull();
            assertThat(httpResponse.body().string()).isNotNull();

            if (httpResponse.body().string().contains(metricName)) {
                value = 1.0;
                break;
            }

            throttle.throttle();
        }

        return value;
    }

    protected static HttpResponse sendPrometheusQuery(PrometheusTestEnvironment prometheusTestEnvironment, String query)
            throws IOException {
        return sendRequest(
                prometheusTestEnvironment, "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    protected static HttpResponse sendRequest(PrometheusTestEnvironment prometheusTestEnvironment, String path)
            throws IOException {
        return HttpClient.sendRequest(prometheusTestEnvironment.getPrometheusUrl(path));
    }
}
