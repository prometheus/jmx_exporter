package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.DockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

/** Class to implement OpenTelemetryTest_v2_46_0 */
public class OpenTelemetryTest_v2_45_5 extends AbstractOpenTelemetryTest {

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestArguments> arguments() {
        List<OpenTelemetryTestArguments> openTelemetryTestArguments = new ArrayList<>();

        List<String> prometheusDockerImageNames = new ArrayList<>();
        prometheusDockerImageNames.add("prom/prometheus:v2.46.0");

        prometheusDockerImageNames.forEach(
                prometheusDockerImage ->
                        DockerImageNames.names()
                                .forEach(
                                        javaDockerImageName -> {
                                            for (JmxExporterMode jmxExporterMode :
                                                    JmxExporterMode.values()) {
                                                openTelemetryTestArguments.add(
                                                        OpenTelemetryTestArguments.of(
                                                                prometheusDockerImage
                                                                        + " / "
                                                                        + javaDockerImageName
                                                                        + " / "
                                                                        + jmxExporterMode,
                                                                prometheusDockerImage,
                                                                javaDockerImageName,
                                                                jmxExporterMode));
                                            }
                                        }));

        return openTelemetryTestArguments.stream();
    }
}
