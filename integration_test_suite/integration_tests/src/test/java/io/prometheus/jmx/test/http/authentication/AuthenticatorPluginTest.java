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

package io.prometheus.jmx.test.http.authentication;

import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

public class AuthenticatorPluginTest {

    private static final int ENVIRONMENT_LEVEL = 2;

    private static final String ENVIRONMENT_KEY = "environment";

    private static final String NETWORK_KEY = "network";

    private static final String VALID_USERNAME = "Prometheus";

    private static final String VALID_PASSWORD = "secret";

    private static final String[] TEST_USERNAMES = new String[] {VALID_USERNAME, "prometheus", "bad", "", null};

    private static final String[] TEST_PASSWORDS = new String[] {VALID_PASSWORD, "Secret", "bad", "", null};

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                AuthenticatorPluginTest.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .filter(env -> env.getJmxExporterMode() == JmxExporterMode.JavaAgent)
                        .map(AuthenticatorPluginTest::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment environment) {
        Action testHealthy = Direct.of("testHealthy", AuthenticatorPluginTest::testHealthy);

        Action testDefaultTextMetrics =
                Direct.of("testDefaultTextMetrics", AuthenticatorPluginTest::testDefaultTextMetrics);

        Action testOpenMetricsTextMetrics =
                Direct.of("testOpenMetricsTextMetrics", AuthenticatorPluginTest::testOpenMetricsTextMetrics);

        Action testPrometheusTextMetrics =
                Direct.of("testPrometheusTextMetrics", AuthenticatorPluginTest::testPrometheusTextMetrics);

        Action testPrometheusProtobufMetrics =
                Direct.of("testPrometheusProtobufMetrics", AuthenticatorPluginTest::testPrometheusProtobufMetrics);

        Action tests = DependentSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                environment.getName(),
                Direct.of("setUp", context -> setUp(context, environment)),
                tests,
                Direct.of("tearDown", AuthenticatorPluginTest::tearDown));
    }

    private static void setUp(Context context, JmxExporterTestEnvironment environment) throws Throwable {
        Network network = Network.newNetwork();
        network.getId();
        environment.initialize(AuthenticatorPluginTest.class, network);
        context.getStore().put(NETWORK_KEY, Value.of(network));
        context.getStore().put(ENVIRONMENT_KEY, Value.of(environment));
    }

    private static void testHealthy(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.HEALTHY);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                assertThat(httpResponse.statusCode()).isEqualTo(expectedStatusCode);
            }
        }
    }

    private static void testDefaultTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.DEFAULT);
                }
            }
        }
    }

    private static void testOpenMetricsTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                }
            }
        }
    }

    private static void testPrometheusTextMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                }
            }
        }
    }

    private static void testPrometheusProtobufMetrics(Context context) throws Throwable {
        JmxExporterTestEnvironment environment = getEnvironment(context);
        String url = environment.getUrl(JmxExporterPath.METRICS);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                int expectedStatusCode = 401;

                if (VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password)) {
                    expectedStatusCode = 200;
                }

                HttpRequest httpRequest = HttpRequest.builder()
                        .url(url)
                        .basicAuthentication(username, password)
                        .header(HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString())
                        .build();

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequest);

                if (expectedStatusCode == 401) {
                    assertThat(httpResponse.statusCode()).isEqualTo(401);
                } else {
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
                }
            }
        }
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

    private static void assertMetricsResponse(
            JmxExporterTestEnvironment jmxExporterTestEnvironment,
            HttpResponse httpResponse,
            MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = new LinkedHashMap<>();

        Set<String> compositeNameSet = new HashSet<>();
        MetricsParser.parseCollection(httpResponse).forEach(metric -> {
            String name = metric.name();
            Map<String, String> labels = metric.labels();
            String compositeName = name + " " + labels;
            assertThat(compositeNameSet).doesNotContain(compositeName);
            compositeNameSet.add(compositeName);
            metrics.computeIfAbsent(name, k -> new ArrayList<>()).add(metric);
        });

        boolean isJmxExporterModeJavaAgent =
                jmxExporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName = jmxExporterTestEnvironment.getJmxExporterMode().getBuildInfoName();

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
    }
}
