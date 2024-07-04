package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_50_1 */
public class OpenTelemetryTest_v2_51_2 extends AbstractOpenTelemetryTest {

    private static final String PROMETHEUS_DOCKER_IMAGE_NAME = "prom/prometheus:v2.51.2";

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        return buildTestEnvironments(
                PROMETHEUS_DOCKER_IMAGE_NAME, JavaDockerImages.names(), JmxExporterMode.values());
    }
}
