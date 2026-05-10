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

package io.prometheus.jmx.test.support.environment;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.PrometheusDockerImages;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Combines a Prometheus test environment and a JMX exporter test environment
 * for OpenTelemetry integration testing.
 */
public class OpenTelemetryTestEnvironment {

    private final String id;
    private final PrometheusTestEnvironment prometheusTestEnvironment;
    private final JmxExporterTestEnvironment jmxExporterTestEnvironment;

    /**
     * Creates an OpenTelemetry test environment combining a Prometheus and JMX exporter environment.
     *
     * @param prometheusTestEnvironment the Prometheus test environment for scraping and OTLP reception
     * @param jmxExporterTestEnvironment the JMX exporter test environment providing the metrics source
     */
    public OpenTelemetryTestEnvironment(
            PrometheusTestEnvironment prometheusTestEnvironment,
            JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        this.id = UUID.randomUUID().toString();
        this.prometheusTestEnvironment = prometheusTestEnvironment;
        this.jmxExporterTestEnvironment = jmxExporterTestEnvironment;
    }

    /**
     * Returns the display name of the test environment, combining mode and Docker images.
     *
     * @return the display name of the test environment
     */
    public String getName() {
        return jmxExporterTestEnvironment.getJmxExporterMode()
                + " ("
                + jmxExporterTestEnvironment.getJavaDockerImage()
                + ","
                + prometheusTestEnvironment.getPrometheusDockerImage()
                + ")";
    }

    /**
     * Returns the unique identifier of the test environment.
     *
     * @return the unique identifier of the test environment
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the Prometheus test environment used for OTLP reception and scraping.
     *
     * @return the {@link PrometheusTestEnvironment}
     */
    public PrometheusTestEnvironment prometheusTestEnvironment() {
        return prometheusTestEnvironment;
    }

    /**
     * Returns the JMX exporter test environment providing the metrics source.
     *
     * @return the {@link JmxExporterTestEnvironment}
     */
    public JmxExporterTestEnvironment exporterTestEnvironment() {
        return jmxExporterTestEnvironment;
    }

    /**
     * Creates a stream of OpenTelemetry test environments for all combinations
     * of configured Prometheus and Java Docker images and exporter modes.
     *
     * @return a stream of {@link OpenTelemetryTestEnvironment} instances
     */
    public static Stream<OpenTelemetryTestEnvironment> createEnvironments() {
        List<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PrometheusDockerImages.names()
                .forEach(prometheusDockerImage -> JavaDockerImages.names().forEach(javaDockerImage -> {
                    for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                        openTelemetryTestEnvironments.add(new OpenTelemetryTestEnvironment(
                                new PrometheusTestEnvironment(prometheusDockerImage),
                                new JmxExporterTestEnvironment(javaDockerImage, jmxExporterMode)));
                    }
                }));

        return openTelemetryTestEnvironments.stream();
    }
}
