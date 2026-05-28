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
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.Collection;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Lifecycle;
import org.paramixel.api.action.Parallel;
import org.paramixel.api.action.Spec;

public class CompositeKeyDataTest {

    private final JmxExporterTestEnvironment environment;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Spec<?> factory() throws Throwable {
        return Parallel.of(CompositeKeyDataTest.class.getName())
                .each(
                        JmxExporterTestEnvironment.createTestEnvironments(CompositeKeyDataTest.class),
                        environment -> Instance.of(environment.name(), () -> new CompositeKeyDataTest(environment))
                                .child(Lifecycle.<CompositeKeyDataTest>of("lifecycle")
                                        .before("setUp()", CompositeKeyDataTest::setUp)
                                        .child("testHealthy()", CompositeKeyDataTest::testHealthy)
                                        .child("testDefaultTextMetrics()", CompositeKeyDataTest::testDefaultTextMetrics)
                                        .child(
                                                "testOpenMetricsTextMetrics()",
                                                CompositeKeyDataTest::testOpenMetricsTextMetrics)
                                        .child(
                                                "testPrometheusTextMetrics()",
                                                CompositeKeyDataTest::testPrometheusTextMetrics)
                                        .child(
                                                "testPrometheusProtobufMetrics()",
                                                CompositeKeyDataTest::testPrometheusProtobufMetrics)
                                        .after("tearDown()", CompositeKeyDataTest::tearDown)));
    }

    private CompositeKeyDataTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        environment.initialize();
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

    public void tearDown() {
        environment.close();
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "1")
                .withLabel("key_path", "/db/query1.xq")
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "2")
                .withLabel("key_path", "/db/query2.xq")
                .isPresent();
    }
}
