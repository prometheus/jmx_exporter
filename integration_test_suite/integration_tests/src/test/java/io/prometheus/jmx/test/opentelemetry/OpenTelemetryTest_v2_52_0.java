package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.JavaDockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_52_0 */
public class OpenTelemetryTest_v2_52_0 extends AbstractOpenTelemetryTest {

    private static final String PROMETHEUS_DOCKER_IMAGE_NAME = "prom/prometheus:v2.52.0";

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        return buildTestEnvironments(
                PROMETHEUS_DOCKER_IMAGE_NAME,
                JavaDockerImageNames.names().collect(Collectors.toList()),
                JmxExporterMode.values())
                .stream();
    }
}
