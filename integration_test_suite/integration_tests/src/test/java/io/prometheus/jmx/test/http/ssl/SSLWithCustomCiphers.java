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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.paramixel.api.Context.withInstance;
import static org.paramixel.api.action.Instance.instance;
import static org.paramixel.api.action.Scope.scope;
import static org.paramixel.api.action.Sequential.sequential;
import static org.paramixel.api.action.Step.step;

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
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import nl.altindag.ssl.SSLFactory;
import org.altcontainers.api.Network;
import org.assertj.core.util.Strings;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class SSLWithCustomCiphers {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    private static final String[] DEFAULT_CIPHERS = {
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256",
        "TLS_CHACHA20_POLY1305_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
        "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
        "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
        "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA"
    };

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode, String[] ciphers) throws Exception {
        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/mode/%s/keystore.pkcs12", SSLWithCustomCiphers.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = SSLWithCustomCiphers.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }

        String alias = keyStore.aliases().nextElement();
        Certificate cert = keyStore.getCertificate(alias);

        return SSLFactory.builder()
                .withIdentityMaterial(keyStore, password)
                .withTrustMaterial(cert)
                .withCiphers(ciphers)
                .build()
                .getSslContext();
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(SSLWithCustomCiphers.class).stream()
                .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                .collect(Collectors.toList());

        return Each.parallel(
                        SSLWithCustomCiphers.class.getName(),
                        environments,
                        environment -> instance(environment.name(), () -> new SSLWithCustomCiphers(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(SSLWithCustomCiphers.class, SSLWithCustomCiphers::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers::testHealthy)))
                                                .child(step(
                                                        "testCallingServerWithNonMatchingSslCiphers()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers
                                                                        ::testCallingServerWithNonMatchingSslCiphers)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphers.class,
                                                                SSLWithCustomCiphers::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        SSLWithCustomCiphers.class, SSLWithCustomCiphers::tearDown)))))
                .build();
    }

    private SSLWithCustomCiphers(JmxExporterTestEnvironment environment) {
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

        HttpResponse httpResponse = HttpClient.sendRequest(
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_CIPHERS));
        assertHealthyResponse(httpResponse);
    }

    public void testCallingServerWithNonMatchingSslCiphers() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        String[] nonMatchingCiphers = {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"};
        assertThatThrownBy(() -> HttpClient.sendRequest(
                        url, initSSLContextForClientAuth(environment.getJmxExporterMode(), nonMatchingCiphers)))
                .isInstanceOf(SSLHandshakeException.class);
    }

    public void testDefaultTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        HttpResponse httpResponse = HttpClient.sendRequest(
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_CIPHERS));
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
            HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString());
        });

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
            HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_TEXT_METRICS.toString());
        });

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
            HttpClient.sendRequest(url, HttpHeader.ACCEPT, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString());
        });

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(SSLWithCustomCiphers.class, javaDockerImage, mode, metrics);
    }
}
