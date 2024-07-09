package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_53_0 */
@TestEngine.Parallelize
public class OpenTelemetryTest extends AbstractOpenTelemetryTest {

    private static final List<String> PROMETHEUS_DOCKER_IMAGES = new ArrayList<>();

    static {
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.47.2");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.48.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.49.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.50.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.51.2");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.52.0");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.53.0");
    }

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        Collection<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PROMETHEUS_DOCKER_IMAGES.forEach(
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
