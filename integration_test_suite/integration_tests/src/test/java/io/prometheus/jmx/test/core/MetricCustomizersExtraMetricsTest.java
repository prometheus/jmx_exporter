/*
 * Copyright (C) 2023-present The Prometheus jmx_exporter Authors
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
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ExporterPath;
import io.prometheus.jmx.test.support.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.TestSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class MetricCustomizersExtraMetricsTest {

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<ExporterTestEnvironment> arguments() {
        return ExporterTestEnvironment.createExporterTestEnvironments();
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
    @Verifyica.Order(1)
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws
            IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.HEALTHY);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(exporterTestEnvironment, httpResponse, MetricsContentType.DEFAULT);
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

        assertMetricsResponse(
                exporterTestEnvironment,
                httpResponse,
                MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    @Verifyica.Test
    public void testPrometheusTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

        assertMetricsResponse(
                exporterTestEnvironment, httpResponse,
                MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    @Verifyica.Test
    public void testPrometheusProtobufMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

        assertMetricsResponse(
                exporterTestEnvironment,
                httpResponse,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
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
            ExporterTestEnvironment exporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
        assertCommonMetricsResponse(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        /*
         * Assert that the mbean has a certain metric
         */
        metrics.stream()
                .filter(metric -> metric.name().equals("io_prometheus_jmx_stringValue_isActive"))
                .forEach(
                        metric ->
                                assertThat(metric.value())
                                        .isEqualTo(1));
    }
}
