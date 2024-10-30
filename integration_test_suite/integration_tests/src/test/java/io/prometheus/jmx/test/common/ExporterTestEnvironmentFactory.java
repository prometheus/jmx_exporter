package io.prometheus.jmx.test.common;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public class ExporterTestEnvironmentFactory {

    private ExporterTestEnvironmentFactory() {
        // INTENTIONALLY BLANK
    }

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
}
