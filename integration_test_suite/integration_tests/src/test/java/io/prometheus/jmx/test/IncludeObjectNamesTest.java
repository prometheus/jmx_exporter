/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test;

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.TestEnvironmentFactory;
import io.prometheus.jmx.test.support.TestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class IncludeObjectNamesTest {

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<ExporterTestEnvironment> arguments() {
        return TestEnvironmentFactory.createExporterTestEnvironments();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        TestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/-/healthy";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testMetrics(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/openmetrics-text; version=1.0.0; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, "CONTENT-TYPE", "text/plain; version=0.0.4; charset=utf-8");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + "/metrics";
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        "CONTENT-TYPE",
                        "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily;"
                                + " encoding=delimited");

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> TestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> TestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        new Trap(() -> TestSupport.destroyNetwork(classContext)).assertEmpty();
    }

    private void assertMetricsResponse(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertCommonMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        /*
         * We have to filter metrics that start with ...
         *
         * jmx_exporter
         * jmx_config
         * jmx_scrape
         * jvm_
         * process_
         *
         * ... because they are registered directly and are not MBeans
         */
        metrics.stream()
                .filter(
                        metric ->
                                !metric.name()
                                        .toLowerCase(Locale.ENGLISH)
                                        .startsWith("jmx_exporter"))
                .filter(
                        metric ->
                                !metric.name().toLowerCase(Locale.ENGLISH).startsWith("jmx_config"))
                .filter(
                        metric ->
                                !metric.name().toLowerCase(Locale.ENGLISH).startsWith("jmx_scrape"))
                .filter(metric -> !metric.name().toLowerCase(Locale.ENGLISH).startsWith("jvm_"))
                .filter(metric -> !metric.name().toLowerCase(Locale.ENGLISH).startsWith("process_"))
                .forEach(
                        metric -> {
                            String name = metric.name();
                            boolean match =
                                    name.startsWith("java_lang")
                                            || name.startsWith("io_prometheus_jmx");
                            assertThat(match).isTrue();
                        });
    }
}
