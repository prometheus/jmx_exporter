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
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

import io.prometheus.jmx.test.core.BasicTest;
import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterPath;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import io.prometheus.jmx.test.support.filter.PKCS12KeyStoreExporterTestEnvironmentFilter;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.altcontainers.api.Network;
import org.assertj.core.util.Strings;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class SSLWithTrustStoreAndClientAuth {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/mode/%s/keystore.pkcs12", SSLWithTrustStoreAndClientAuth.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = SSLWithTrustStoreAndClientAuth.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }
        String alias = keyStore.aliases().nextElement();
        Certificate cert = keyStore.getCertificate(alias);
        keyStore.setCertificateEntry(alias + "-trusted", cert);

        KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        km.init(keyStore, password);
        TrustManagerFactory tm = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tm.init(keyStore);

        sslContext.init(km.getKeyManagers(), tm.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        SSLWithTrustStoreAndClientAuth.class.getName(),
                        JmxExporterTestEnvironment.createTestEnvironments(SSLWithTrustStoreAndClientAuth.class).stream()
                                .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                                .toList(),
                        environment -> instance(
                                        environment.name(), () -> new SSLWithTrustStoreAndClientAuth(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        SSLWithTrustStoreAndClientAuth.class,
                                                        SSLWithTrustStoreAndClientAuth::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                SSLWithTrustStoreAndClientAuth.class,
                                                                SSLWithTrustStoreAndClientAuth::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                SSLWithTrustStoreAndClientAuth.class,
                                                                SSLWithTrustStoreAndClientAuth
                                                                        ::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                SSLWithTrustStoreAndClientAuth.class,
                                                                SSLWithTrustStoreAndClientAuth
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                SSLWithTrustStoreAndClientAuth.class,
                                                                SSLWithTrustStoreAndClientAuth
                                                                        ::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                SSLWithTrustStoreAndClientAuth.class,
                                                                SSLWithTrustStoreAndClientAuth
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        SSLWithTrustStoreAndClientAuth.class,
                                                        SSLWithTrustStoreAndClientAuth::tearDown)))))
                .build();
    }

    private SSLWithTrustStoreAndClientAuth(JmxExporterTestEnvironment environment) {
        this.environment = environment;
    }

    public void setUp() throws Throwable {
        environment.setBaseUrl("https://localhost");
        network = Network.create();
        environment.initialize(network);
    }

    public void tearDown() {
        try {
            environment.close();
        } finally {
            Network.close(network);
        }
    }

    public void testHealthy() throws Exception {
        String url = environment.getUrl(JmxExporterPath.HEALTHY);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        HttpResponse httpResponse =
                HttpClient.sendRequest(url, initSSLContextForClientAuth(environment.getJmxExporterMode()));
        assertHealthyResponse(httpResponse);
    }

    public void testDefaultTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        HttpResponse httpResponse =
                HttpClient.sendRequest(url, initSSLContextForClientAuth(environment.getJmxExporterMode()));
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> HttpClient.sendRequest(
                        url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString()));

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode()));

        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> HttpClient.sendRequest(
                        url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString()));

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode()));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> HttpClient.sendRequest(
                        url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString()));

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode()));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(BasicTest.class, javaDockerImage, mode, metrics);
    }
}
