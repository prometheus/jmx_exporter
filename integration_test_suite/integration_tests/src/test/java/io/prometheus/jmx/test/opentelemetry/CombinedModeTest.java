package io.prometheus.jmx.test.opentelemetry;

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MapMetricAssertion.assertMetric;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ExporterPath;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.TestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.jmx.test.support.throttle.ExponentialBackoffThrottle;
import io.prometheus.jmx.test.support.throttle.Throttle;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;
import org.yaml.snakeyaml.Yaml;

/** Class to implement CombinedModeTest */
public class CombinedModeTest {

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<OpenTelemetryTestEnvironment> arguments() {
        return OpenTelemetryTestEnvironment.createOpenTelemetryTestEnvironments();
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

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testHealthy(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        String url = openTelemetryTestEnvironment.getExporterUrl(ExporterPath.HEALTHY);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    @Verifyica.Order(2)
    public void testDefaultTextMetrics(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        String url = openTelemetryTestEnvironment.getExporterUrl(ExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(
                openTelemetryTestEnvironment, httpResponse, MetricsContentType.DEFAULT);
    }

    @Verifyica.Test
    @Verifyica.Order(3)
    public void testOpenMetricsTextMetrics(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment) throws IOException {
        String url = openTelemetryTestEnvironment.getExporterUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

        assertMetricsResponse(
                openTelemetryTestEnvironment,
                httpResponse,
                MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    @Verifyica.Test
    @Verifyica.Order(4)
    public void testPrometheusTextMetrics(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        String url = openTelemetryTestEnvironment.getExporterUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

        assertMetricsResponse(
                openTelemetryTestEnvironment,
                httpResponse,
                MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    @Verifyica.Test
    @Verifyica.Order(5)
    public void testPrometheusProtobufMetrics(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment) throws IOException {
        String url = openTelemetryTestEnvironment.getExporterUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

        assertMetricsResponse(
                openTelemetryTestEnvironment,
                httpResponse,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    /** Method to test that Prometheus is up */
    @Verifyica.Test
    @Verifyica.Order(6)
    public void testPrometheusIsUp(OpenTelemetryTestEnvironment openTelemetryTestEnvironment)
            throws IOException {
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);
        boolean isUp = false;

        for (int i = 0; i < 10; i++) {
            HttpResponse httpResponse = sendPrometheusQuery(openTelemetryTestEnvironment, "up");

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
    @Verifyica.Order(7)
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

    private void assertMetricsResponse(
            OpenTelemetryTestEnvironment openTelemetryTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
        assertCommonMetricsResponse(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        // Validate no duplicate metrics (metrics with the same name and labels)
        // and build a Metrics Map for subsequent processing

        Set<String> compositeSet = new LinkedHashSet<>();
        MetricsParser.parseCollection(httpResponse)
                .forEach(
                        metric -> {
                            String name = metric.name();
                            Map<String, String> labels = metric.labels();
                            String composite = name + " " + labels;
                            assertThat(compositeSet).doesNotContain(composite);
                            compositeSet.add(composite);
                            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
                        });

        // Validate common / known metrics (and potentially values)

        boolean isJmxExporterModeJavaAgent =
                openTelemetryTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName =
                isJmxExporterModeJavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_standalone";

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.COUNTER)
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions")
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps")
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred")
                .withValue(6.0d)
                .isPresent();
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
        return HttpClient.sendRequest(openTelemetryTestEnvironment.getPrometheusUrl(path));
    }
}
