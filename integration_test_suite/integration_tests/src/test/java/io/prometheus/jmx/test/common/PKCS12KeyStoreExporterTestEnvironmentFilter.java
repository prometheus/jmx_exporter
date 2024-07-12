package io.prometheus.jmx.test.common;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Class to implement PKCS12KeyStoreExporterContextFilter */
public class PKCS12KeyStoreExporterTestEnvironmentFilter
        implements Predicate<ExporterTestEnvironment> {

    private final Set<String> filteredDockerImages;

    /** Constructor */
    public PKCS12KeyStoreExporterTestEnvironmentFilter() {
        filteredDockerImages = new HashSet<>();
        filteredDockerImages.add("eclipse-temurin:8-alpine");
        filteredDockerImages.add("ghcr.io/graalvm/jdk:java8");
        filteredDockerImages.add("ibmjava:8");
        filteredDockerImages.add("ibmjava:8-jre");
        filteredDockerImages.add("ibmjava:8-sdk");
        filteredDockerImages.add("ibmjava:8-sfj");
        filteredDockerImages.add("ibmjava:11");
    }

    /**
     * Evaluates this predicate on the given argument.
     *
     * @param ExporterTestEnvironment the input argument
     * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
     */
    @Override
    public boolean test(ExporterTestEnvironment ExporterTestEnvironment) {
        return !filteredDockerImages.contains(ExporterTestEnvironment.getJavaDockerImage());
    }
}
