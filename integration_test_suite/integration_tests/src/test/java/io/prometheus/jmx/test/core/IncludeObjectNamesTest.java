/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.core;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import io.prometheus.jmx.test.support.util.TestSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class IncludeObjectNamesTest {

    @Verifyica.ArgumentSupplier(parallelism = Integer.MAX_VALUE)
    public static Stream<JmxExporterTestEnvironment> arguments() {
        return JmxExporterTestEnvironment.createEnvironments();
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = TestSupport.getOrCreateNetwork(argumentContext);
        TestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testHealthy(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.HEALTHY);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    @Verifyica.Test
    public void testPrometheusTextMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    @Verifyica.Test
    public void testPrometheusProtobufMetrics(JmxExporterTestEnvironment jmxExporterTestEnvironment)
            throws IOException {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url,
                        HttpHeader.ACCEPT,
                        MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(new Trap(() -> TestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> TestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    private void assertMetricsResponse(
            HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

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
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_exporter"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_config"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_scrape"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jvm_"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("process_"))
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
