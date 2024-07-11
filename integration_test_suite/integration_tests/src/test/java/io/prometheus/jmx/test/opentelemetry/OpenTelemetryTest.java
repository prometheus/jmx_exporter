package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;
import org.antublue.test.engine.extras.throttle.ExponentialBackoffThrottle;
import org.antublue.test.engine.extras.throttle.Throttle;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/** Class to implement OpenTelemetryTest */
@TestEngine.ParallelArgumentTest
public class OpenTelemetryTest {

    private static final List<String> PROMETHEUS_DOCKER_IMAGES = new ArrayList<>();

    // List of Prometheus Docker Images
    static {
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.47.2");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.48.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.49.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.50.1");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.51.2");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.52.0");
        PROMETHEUS_DOCKER_IMAGES.add("prom/prometheus:v2.53.0");
    }

    private Network network;

    @TestEngine.Argument public OpenTelemetryTestEnvironment openTelemetryTestEnvironment;

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

    @TestEngine.Prepare
    public void prepare() {
        // Create a Network and get the id to force the network creation
        network = Network.newNetwork();
        network.getId();
    }

    @TestEngine.BeforeAll
    public void beforeAll() {
        openTelemetryTestEnvironment.initialize(getClass(), network);
    }

    /** Method to test that Prometheus is up */
    @TestEngine.Test
    @TestEngine.Order(order = 0)
    public void testIsPrometheusUp() {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        AtomicBoolean success = new AtomicBoolean();

        for (int i = 0; i < 10; i++) {
            sendPrometheusQuery("up")
                    .accept(
                            httpResponse -> {
                                assertThat(httpResponse).isNotNull();

                                if (httpResponse.statusCode() != 200) {
                                    return;
                                }

                                assertThat(httpResponse.body()).isNotNull();
                                assertThat(httpResponse.body().string()).isNotNull();

                                Map<Object, Object> map =
                                        new Yaml().load(httpResponse.body().string());

                                String status = (String) map.get("status");
                                assertThat(status).isEqualTo("success");

                                success.set(true);
                            });

            if (success.get()) {
                break;
            }

            throttle.throttle();
        }

        if (!success.get()) {
            Assertions.fail("Prometheus is not up");
        }
    }

    /** Method to test that metrics exist in Prometheus */
    @TestEngine.Test
    public void testPrometheusHasMetrics() {
        ExpectedMetricsNames.getMetricsNames().stream()
                .filter(
                        metricName -> {
                            if (openTelemetryTestEnvironment.getJmxExporterMode()
                                                    == JmxExporterMode.Standalone
                                            && metricName.startsWith("jvm_")
                                    || metricName.startsWith("process_")) {
                                return false;
                            }
                            return true;
                        })
                .forEach(
                        metricName -> {
                            Double value = getPrometheusMetric(metricName);
                            assertThat(value).as("metricName [%s]", metricName).isNotNull();
                            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
                        });
    }

    @TestEngine.AfterAll
    public void afterAll() {
        openTelemetryTestEnvironment.destroy();
    }

    @TestEngine.Conclude
    public void conclude() {
        if (network != null) {
            network.close();
        }
    }

    /**
     * Method to get a Prometheus metric
     *
     * @param metricName metricName
     * @return the metric value, or null if it doesn't exist
     */
    protected Double getPrometheusMetric(String metricName) {
        return getPrometheusMetric(metricName, null);
    }

    /**
     * Method to get a Prometheus metrics
     *
     * @param metricName metricName
     * @param labels labels
     * @return the metric value, or null if it doesn't exist
     */
    protected Double getPrometheusMetric(String metricName, String[] labels) {
        Throttle throttle = new ExponentialBackoffThrottle(100, 6400);
        AtomicReference<Double> value = new AtomicReference<>();

        for (int i = 0; i < 10; i++) {
            sendPrometheusQuery(metricName)
                    .accept(
                            httpResponse -> {
                                assertThat(httpResponse).isNotNull();
                                assertThat(httpResponse.statusCode()).isEqualTo(200);
                                assertThat(httpResponse.body()).isNotNull();
                                assertThat(httpResponse.body().string()).isNotNull();

                                // TODO parse response and return value
                                if (httpResponse.body().string().contains(metricName)) {
                                    value.set(1.0);
                                }
                            });

            if (value.get() != null) {
                break;
            }

            throttle.throttle();
        }

        return value.get();
    }

    /**
     * Method to send a Prometheus query
     *
     * @param query query
     * @return an HttpResponse
     */
    protected HttpResponse sendPrometheusQuery(String query) {
        return sendRequest(
                "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Method to send an Http GET request
     *
     * @param path path
     * @return an HttpResponse
     */
    protected HttpResponse sendRequest(String path) {
        return openTelemetryTestEnvironment.getPrometheusHttpClient().send(new HttpRequest(path));
    }
}
