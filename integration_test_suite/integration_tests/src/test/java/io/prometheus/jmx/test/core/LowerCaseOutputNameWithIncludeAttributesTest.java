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
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.NetworkSupport;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.testcontainers.containers.Network;

public class LowerCaseOutputNameWithIncludeAttributesTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        LowerCaseOutputNameWithIncludeAttributesTest.class.getName(),
                        JmxExporterTestEnvironment.createTestEnvironments(
                                LowerCaseOutputNameWithIncludeAttributesTest.class),
                        environment -> Instance.builder(
                                        environment.name(),
                                        () -> new LowerCaseOutputNameWithIncludeAttributesTest(environment))
                                .body(Scope.builder("scenario")
                                        .before(Step.of(
                                                "setUp()",
                                                withInstance(
                                                        LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                        LowerCaseOutputNameWithIncludeAttributesTest::setUp)))
                                        .body(Sequence.builder("tests")
                                                .child(Step.of(
                                                        "testHealthy()",
                                                        withInstance(
                                                                LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                                LowerCaseOutputNameWithIncludeAttributesTest
                                                                        ::testHealthy)))
                                                .child(Step.of(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                                LowerCaseOutputNameWithIncludeAttributesTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(Step.of(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                                LowerCaseOutputNameWithIncludeAttributesTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                                LowerCaseOutputNameWithIncludeAttributesTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(Step.of(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                                LowerCaseOutputNameWithIncludeAttributesTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(Step.of(
                                                "tearDown()",
                                                withInstance(
                                                        LowerCaseOutputNameWithIncludeAttributesTest.class,
                                                        LowerCaseOutputNameWithIncludeAttributesTest::tearDown)))))
                .build();
    }

    private LowerCaseOutputNameWithIncludeAttributesTest(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        network = NetworkSupport.create();
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

    public void tearDown() {
        environment.close();
        NetworkSupport.close(network);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        metrics.forEach(
                metric -> assertThat(metric.name()).isEqualTo(metric.name().toLowerCase()));

        Set<String> includeJavaLangThreadingAttributeSet = new HashSet<>();
        includeJavaLangThreadingAttributeSet.add("java_lang_threading_threadcount");
        includeJavaLangThreadingAttributeSet.add("java_lang_threading_totalstartedthreadcount");

        Set<Metric> includeMetrics = metrics.stream()
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_exporter"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_config"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jmx_scrape"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("jvm_"))
                .filter(metric -> !metric.name().toLowerCase().startsWith("process_"))
                .collect(Collectors.toSet());

        assertThat(includeMetrics).hasSize(includeJavaLangThreadingAttributeSet.size());

        includeMetrics.forEach(metric -> assertThat(includeJavaLangThreadingAttributeSet.contains(metric.name()))
                .isTrue());
    }
}
