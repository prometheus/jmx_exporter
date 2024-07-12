package io.prometheus.jmx.test.opentelemetry;

import java.util.Collection;
import java.util.List;

/** Class to implement PrometheusDockerImages */
public class PrometheusDockerImages {

    private static final Collection<String> PROMETHEUS_DOCKER_IMAGES;

    // Build the immutable list of Docker image names
    static {
        PROMETHEUS_DOCKER_IMAGES =
                List.of(
                        "prom/prometheus:v2.47.2",
                        "prom/prometheus:v2.48.1",
                        "prom/prometheus:v2.49.1",
                        "prom/prometheus:v2.50.1",
                        "prom/prometheus:v2.51.2",
                        "prom/prometheus:v2.52.0",
                        "prom/prometheus:v2.53.1");
    }

    /** Constructor */
    private PrometheusDockerImages() {
        // DO NOTHING
    }

    /**
     * Method to get List of all Docker image names
     *
     * @return the List of Docker image names
     */
    public static Collection<String> names() {
        return PROMETHEUS_DOCKER_IMAGES;
    }
}
