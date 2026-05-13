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
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Container;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.paramixel.core.support.Retry;
import org.testcontainers.containers.Network;

public class BasicAuthenticationTest {

    private static final String NETWORK_KEY = "network";

    private static final String JMX_EXPORTER_TEST_ENVIRONMENT = "jmxExporterTestEnvironment";

    private static final String PROMETHEUS_TEST_ENVIRONMENT = "prometheusTestEnvironment";

    private static final String VALID_USER = "Prometheus";
    private static final String VALUE_PASSWORD = "secret";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(BasicAuthenticationTest.class.getName());
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
                .runnable(context -> {
                    JmxExporterTestEnvironment jmxExporterTestEnvironment =
                            openTelemetryTestEnvironment.exporterTestEnvironment();
                    PrometheusTestEnvironment prometheusTestEnvironment =
                            openTelemetryTestEnvironment.prometheusTestEnvironment();
                    Network network = Network.newNetwork();
                    network.getId();
                    prometheusTestEnvironment.initialize(BasicAuthenticationTest.class, network);
                    prometheusTestEnvironment.waitForReady(VALID_USER, VALUE_PASSWORD);
                    jmxExporterTestEnvironment.initialize(BasicAuthenticationTest.class, network);
                    var store = context.getStore();
                    store.put(NETWORK_KEY, network);
                    store.put(JMX_EXPORTER_TEST_ENVIRONMENT, jmxExporterTestEnvironment);
                    store.put(PROMETHEUS_TEST_ENVIRONMENT, prometheusTestEnvironment);
                })
                .build();
    }

    private static Action testPrometheusHasMetrics() {
        return Direct.builder("testPrometheusHasMetrics")
                .runnable(context -> {
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
                .runnable(context -> {
                    var store = context.getStore();
                    Network network = store.remove(NETWORK_KEY, Network.class).orElse(null);
                    JmxExporterTestEnvironment jmxExporterTestEnvironment = store.remove(
                                    JMX_EXPORTER_TEST_ENVIRONMENT, JmxExporterTestEnvironment.class)
                            .orElse(null);
                    PrometheusTestEnvironment prometheusTestEnvironment = store.remove(
                                    PROMETHEUS_TEST_ENVIRONMENT, PrometheusTestEnvironment.class)
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
        return context.getParent()
                .getStore()
                .get(JMX_EXPORTER_TEST_ENVIRONMENT, JmxExporterTestEnvironment.class)
                .orElseThrow();
    }

    private static PrometheusTestEnvironment getPrometheusTestEnvironment(Context context) {
        return context.getParent()
                .getStore()
                .get(PROMETHEUS_TEST_ENVIRONMENT, PrometheusTestEnvironment.class)
                .orElseThrow();
    }

    protected static Double getPrometheusMetric(PrometheusTestEnvironment prometheusTestEnvironment, String metricName)
            throws Throwable {
        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(5)))
                .retryOn(t -> t instanceof RuntimeException)
                .run(() -> {
                    HttpResponse httpResponse = sendPrometheusQuery(prometheusTestEnvironment, metricName);

                    assertThat(httpResponse.statusCode()).isEqualTo(200);
                    assertThat(httpResponse.body()).isNotNull();
                    assertThat(httpResponse.body().string()).isNotNull();

                    if (httpResponse.body().string().contains(metricName)) {
                        return;
                    }

                    throw new RuntimeException("metric not found yet: " + metricName);
                });

        return result.isPass() ? 1.0 : null;
    }

    protected static HttpResponse sendPrometheusQuery(PrometheusTestEnvironment prometheusTestEnvironment, String query)
            throws IOException {
        return sendRequest(
                prometheusTestEnvironment, "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    protected static HttpResponse sendRequest(PrometheusTestEnvironment prometheusTestEnvironment, String path)
            throws IOException {
        return HttpClient.sendRequest(HttpRequest.builder()
                .url(prometheusTestEnvironment.getPrometheusUrl(path))
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
    }
}
