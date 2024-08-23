package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.throttle.ExponentialBackoffThrottle;
import io.prometheus.jmx.test.support.throttle.Throttle;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.Verifyica;
import org.junit.jupiter.api.Assertions;
import org.testcontainers.containers.Network;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/** Class to implement OpenTelemetryTest */
@Verifyica.Order(order = 1)
public class OpenTelemetryTest {

    public static final String NETWORK = "network";

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        Collection<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PrometheusDockerImages.names()
                .forEach(
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

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        // Create a Network and get the id to force the network creation
        Network network = Network.newNetwork();
        network.getId();

        argumentContext.getStore().put(NETWORK, network);

        Class<?> testClass = argumentContext.getClassContext().getTestClass();

        OpenTelemetryTestEnvironment openTelemetryTestEnvironment =
                argumentContext.getTestArgument(OpenTelemetryTestEnvironment.class).getPayload();

        openTelemetryTestEnvironment.initialize(testClass, network);
    }

    /** Method to test that Prometheus is up */
    @Verifyica.Test
    @Verifyica.Order(order = 0)
    public void testIsPrometheusUp(ArgumentContext argumentContext) {
        OpenTelemetryTestEnvironment openTelemetryTestEnvironment =
                argumentContext.getTestArgument(OpenTelemetryTestEnvironment.class).getPayload();

        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        AtomicBoolean success = new AtomicBoolean();

        for (int i = 0; i < 10; i++) {
            sendPrometheusQuery(openTelemetryTestEnvironment, "up")
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
    @Verifyica.Test
    public void testPrometheusHasMetrics(ArgumentContext argumentContext) {
        OpenTelemetryTestEnvironment openTelemetryTestEnvironment =
                argumentContext.getTestArgument(OpenTelemetryTestEnvironment.class).getPayload();

        boolean isJmxExporterModeJavaStandalone =
                openTelemetryTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

        ExpectedMetricsNames.getMetricsNames().stream()
                .filter(
                        metricName ->
                                !isJmxExporterModeJavaStandalone
                                        || (!metricName.startsWith("jvm_")
                                                && !metricName.startsWith("process_")))
                .forEach(
                        metricName -> {
                            Double value =
                                    getPrometheusMetric(openTelemetryTestEnvironment, metricName);
                            assertThat(value).as("metricName [%s]", metricName).isNotNull();
                            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
                        });
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.getTestArgument(OpenTelemetryTestEnvironment.class))
                .ifPresent(
                        openTelemetryTestEnvironmentArgument ->
                                openTelemetryTestEnvironmentArgument.getPayload().destroy());

        Optional.ofNullable(argumentContext.getStore().remove(NETWORK, Network.class))
                .ifPresent(Network::close);
    }

    /**
     * Method to get a Prometheus metric
     *
     * @param openTelemetryTestEnvironment openTelemetryTestEnvironment
     * @param metricName metricName
     * @return the metric value, or null if it doesn't exist
     */
    protected Double getPrometheusMetric(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String metricName) {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        AtomicReference<Double> value = new AtomicReference<>();

        for (int i = 0; i < 10; i++) {
            sendPrometheusQuery(openTelemetryTestEnvironment, metricName)
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
     * @param openTelemetryTestEnvironment openTelemetryTestEnvironment
     * @param query query
     * @return an HttpResponse
     */
    protected HttpResponse sendPrometheusQuery(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String query) {
        return sendRequest(
                openTelemetryTestEnvironment,
                "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Method to send a Http GET request
     *
     * @param openTelemetryTestEnvironment openTelemetryTestEnvironment
     * @param path path
     * @return an HttpResponse
     */
    protected HttpResponse sendRequest(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String path) {
        return openTelemetryTestEnvironment.getPrometheusHttpClient().send(new HttpRequest(path));
    }
}
