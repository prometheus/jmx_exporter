package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.JavaDockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_53_0 */
public class OpenTelemetryTest_v2_53_0 extends AbstractOpenTelemetryTest {

    private static final String PROMETHEUS_DOCKER_IMAGE_NAME = "prom/prometheus:v2.53.0";

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
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
