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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

/** Class to implement BasicTest */
public class BasicTest {

    private static final String NETWORK = "network";

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        return OpenTelemetryTestEnvironment.createEnvironments();
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();

        Network network = Network.newNetwork();
        network.getId();

        argumentContext.map().put(NETWORK, network);

        OpenTelemetryTestEnvironment openTelemetryTestEnvironment =
                argumentContext.testArgument().payload(OpenTelemetryTestEnvironment.class);

        PrometheusTestEnvironment prometheusTestEnvironment =
                openTelemetryTestEnvironment.prometheusTestEnvironment();
        prometheusTestEnvironment.initialize(testClass, network);
        prometheusTestEnvironment.waitForReady();

        JmxExporterTestEnvironment jmxExporterTestEnvironment =
                openTelemetryTestEnvironment.exporterTestEnvironment();
        jmxExporterTestEnvironment.initialize(testClass, network);
    }

    /** Method to test that metrics exist in Prometheus */
    @Verifyica.Test
    public void testPrometheusHasMetrics(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        PrometheusTestEnvironment prometheusTestEnvironment =
                openTelemetryTestEnvironment.prometheusTestEnvironment();

        JmxExporterTestEnvironment jmxExporterTestEnvironment =
                openTelemetryTestEnvironment.exporterTestEnvironment();

        boolean isJmxExporterModeJavaStandalone =
                jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

        for (String metricName :
                ExpectedMetricsNames.getMetricsNames().stream()
                        .filter(
                                metricName ->
                                        !isJmxExporterModeJavaStandalone
                                                || (!metricName.startsWith("jvm_")
                                                        && !metricName.startsWith("process_")))
                        .collect(Collectors.toList())) {
            Double value = getPrometheusMetric(prometheusTestEnvironment, metricName);

            assertThat(value).as("metricName [%s]", metricName).isNotNull();
            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
        }
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        OpenTelemetryTestEnvironment openTelemetryTestEnvironment =
                argumentContext.testArgument().payload(OpenTelemetryTestEnvironment.class);

        JmxExporterTestEnvironment jmxExporterTestEnvironment =
                openTelemetryTestEnvironment.exporterTestEnvironment();

        PrometheusTestEnvironment prometheusTestEnvironment =
                openTelemetryTestEnvironment.prometheusTestEnvironment();

        Network network = argumentContext.map().getAs(NETWORK);

        List<Trap> traps = new ArrayList<>();

        if (jmxExporterTestEnvironment != null) {
            traps.add(new Trap(jmxExporterTestEnvironment::destroy));
        }

        if (prometheusTestEnvironment != null) {
            traps.add(new Trap(prometheusTestEnvironment::destroy));
        }

        if (network != null) {
            traps.add(new Trap(network::close));
        }

        Trap.assertEmpty(traps);
    }

    /**
     * Method to get a Prometheus metric
     *
     * @param prometheusTestEnvironment prometheusTestEnvironment
     * @param metricName metricName
     * @return the metric value, or null if it doesn't exist
     */
    protected Double getPrometheusMetric(
            PrometheusTestEnvironment prometheusTestEnvironment, String metricName)
            throws IOException {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        Double value = null;

        for (int i = 0; i < 10; i++) {
            HttpResponse httpResponse = sendPrometheusQuery(prometheusTestEnvironment, metricName);

            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body()).isNotNull();
            assertThat(httpResponse.body().string()).isNotNull();

            // TODO parse response and return value
            if (httpResponse.body().string().contains(metricName)) {
                value = 1.0;
                break;
            }

            throttle.throttle();
        }

        return value;
    }

    /**
     * Method to send a Prometheus query
     *
     * @param prometheusTestEnvironment prometheusTestEnvironment
     * @param query query
     * @return an HttpResponse
     */
    protected HttpResponse sendPrometheusQuery(
            PrometheusTestEnvironment prometheusTestEnvironment, String query) throws IOException {
        return sendRequest(
                prometheusTestEnvironment,
                "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Method to send a Http GET request
     *
     * @param prometheusTestEnvironment prometheusTestEnvironment
     * @param path path
     * @return an HttpResponse
     */
    protected HttpResponse sendRequest(
            PrometheusTestEnvironment prometheusTestEnvironment, String path) throws IOException {
        return HttpClient.sendRequest(prometheusTestEnvironment.getPrometheusUrl(path));
    }
}
