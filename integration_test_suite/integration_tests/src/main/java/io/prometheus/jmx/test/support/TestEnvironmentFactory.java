package io.prometheus.jmx.test.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/** Class to implement TestEnvironmentFactory */
public class TestEnvironmentFactory {

    /** Constructor */
    private TestEnvironmentFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Create the ExporterTestEnvironments
     *
     * @return a Stream of ExporterTestEnvironments
     */
    public static Stream<ExporterTestEnvironment> createExporterTestEnvironments() {
        Collection<ExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names()
                .forEach(
                        dockerImageName -> {
                            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                                collection.add(
                                        new ExporterTestEnvironment(
                                                dockerImageName, jmxExporterMode));
                            }
                        });

        return collection.stream();
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
