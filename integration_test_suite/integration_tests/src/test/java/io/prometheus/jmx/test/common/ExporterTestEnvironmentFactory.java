package io.prometheus.jmx.test.common;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/** Class to implement ExporterTestEnvironmentFactory */
public class ExporterTestEnvironmentFactory {

    /** Constructor */
    private ExporterTestEnvironmentFactory() {
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
}
