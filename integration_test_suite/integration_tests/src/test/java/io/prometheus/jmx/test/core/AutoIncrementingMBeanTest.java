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
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseCollection;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import org.altcontainers.api.Network;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.exception.FailException;

public class AutoIncrementingMBeanTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        AutoIncrementingMBeanTest.class.getName(),
                        JmxExporterTestEnvironment.createTestEnvironments(AutoIncrementingMBeanTest.class),
                        environment -> instance(environment.name(), () -> new AutoIncrementingMBeanTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        AutoIncrementingMBeanTest.class,
                                                        AutoIncrementingMBeanTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest
                                                                        ::testPrometheusProtobufMetrics)))
                                                .child(step(
                                                        "testAutoIncrementingMBean()",
                                                        withInstance(
                                                                AutoIncrementingMBeanTest.class,
                                                                AutoIncrementingMBeanTest::testAutoIncrementingMBean))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        AutoIncrementingMBeanTest.class,
                                                        AutoIncrementingMBeanTest::tearDown)))))
                .build();
    }

    private AutoIncrementingMBeanTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = Network.create();
        environment.initialize(network);
    }

    public void testHealthy() throws IOException {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    public void testAutoIncrementingMBean() throws IOException {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        double value1 = collect(url);
        double value2 = collect(url);
        double value3 = collect(url);

        assertThat(value2).isGreaterThan(value1);
        assertThat(value2).isEqualTo(value1 + 1);
        assertThat(value3).isGreaterThan(value2);
        assertThat(value3).isEqualTo(value2 + 1);
    }

    public void tearDown() {
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(AutoIncrementingMBeanTest.class, javaDockerImage, mode, metrics);
    }

    private double collect(String url) throws IOException {
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsContentType(httpResponse, MetricsContentType.DEFAULT);

        Collection<Metric> metrics = parseCollection(httpResponse);

        java.util.Optional<Double> optionalValue = metrics.stream()
                .filter(metric -> metric.name().startsWith("io_prometheus_jmx_autoIncrementing"))
                .map(Metric::value)
                .limit(1)
                .findFirst();

        if (optionalValue.isPresent()) {
            return optionalValue.get();
        }

        throw new FailException("Metric name [io_prometheus_jmx_autoIncrementing] s not present");
    }
}
