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
import org.verifyica.api.Named;

/** Class to implement Environment */
public class OpenTelemetryTestEnvironment implements Named {

    private final String id;
    private final PrometheusTestEnvironment prometheusTestEnvironment;
    private final JmxExporterTestEnvironment jmxExporterTestEnvironment;

    /**
     * Constructor
     *
     * @param prometheusTestEnvironment PrometheusTestEnvironment
     * @param jmxExporterTestEnvironment ExporterTestEnvironment
     */
    public OpenTelemetryTestEnvironment(
            PrometheusTestEnvironment prometheusTestEnvironment,
            JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        this.id = UUID.randomUUID().toString();
        this.prometheusTestEnvironment = prometheusTestEnvironment;
        this.jmxExporterTestEnvironment = jmxExporterTestEnvironment;
    }

    @Override
    public String getName() {
        return jmxExporterTestEnvironment.getJmxExporterMode()
                + " / "
                + jmxExporterTestEnvironment.getJavaDockerImage()
                + " / "
                + prometheusTestEnvironment.getPrometheusDockerImage();
    }

    /**
     * Method to get the ID of the test environment
     *
     * @return the ID of the test environment
     */
    public String getId() {
        return id;
    }

    /**
     * Method to get the PrometheusTestEnvironment
     *
     * @return the PrometheusTestEnvironment
     */
    public PrometheusTestEnvironment prometheusTestEnvironment() {
        return prometheusTestEnvironment;
    }

    /**
     * Method to get the ExporterTestEnvironment
     *
     * @return the ExporterTestEnvironment
     */
    public JmxExporterTestEnvironment exporterTestEnvironment() {
        return jmxExporterTestEnvironment;
    }

    /**
     * Method to create a Stream of Environments
     *
     * @return a Stream of Environments
     */
    public static Stream<OpenTelemetryTestEnvironment> createEnvironments() {
        List<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PrometheusDockerImages.names()
                .forEach(
                        prometheusDockerImage ->
                                JavaDockerImages.names()
                                        .forEach(
                                                javaDockerImage -> {
                                                    for (JmxExporterMode jmxExporterMode :
                                                            JmxExporterMode.values()) {
                                                        openTelemetryTestEnvironments.add(
                                                                new OpenTelemetryTestEnvironment(
                                                                        new PrometheusTestEnvironment(
                                                                                prometheusDockerImage),
                                                                        new JmxExporterTestEnvironment(
                                                                                javaDockerImage,
                                                                                jmxExporterMode)));
                                                    }
                                                }));

        return openTelemetryTestEnvironments.stream();
    }
}
