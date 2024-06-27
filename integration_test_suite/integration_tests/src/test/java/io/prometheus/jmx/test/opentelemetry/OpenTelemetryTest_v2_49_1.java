package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.DockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_49_1 */
public class OpenTelemetryTest_v2_49_1 extends AbstractOpenTelemetryTest {

    private static final String PROMETHEUS_DOCKER_IMAGE_NAME = "prom/prometheus:v2.49.1";

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        Collection<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        DockerImageNames.names()
                .forEach(
                        javaDockerImageName -> {
                            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                                openTelemetryTestEnvironments.add(
                                        new OpenTelemetryTestEnvironment(
                                                PROMETHEUS_DOCKER_IMAGE_NAME,
                                                javaDockerImageName,
                                                jmxExporterMode));
                            }
                        });

        return openTelemetryTestEnvironments.stream();
    }
}
