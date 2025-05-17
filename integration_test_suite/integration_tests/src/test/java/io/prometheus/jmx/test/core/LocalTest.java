/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.core;

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MapMetricAssertion.assertMetric;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.AutoIncrementing;
import io.prometheus.jmx.BuildInfoMetrics;
import io.prometheus.jmx.CustomValue;
import io.prometheus.jmx.ExistDb;
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.OptionalValue;
import io.prometheus.jmx.PerformanceMetrics;
import io.prometheus.jmx.StringValue;
import io.prometheus.jmx.TabularData;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.test.support.Repeater;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.MalformedObjectNameException;
import org.verifyica.api.Argument;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Verifyica;

@Verifyica.Disabled
public class LocalTest {

    private static final int COUNT = 10;

    private static final int PORT = 9876;

    private static final String URL = "http://localhost" + ":" + PORT;

    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<Argument<String>> arguments() {
        List<Argument<String>> arguments = new ArrayList<>();

        for (int i = 1; i < COUNT + 1; i++) {
            arguments.add(Argument.ofString("client " + i));
        }

        return arguments.stream();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) throws Throwable {
        new TabularData().register();
        new AutoIncrementing().register();
        new ExistDb().register();
        new OptionalValue().register();
        new PerformanceMetrics().register();
        new CustomValue().register();
        new StringValue().register();

        String resource =
                (classContext.getTestClass().getName().replace(".", "/") + "/exporter.yaml");
        String exporterYaml = ResourceSupport.load(resource);

        new BuildInfoMetrics().register(DEFAULT_REGISTRY);

        JmxCollector jmxCollector = new JmxCollector(exporterYaml);
        jmxCollector.register(DEFAULT_REGISTRY);

        classContext.map().put("jmxCollector", jmxCollector);

        File file = File.createTempFile("exporter", ".yaml");
        file.deleteOnExit();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file))) {
            bufferedWriter.write(exporterYaml);
        }

        final HTTPServer httpServer =
                HTTPServerFactory.createHTTPServer(
                        InetAddress.getLoopbackAddress(), PORT, DEFAULT_REGISTRY, file);

        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    if (httpServer != null) {
                                        try {
                                            httpServer.stop();
                                        } catch (Throwable t) {
                                            // INTENTIONALLY BLANK
                                        }
                                    }
                                }));

        classContext.map().put("httpServer", httpServer);
    }

    @Verifyica.Disabled
    @Verifyica.Test
    @Verifyica.Order(1)
    public void testPrometheusRegistry(ArgumentContext argumentContext) {
        MetricSnapshots metricSnapshots = DEFAULT_REGISTRY.scrape();

        List<MetricMetadata> metricMetadataList =
                metricSnapshots.stream()
                        .map(MetricSnapshot::getMetadata)
                        .collect(Collectors.toList());

        Map<String, MetricMetadata> metricMetadataMap = new LinkedHashMap<>();
        for (MetricMetadata metricMetadata : metricMetadataList) {
            metricMetadataMap.put(metricMetadata.getName(), metricMetadata);
        }

        assertThat(metricMetadataMap)
                .containsKey("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size");

        assertThat(metricMetadataMap)
                .containsKey("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent");

        assertThat(metricMetadataMap)
                .containsKey(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions");

        assertThat(metricMetadataMap)
                .containsKey(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps");

        for (MetricMetadata metricMetadata : metricMetadataList) {
            assertThat(metricMetadata.getName()).doesNotStartWith("java_");
            assertThat(metricMetadata.getName()).doesNotStartWith("jvm_");
            assertThat(metricMetadata.getName()).doesNotStartWith("com_sun_management");
        }
    }

    @Verifyica.Test
    @Verifyica.Order(2)
    public void testHealthy(ArgumentContext argumentContext) throws Throwable {
        HttpResponse httpResponse = HttpClient.sendRequest(URL + "/-/healthy");

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(ArgumentContext argumentContext) throws Throwable {
        new Repeater(100)
                .test(
                        () -> {
                            HttpResponse httpResponse = HttpClient.sendRequest(URL + "/metrics");

                            assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
                        })
                .run();
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(ArgumentContext argumentContext) throws Throwable {
        new Repeater(100)
                .test(
                        () -> {
                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            URL + "/metrics",
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.OPEN_METRICS_TEXT_METRICS
                                                    .toString());

                            assertMetricsResponse(
                                    httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                        })
                .run();
    }

    @Verifyica.Disabled
    @Verifyica.Test
    public void testPrometheusTextMetrics(ArgumentContext argumentContext) throws Throwable {
        new Repeater(100)
                .test(
                        () -> {
                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            URL + "/metrics",
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

                            assertMetricsResponse(
                                    httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                        })
                .run();
    }

    @Verifyica.Disabled
    @Verifyica.Test
    public void testPrometheusProtobufMetrics(ArgumentContext argumentContext) throws Throwable {
        new Repeater(100)
                .test(
                        () -> {
                            HttpResponse httpResponse =
                                    HttpClient.sendRequest(
                                            URL + "/metrics",
                                            HttpHeader.ACCEPT,
                                            MetricsContentType.PROMETHEUS_PROTOBUF_METRICS
                                                    .toString());

                            assertMetricsResponse(
                                    httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                        })
                .run();
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext)
            throws IOException, MalformedObjectNameException {
        HTTPServer httpServer = classContext.map().getAs("httpServer", HTTPServer.class);
        if (httpServer != null) {
            httpServer.stop();
        }

        classContext.map().clear();
    }

    private void assertMetricsResponse(
            HttpResponse httpResponse, MetricsContentType metricsContentType) {
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

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_optionalValue_Value")
                .withValue(345.0d)
                .isPresent();

        /*
        for (String metricName : metrics.keySet()) {
            String lowerCaseMetricName = metricName.toLowerCase();
            assertThat(lowerCaseMetricName).doesNotStartWith("jvm_");
            assertThat(lowerCaseMetricName).doesNotStartWith("java_");
        }
        */
    }
}
