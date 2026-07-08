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
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import nl.altindag.ssl.SSLFactory;
import org.altcontainers.api.Network;
import org.assertj.core.util.Strings;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class SSLWithCustomProtocols {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    private static final String DEFAULT_TLS_PROTOCOL = "TLSv1.2";

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode, String protocol) throws Exception {
        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/mode/%s/keystore.pkcs12", SSLWithCustomProtocols.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream = SSLWithCustomProtocols.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }

        String alias = keyStore.aliases().nextElement();
        Certificate cert = keyStore.getCertificate(alias);

        return SSLFactory.builder()
                .withIdentityMaterial(keyStore, password)
                .withTrustMaterial(cert)
                .withProtocols(protocol)
                .build()
                .getSslContext();
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments = JmxExporterTestEnvironment.createTestEnvironments(SSLWithCustomProtocols.class).stream()
                .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                .collect(Collectors.toList());

        return Each.parallel(
                        SSLWithCustomProtocols.class.getName(),
                        environments,
                        environment -> instance(environment.name(), () -> new SSLWithCustomProtocols(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        SSLWithCustomProtocols.class, SSLWithCustomProtocols::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                SSLWithCustomProtocols.class,
                                                                SSLWithCustomProtocols::testHealthy)))
                                                .child(
                                                        step(
                                                                "testCallingServerWithNonMatchingSslProtocols()",
                                                                withInstance(
                                                                        SSLWithCustomProtocols.class,
                                                                        SSLWithCustomProtocols
                                                                                ::testCallingServerWithNonMatchingSslProtocols)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomProtocols.class,
                                                                SSLWithCustomProtocols::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomProtocols.class,
                                                                SSLWithCustomProtocols::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomProtocols.class,
                                                                SSLWithCustomProtocols::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                SSLWithCustomProtocols.class,
                                                                SSLWithCustomProtocols
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        SSLWithCustomProtocols.class,
                                                        SSLWithCustomProtocols::tearDown)))))
                .build();
    }

    private SSLWithCustomProtocols(JmxExporterTestEnvironment environment) {
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
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));
        assertHealthyResponse(httpResponse);
    }

    public void testCallingServerWithNonMatchingSslProtocols() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        SSLContext sslContext = SSLContext.getDefault();
        Optional<String> anyOtherProtocol = Stream.of(
                        sslContext.getDefaultSSLParameters().getProtocols())
                .filter(protocol -> !protocol.equals("TLSv1.2"))
                .findAny();

        assertThat(anyOtherProtocol).isPresent();
        assertThatThrownBy(() -> HttpClient.sendRequest(
                        url, initSSLContextForClientAuth(environment.getJmxExporterMode(), anyOtherProtocol.get())))
                .isInstanceOf(SSLHandshakeException.class);
    }

    public void testDefaultTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatExceptionOfType(IOException.class).isThrownBy(() -> HttpClient.sendRequest(url));

        HttpResponse httpResponse = HttpClient.sendRequest(
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));
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
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

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
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

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
                initSSLContextForClientAuth(environment.getJmxExporterMode(), DEFAULT_TLS_PROTOCOL));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(SSLWithCustomProtocols.class, javaDockerImage, mode, metrics);
    }
}
