package io.prometheus.jmx.test.opentelemetry.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.opentelemetry.ExpectedMetricsNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.TestEnvironmentFactory;
import io.prometheus.jmx.test.support.TestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.throttle.ExponentialBackoffThrottle;
import io.prometheus.jmx.test.support.throttle.Throttle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;
import org.yaml.snakeyaml.Yaml;

/** Class to implement OpenTelemetryBasicAuthenticationTest */
public class OpenTelemetryBasicAuthenticationTest {

    private static final String VALID_USER = "Prometheus";
    private static final String VALUE_PASSWORD = "secret";

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        return TestEnvironmentFactory.createOpenTelemetryTestEnvironments();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        TestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeOpenTelemetryTestEnvironment(argumentContext, network, testClass);
    }

    /** Method to test that Prometheus is up */
    @Verifyica.Test
    @Verifyica.Order(1)
    public void testPrometheusIsUp(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        boolean isUp = false;

        for (int i = 0; i < 10; i++) {
            HttpResponse httpResponse = sendPrometheusQuery(openTelemetryTestEnvironment, "up");

            assertThat(httpResponse).isNotNull();

            if (httpResponse.statusCode() == 200) {
                assertThat(httpResponse.body()).isNotNull();
                assertThat(httpResponse.body().string()).isNotNull();

                Map<Object, Object> map = new Yaml().load(httpResponse.body().string());

                String status = (String) map.get("status");
                assertThat(status).isEqualTo("success");

                isUp = true;
                break;
            } else {
                throttle.throttle();
            }
        }

        assertThat(isUp).withFailMessage("Prometheus is down").isTrue();
    }

    /** Method to test that metrics exist in Prometheus */
    @Verifyica.Test
    @Verifyica.Order(2)
    public void testPrometheusHasMetrics(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        boolean isJmxExporterModeJavaStandalone =
                openTelemetryTestEnvironment.getJmxExporterMode() == JmxExporterMode.Standalone;

        for (String metricName :
                ExpectedMetricsNames.getMetricsNames().stream()
                        .filter(
                                metricName ->
                                        !isJmxExporterModeJavaStandalone
                                                || (!metricName.startsWith("jvm_")
                                                        && !metricName.startsWith("process_")))
                        .collect(Collectors.toList())) {
            Double value = getPrometheusMetric(openTelemetryTestEnvironment, metricName);

            assertThat(value).as("metricName [%s]", metricName).isNotNull();
            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
        }
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> TestSupport.destroyOpenTelemetryTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> TestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        new Trap(() -> TestSupport.destroyNetwork(classContext)).assertEmpty();
    }

    /**
     * Method to get a Prometheus metric
     *
     * @param openTelemetryTestEnvironment openTelemetryTestEnvironment
     * @param metricName metricName
     * @return the metric value, or null if it doesn't exist
     */
    protected Double getPrometheusMetric(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String metricName)
            throws IOException {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        Double value = null;

        for (int i = 0; i < 10; i++) {
            HttpResponse httpResponse =
                    sendPrometheusQuery(openTelemetryTestEnvironment, metricName);

            assertThat(httpResponse).isNotNull();
            assertThat(httpResponse.statusCode()).isEqualTo(200);
            assertThat(httpResponse.body()).isNotNull();
            assertThat(httpResponse.body().string()).isNotNull();

            // TODO parse response and return value
            if (httpResponse.body().string().contains(metricName)) {
                value = 1.0;
                break;
            }

            throttle.throttle();
        }

        return value;
    }

    /**
     * Method to send a Prometheus query
     *
     * @param openTelemetryTestEnvironment openTelemetryTestEnvironment
     * @param query query
     * @return an HttpResponse
     */
    protected HttpResponse sendPrometheusQuery(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String query)
            throws IOException {
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
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment, String path)
            throws IOException {
        return HttpClient.sendRequest(
                HttpRequest.builder()
                        .url(openTelemetryTestEnvironment.getUrl(path))
                        .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                        .build());
    }
}
