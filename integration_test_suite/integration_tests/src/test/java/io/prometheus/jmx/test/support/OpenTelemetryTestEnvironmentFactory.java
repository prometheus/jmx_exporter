package io.prometheus.jmx.test.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/** Class to implement OpenTelemetryTestEnvironmentFactory */
public class OpenTelemetryTestEnvironmentFactory {

    /** Constructor */
    private OpenTelemetryTestEnvironmentFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Create the OpenTelemetryTestEnvironments
     *
     * @return a Stream of OpenTelemetryTestEnvironments
     */
    public static Stream<OpenTelemetryTestEnvironment> createOpenTelemetryTestEnvironments() {
        Collection<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PrometheusDockerImages.names()
                .forEach(
                        prometheusDockerImage ->
                                JavaDockerImages.names()
                                        .forEach(
                                                javaDockerImageName -> {
                                                    for (JmxExporterMode jmxExporterMode :
                                                            JmxExporterMode.values()) {
                                                        openTelemetryTestEnvironments.add(
                                                                new OpenTelemetryTestEnvironment(
                                                                        prometheusDockerImage,
                                                                        javaDockerImageName,
                                                                        jmxExporterMode));
                                                    }
                                                }));

        return openTelemetryTestEnvironments.stream();
    }
}
