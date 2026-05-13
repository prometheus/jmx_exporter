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
import static org.assertj.core.api.Assertions.fail;

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
import java.util.Set;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Container;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class ExcludeObjectNameAttributesTest {

    private static final String ENVIRONMENT_KEY = "environment";
    private static final String NETWORK_KEY = "network";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(ExcludeObjectNameAttributesTest.class.getName());
        for (JmxExporterTestEnvironment environment :
                JmxExporterTestEnvironment.createEnvironments().toList()) {
            parallelBuilder.child(argument(environment));
        }
        return parallelBuilder.build();
    }

    private static Action argument(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action setUp = setUp(jmxExporterTestEnvironment);
        Action testHealthy = testHealthy();
        Action testDefaultTextMetrics = testDefaultTextMetrics();
        Action testOpenMetricsTextMetrics = testOpenMetricsTextMetrics();
        Action testPrometheusTextMetrics = testPrometheusTextMetrics();
        Action testPrometheusProtobufMetrics = testPrometheusProtobufMetrics();
        Action tearDown = tearDown();

        return Container.builder(jmxExporterTestEnvironment.getName())
                .before(setUp)
                .child(testHealthy)
                .child(testDefaultTextMetrics)
                .child(testOpenMetricsTextMetrics)
                .child(testPrometheusTextMetrics)
                .child(testPrometheusProtobufMetrics)
                .after(tearDown)
                .build();
    }

    private static Action setUp(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        return Direct.builder("setUp")
                .runnable(context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    jmxExporterTestEnvironment.initialize(ExcludeObjectNameAttributesTest.class, network);
                    var store = context.getStore();
                    store.put(NETWORK_KEY, network);
                    store.put(ENVIRONMENT_KEY, jmxExporterTestEnvironment);
                })
                .build();
    }

    private static Action testHealthy() {
        return Direct.builder("testHealthy")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.HEALTHY);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertHealthyResponse(httpResponse);
                })
                .build();
    }

    private static Action testDefaultTextMetrics() {
        return Direct.builder("testDefaultTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(url);
                    assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
                })
                .build();
    }

    private static Action testOpenMetricsTextMetrics() {
        return Direct.builder("testOpenMetricsTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
                    assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusTextMetrics() {
        return Direct.builder("testPrometheusTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusProtobufMetrics() {
        return Direct.builder("testPrometheusProtobufMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);
                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
                    assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                })
                .build();
    }

    private static Action tearDown() {
        return Direct.builder("tearDown")
                .runnable(context -> {
                    var store = context.getStore();
                    Network network = store.remove(NETWORK_KEY, Network.class).orElse(null);
                    JmxExporterTestEnvironment environment = store.remove(
                                    ENVIRONMENT_KEY, JmxExporterTestEnvironment.class)
                            .orElse(null);

                    if (network != null && environment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(environment)
                                .addCloseable(network)
                                .runAndThrow();
                    }
                })
                .build();
    }

    private static JmxExporterTestEnvironment getEnvironment(Context context) {
        return context.getParent()
                .getStore()
                .get(ENVIRONMENT_KEY, JmxExporterTestEnvironment.class)
                .orElseThrow();
    }

    private static void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        Set<String> excludeAttributeNameSet = new HashSet<>();
        excludeAttributeNameSet.add("_ClassPath");
        excludeAttributeNameSet.add("_SystemProperties");

        Set<String> excludeJavaLangMemoryAttributeSet = new HashSet<>();
        excludeJavaLangMemoryAttributeSet.add("NonHeapMemoryUsage");
        excludeJavaLangMemoryAttributeSet.add("Verbose");
        excludeJavaLangMemoryAttributeSet.add("ObjectPendingFinalizationCount");

        metrics.forEach(metric -> {
            String name = metric.name();
            if (name.equals("java_lang_Memory")) {
                for (String attributeName : excludeJavaLangMemoryAttributeSet) {
                    if (name.equals(attributeName)) {
                        fail("metric [" + metric + "] found");
                    }
                }
            } else {
                for (String attributeName : excludeAttributeNameSet) {
                    if (name.contains(attributeName)) {
                        fail("metric [" + metric + "] found");
                    }
                }
            }
        });
    }
}
