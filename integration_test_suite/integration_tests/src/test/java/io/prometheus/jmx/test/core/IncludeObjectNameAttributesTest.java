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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.Value;
import org.paramixel.core.action.DependentSequential;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class IncludeObjectNameAttributesTest {

    private static final int ENVIRONMENT_LEVEL = 2;

    private static final String ENVIRONMENT_KEY = "environment";

    private static final String NETWORK_KEY = "network";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                IncludeObjectNameAttributesTest.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .map(IncludeObjectNameAttributesTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action testHealthy = Direct.of("testHealthy", IncludeObjectNameAttributesTest::testHealthy);

        Action testDefaultTextMetrics =
                Direct.of("testDefaultTextMetrics", IncludeObjectNameAttributesTest::testDefaultTextMetrics);

        Action testOpenMetricsTextMetrics =
                Direct.of("testOpenMetricsTextMetrics", IncludeObjectNameAttributesTest::testOpenMetricsTextMetrics);

        Action testPrometheusTextMetrics =
                Direct.of("testPrometheusTextMetrics", IncludeObjectNameAttributesTest::testPrometheusTextMetrics);

        Action testPrometheusProtobufMetrics = Direct.of(
                "testPrometheusProtobufMetrics", IncludeObjectNameAttributesTest::testPrometheusProtobufMetrics);

        Action tests = DependentSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                jmxExporterTestEnvironment.getName(),
                Direct.of("setUp", context -> setUp(context, jmxExporterTestEnvironment)),
                tests,
                Direct.of("tearDown", IncludeObjectNameAttributesTest::tearDown));
    }

    private static void setUp(Context context, JmxExporterTestEnvironment jmxExporterTestEnvironment) throws Throwable {
        Network network = Network.newNetwork();
        network.getId();
        jmxExporterTestEnvironment.initialize(IncludeObjectNameAttributesTest.class, network);
        context.getStore().put(NETWORK_KEY, Value.of(network));
        context.getStore().put(ENVIRONMENT_KEY, Value.of(jmxExporterTestEnvironment));
    }

    private static void testHealthy(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.HEALTHY);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertHealthyResponse(httpResponse);
    }

    private static void testDefaultTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    private static void testOpenMetricsTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    private static void testPrometheusTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    private static void testPrometheusProtobufMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(
                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private static void tearDown(Context context) throws Throwable {
        Network network = context.getStore()
                .remove(NETWORK_KEY)
                .map(value -> value.cast(Network.class))
                .orElse(null);
        JmxExporterTestEnvironment environment = context.getStore()
                .remove(ENVIRONMENT_KEY)
                .map(value -> value.cast(JmxExporterTestEnvironment.class))
                .orElse(null);

        if (network != null && environment != null) {
            Cleanup.of(Cleanup.Mode.FORWARD)
                    .addCloseable(environment)
                    .addCloseable(network)
                    .runAndThrow();
        }
    }

    private static JmxExporterTestEnvironment getEnvironment(Context context) {
        return context.findAncestor(ENVIRONMENT_LEVEL)
                .orElseThrow()
                .getStore()
                .get(ENVIRONMENT_KEY)
                .orElseThrow()
                .cast(JmxExporterTestEnvironment.class);
    }

    private static void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        Set<String> includeJavaLangThreadingAttributeSet = new HashSet<>();
        includeJavaLangThreadingAttributeSet.add("java_lang_Threading_ThreadCount");
        includeJavaLangThreadingAttributeSet.add("java_lang_Threading_TotalStartedThreadCount");

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
