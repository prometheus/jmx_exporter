package io.prometheus.jmx.test.common;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Class to implement PBKDF2WithHmacTestArgumentFilter */
public class PBKDF2WithHmacExporterTestEnvironmentFilter
        implements Predicate<ExporterTestEnvironment> {

    private final Set<String> filteredDockerImages;

    /** Constructor */
    public PBKDF2WithHmacExporterTestEnvironmentFilter() {
        // Filter out Docker image names that don't support PBKDF2 with HMAC
        filteredDockerImages = new HashSet<>();
        filteredDockerImages.add("ibmjava:8");
        filteredDockerImages.add("ibmjava:8-jre");
        filteredDockerImages.add("ibmjava:8-sdk");
        filteredDockerImages.add("ibmjava:8-sfj");
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
