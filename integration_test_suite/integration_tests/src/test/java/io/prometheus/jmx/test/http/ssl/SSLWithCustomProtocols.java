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

package io.prometheus.jmx.test.http.ssl;

import static io.prometheus.jmx.test.support.http.HttpResponse.assertHealthyResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetricsContentType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.filter.PKCS12KeyStoreExporterTestEnvironmentFilter;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import nl.altindag.ssl.SSLFactory;
import org.assertj.core.util.Strings;
import org.paramixel.core.Action;
import org.paramixel.core.Context;
import org.paramixel.core.Factory;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Container;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class SSLWithCustomProtocols {

    private static final String ENVIRONMENT_KEY = "environment";
    private static final String NETWORK_KEY = "network";

    private static final String BASE_URL = "https://localhost";
    private static final String DEFAULT_TLS_PROTOCOL = "TLSv1.2";

    public static void main(String[] args) {
        Factory.defaultRunner().runAndExit(actionFactory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode, String protocol) throws Exception {
        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/%s/keystore.pkcs12", SSLWithCustomProtocols.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = SSLWithCustomProtocols.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }

        return SSLFactory.builder()
                .withIdentityMaterial(keyStore, password)
                .withTrustMaterial(keyStore)
                .withProtocols(protocol)
                .build()
                .getSslContext();
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        var parallelBuilder = Parallel.builder(SSLWithCustomProtocols.class.getName());
        for (JmxExporterTestEnvironment environment : JmxExporterTestEnvironment.createEnvironments()
                .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                .map(e -> e.setBaseUrl(BASE_URL))
                .toList()) {
            parallelBuilder.child(argument(environment));
        }
        return parallelBuilder.build();
    }

    private static Action argument(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        Action setUp = setUp(jmxExporterTestEnvironment);
        Action testHealthy = testHealthy();
        Action testCallingServerWithNonMatchingSslProtocols = testCallingServerWithNonMatchingSslProtocols();
        Action testDefaultTextMetrics = testDefaultTextMetrics();
        Action testOpenMetricsTextMetrics = testOpenMetricsTextMetrics();
        Action testPrometheusTextMetrics = testPrometheusTextMetrics();
        Action testPrometheusProtobufMetrics = testPrometheusProtobufMetrics();
        Action tearDown = tearDown();

        return Container.builder(jmxExporterTestEnvironment.getName())
                .before(setUp)
                .child(testHealthy)
                .child(testCallingServerWithNonMatchingSslProtocols)
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
                    jmxExporterTestEnvironment.initialize(SSLWithCustomProtocols.class, network);
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

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));
                    assertHealthyResponse(httpResponse);
                })
                .build();
    }

    private static Action testCallingServerWithNonMatchingSslProtocols() {
        return Direct.builder("testCallingServerWithNonMatchingSslProtocols")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

                    SSLContext sslContext = SSLContext.getDefault();
                    Optional<String> anyOtherProtocol = Stream.of(
                                    sslContext.getDefaultSSLParameters().getProtocols())
                            .filter(protocol -> !protocol.equals("TLSv1.2"))
                            .findAny();

                    assertThat(anyOtherProtocol).isPresent();
                    assertThatThrownBy(() -> HttpClient.sendRequest(
                                    url,
                                    initSSLContextForClientAuth(
                                            environment.getJmxExporterMode(), anyOtherProtocol.get())))
                            .isInstanceOf(SSLHandshakeException.class);
                })
                .build();
    }

    private static Action testDefaultTextMetrics() {
        return Direct.builder("testDefaultTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));
                    assertMetricsResponse(environment, httpResponse, MetricsContentType.DEFAULT);
                })
                .build();
    }

    private static Action testOpenMetricsTextMetrics() {
        return Direct.builder("testOpenMetricsTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                        HttpClient.sendRequest(
                                url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
                    });

                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url,
                            HttpHeader.ACCEPT,
                            MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString(),
                            initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

                    assertMetricsResponse(environment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusTextMetrics() {
        return Direct.builder("testPrometheusTextMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                        HttpClient.sendRequest(
                                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
                    });

                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url,
                            HttpHeader.ACCEPT,
                            MetricsContentType.PROMETHEUS_TEXT_METRICS.toString(),
                            initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
                })
                .build();
    }

    private static Action testPrometheusProtobufMetrics() {
        return Direct.builder("testPrometheusProtobufMetrics")
                .runnable(context -> {
                    JmxExporterTestEnvironment environment = getEnvironment(context);
                    String url = environment.getUrl(JmxExporterPath.METRICS);

                    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                        HttpClient.sendRequest(
                                url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
                    });

                    HttpResponse httpResponse = HttpClient.sendRequest(
                            url,
                            HttpHeader.ACCEPT,
                            MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString(),
                            initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

                    assertMetricsResponse(environment, httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
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

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions")
                .withValue(2.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps")
                .withValue(4.0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred")
                .withValue(6.0d)
                .isPresent();
    }
}
