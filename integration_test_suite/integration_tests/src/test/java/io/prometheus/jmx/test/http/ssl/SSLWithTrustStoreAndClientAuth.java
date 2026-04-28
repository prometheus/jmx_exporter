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
import io.prometheus.jmx.test.support.util.TestSupport;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.assertj.core.util.Strings;
import org.paramixel.core.Action;
import org.paramixel.core.ConsoleRunner;
import org.paramixel.core.Paramixel;
import org.paramixel.core.action.Direct;
import org.paramixel.core.action.Lifecycle;
import org.paramixel.core.action.Parallel;
import org.paramixel.core.action.StrictSequential;
import org.paramixel.core.support.Cleanup;
import org.testcontainers.containers.Network;

public class SSLWithTrustStoreAndClientAuth {

    private static final String BASE_URL = "https://localhost";

    private static class Attachment {
        public Network network;
        public JmxExporterTestEnvironment environment;

        public Attachment() {}
    }

    public static void main(String[] args) {
        ConsoleRunner.runAndExit(actionFactory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/%s/localhost.pkcs12", SSLWithTrustStoreAndClientAuth.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = SSLWithTrustStoreAndClientAuth.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }
        KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        km.init(keyStore, password);
        TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tm.init(keyStore);

        sslContext.init(km.getKeyManagers(), tm.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    @Paramixel.ActionFactory
    public static Action actionFactory() {
        return Parallel.of(
                SSLWithTrustStoreAndClientAuth.class.getName(),
                JmxExporterTestEnvironment.createEnvironments()
                        .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                        .map(e -> e.setBaseUrl(BASE_URL))
                        .map(SSLWithTrustStoreAndClientAuth::createLifecycleAction)
                        .toList());
    }

    private static Action createLifecycleAction(JmxExporterTestEnvironment environment) {
        Action testHealthy = Direct.of("testHealthy", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.HEALTHY);

            assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                HttpClient.sendRequest(url);
            });

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, initSSLContextForClientAuth(attachment.environment.getJmxExporterMode()));
            assertHealthyResponse(httpResponse);
        });

        Action testDefaultTextMetrics = Direct.of("testDefaultTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);

            assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                HttpClient.sendRequest(url);
            });

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url, initSSLContextForClientAuth(attachment.environment.getJmxExporterMode()));
            assertMetricsResponse(attachment.environment, httpResponse, MetricsContentType.DEFAULT);
        });

        Action testOpenMetricsTextMetrics = Direct.of("testOpenMetricsTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);

            assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
            });

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url,
                    HttpHeader.ACCEPT,
                    MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString(),
                    initSSLContextForClientAuth(attachment.environment.getJmxExporterMode()));

            assertMetricsResponse(attachment.environment, httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
        });

        Action testPrometheusTextMetrics = Direct.of("testPrometheusTextMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);

            assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
            });

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url,
                    HttpHeader.ACCEPT,
                    MetricsContentType.PROMETHEUS_TEXT_METRICS.toString(),
                    initSSLContextForClientAuth(attachment.environment.getJmxExporterMode()));

            assertMetricsResponse(attachment.environment, httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
        });

        Action testPrometheusProtobufMetrics = Direct.of("testPrometheusProtobufMetrics", context -> {
            var lifecycleContext = context.findContext(2).orElseThrow();
            Attachment attachment = lifecycleContext
                    .getAttachment()
                    .flatMap(a -> a.to(Attachment.class))
                    .orElseThrow();
            String url = attachment.environment.getUrl(JmxExporterPath.METRICS);

            assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
                HttpClient.sendRequest(
                        url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
            });

            HttpResponse httpResponse = HttpClient.sendRequest(
                    url,
                    HttpHeader.ACCEPT,
                    MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString(),
                    initSSLContextForClientAuth(attachment.environment.getJmxExporterMode()));

            assertMetricsResponse(attachment.environment, httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
        });

        Action tests = StrictSequential.of(
                "tests",
                List.of(
                        testHealthy,
                        testDefaultTextMetrics,
                        testOpenMetricsTextMetrics,
                        testPrometheusTextMetrics,
                        testPrometheusProtobufMetrics));

        return Lifecycle.of(
                environment.getName(),
                Direct.of("setUp", context -> {
                    Network network = Network.newNetwork();
                    network.getId();
                    environment.initialize(SSLWithTrustStoreAndClientAuth.class, network);
                    Attachment attachment = new Attachment();
                    attachment.network = network;
                    attachment.environment = environment;
                    context.setAttachment(attachment);
                }),
                tests,
                Direct.of("tearDown", context -> {
                    Attachment attachment = context.removeAttachment()
                            .flatMap(a -> a.to(Attachment.class))
                            .orElse(null);

                    if (attachment != null) {
                        Cleanup.of(Cleanup.Mode.FORWARD)
                                .addCloseable(attachment.environment)
                                .addCloseable(attachment.network)
                                .runAndThrow();
                    }
                }));
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

        String buildInfoName = TestSupport.getBuildInfoName(jmxExporterTestEnvironment.getJmxExporterMode());

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
