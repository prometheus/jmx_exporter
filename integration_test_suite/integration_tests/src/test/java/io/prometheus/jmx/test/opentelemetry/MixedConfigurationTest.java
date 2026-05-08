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
import java.util.stream.Collectors;
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

public class MixedConfigurationTest {

    private static final String NETWORK_KEY = "network";

    private static final String JMX_EXPORTER_TEST_ENVIRONMENT = "jmxExporterTestEnvironment";

    private static final String PROMETHEUS_TEST_ENVIRONMENT = "prometheusTestEnvironment";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(MixedConfigurationTest.class.getName());
        for (OpenTelemetryTestEnvironment environment :
                OpenTelemetryTestEnvironment.createEnvironments().toList()) {
            parallelBuilder.child(argument(environment));
        }
        return parallelBuilder.build();
    }

    private static Action argument(OpenTelemetryTestEnvironment openTelemetryTestEnvironment) {
        Action setUp = setUp(openTelemetryTestEnvironment);
        Action testPrometheusHasMetrics = testPrometheusHasMetrics();
        Action tearDown = tearDown();

        return Container.builder(openTelemetryTestEnvironment.getName())
                .before(setUp)
                .child(testPrometheusHasMetrics)
                .after(tearDown)
                .build();
    }

    private static Action setUp(OpenTelemetryTestEnvironment openTelemetryTestEnvironment) {
        return Direct.builder("setUp")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment jmxExporterTestEnvironment =
                            openTelemetryTestEnvironment.exporterTestEnvironment();
                    PrometheusTestEnvironment prometheusTestEnvironment =
                            openTelemetryTestEnvironment.prometheusTestEnvironment();
                    Network network = Network.newNetwork();
                    network.getId();
                    prometheusTestEnvironment.initialize(MixedConfigurationTest.class, network);
                    prometheusTestEnvironment.waitForReady();
                    jmxExporterTestEnvironment.initialize(MixedConfigurationTest.class, network);
                    context.getStore().put(NETWORK_KEY, Value.of(network));
                    context.getStore().put(JMX_EXPORTER_TEST_ENVIRONMENT, Value.of(jmxExporterTestEnvironment));
                    context.getStore().put(PROMETHEUS_TEST_ENVIRONMENT, Value.of(prometheusTestEnvironment));
                })
                .build();
    }

    private static Action testPrometheusHasMetrics() {
        return Direct.builder("testPrometheusHasMetrics")
                .contextMode(Action.ContextMode.SHARED)
                .execute(context -> {
                    JmxExporterTestEnvironment currentJmxExporterTestEnvironment =
                            getJmxExporterTestEnvironment(context);
                    PrometheusTestEnvironment currentPrometheusTestEnvironment = getPrometheusTestEnvironment(context);

                    boolean isJmxExporterModeJavaStandalone =
                            currentJmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

                    for (String metricName : ExpectedMetricsNames.getMetricsNames().stream()
                            .filter(metricName -> !isJmxExporterModeJavaStandalone
                                    || (!metricName.startsWith("jvm_") && !metricName.startsWith("process_")))
                            .collect(Collectors.toList())) {
                        Double value = getPrometheusMetric(currentPrometheusTestEnvironment, metricName);

                        assertThat(value).as("metricName [%s]", metricName).isNotNull();
                        assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
                    }
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
                    JmxExporterTestEnvironment jmxExporterTestEnvironment = context.getStore()
                            .remove(JMX_EXPORTER_TEST_ENVIRONMENT)
                            .map(value -> value.cast(JmxExporterTestEnvironment.class))
                            .orElse(null);
                    PrometheusTestEnvironment prometheusTestEnvironment = context.getStore()
                            .remove(PROMETHEUS_TEST_ENVIRONMENT)
                            .map(value -> value.cast(PrometheusTestEnvironment.class))
                            .orElse(null);

                    if (network != null && jmxExporterTestEnvironment != null && prometheusTestEnvironment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(jmxExporterTestEnvironment)
                                .addCloseable(prometheusTestEnvironment)
                                .addCloseable(network)
                                .runAndThrow();
                    }
                })
                .build();
    }

    private static JmxExporterTestEnvironment getJmxExporterTestEnvironment(Context context) {
        return context.getStore()
                .get(JMX_EXPORTER_TEST_ENVIRONMENT)
                .orElseThrow()
                .cast(JmxExporterTestEnvironment.class);
    }

    private static PrometheusTestEnvironment getPrometheusTestEnvironment(Context context) {
        return context.getStore().get(PROMETHEUS_TEST_ENVIRONMENT).orElseThrow().cast(PrometheusTestEnvironment.class);
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
