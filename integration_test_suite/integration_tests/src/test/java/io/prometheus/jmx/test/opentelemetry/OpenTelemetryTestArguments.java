package io.prometheus.jmx.test.opentelemetry;

import io.prometheus.jmx.test.support.JmxExporterMode;
import org.antublue.test.engine.api.Argument;

public class OpenTelemetryTestArguments implements Argument<OpenTelemetryTestArguments> {

    private final String name;
    private final String prometheusDockerImageName;
    private final String javaDockerImageName;
    private final JmxExporterMode jmxExporterMode;

    private OpenTelemetryTestArguments(String name, String prometheusDockerImageName, String javaDockerImageName, JmxExporterMode jmxExporterMode) {
        this.name = name;
        this.prometheusDockerImageName = prometheusDockerImageName;
        this.javaDockerImageName = javaDockerImageName;
        this.jmxExporterMode = jmxExporterMode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public OpenTelemetryTestArguments getPayload() {
        return this;
    }

    public String getPrometheusDockerImageName() {
        return prometheusDockerImageName;
    }

    public String getJavaDockerImageName() {
        return javaDockerImageName;
    }

    public JmxExporterMode getJmxExporterMode() {
        return jmxExporterMode;
    }

    @Override
    public String toString() {
        return String.format(
                "TestArgument{name=[%s],dockerImageName=[%s],mode=[%s]}",
                name, javaDockerImageName, jmxExporterMode);
    }

    public static OpenTelemetryTestArguments of(
            String name, String prometheusDockerImageName, String javaDockerImageName, JmxExporterMode jmxExporterMode) {
        return new OpenTelemetryTestArguments(name, prometheusDockerImageName, javaDockerImageName, jmxExporterMode);
    }
}
