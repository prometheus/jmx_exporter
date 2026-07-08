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

import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetrics;
import static io.prometheus.jmx.test.support.metrics.MetricsAssertions.assertMetricsContentType;
import static io.prometheus.jmx.test.support.metrics.MetricsParser.parseMap;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import nl.altindag.ssl.SSLFactory;
import org.altcontainers.api.Network;
import org.assertj.core.util.Strings;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;

public class SSLWithCustomCiphersAndProtocolsTest {

    private final JmxExporterTestEnvironment environment;

    private Network network;

    private static final String[] MATCHING_CIPHERS = {
        "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"
    };

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    private static SSLContext initSSLContextForClientAuth(JmxExporterMode mode, String[] ciphers) throws Exception {
        final String type = "PKCS12";
        final char[] password = "changeit".toCharArray();
        final String keyStoreResource = Strings.formatIfArgs(
                "%s/mode/%s/keystore.pkcs12",
                SSLWithCustomCiphersAndProtocolsTest.class.getSimpleName(), mode.toString());
        KeyStore keyStore = KeyStore.getInstance(type);
        try (InputStream inputStream =
                SSLWithCustomCiphersAndProtocolsTest.class.getResourceAsStream(keyStoreResource)) {
            keyStore.load(inputStream, password);
        }

        String alias = keyStore.aliases().nextElement();
        Certificate cert = keyStore.getCertificate(alias);

        return SSLFactory.builder()
                .withIdentityMaterial(keyStore, password)
                .withTrustMaterial(cert)
                .withCiphers(ciphers)
                .withProtocols("TLSv1.2")
                .build()
                .getSslContext();
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        var environments =
                JmxExporterTestEnvironment.createTestEnvironments(SSLWithCustomCiphersAndProtocolsTest.class).stream()
                        .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter())
                        .collect(java.util.stream.Collectors.toList());

        return Each.parallel(
                        SSLWithCustomCiphersAndProtocolsTest.class.getName(),
                        environments,
                        environment -> instance(
                                        environment.name(), () -> new SSLWithCustomCiphersAndProtocolsTest(environment))
                                .body(scope("scenario")
                                        .before(step(
                                                "setUp()",
                                                withInstance(
                                                        SSLWithCustomCiphersAndProtocolsTest.class,
                                                        SSLWithCustomCiphersAndProtocolsTest::setUp)))
                                        .body(sequential("tests")
                                                .child(step(
                                                        "testHealthy()",
                                                        withInstance(
                                                                SSLWithCustomCiphersAndProtocolsTest.class,
                                                                SSLWithCustomCiphersAndProtocolsTest::testHealthy)))
                                                .child(step(
                                                        "testDefaultTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphersAndProtocolsTest.class,
                                                                SSLWithCustomCiphersAndProtocolsTest
                                                                        ::testDefaultTextMetrics)))
                                                .child(step(
                                                        "testOpenMetricsTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphersAndProtocolsTest.class,
                                                                SSLWithCustomCiphersAndProtocolsTest
                                                                        ::testOpenMetricsTextMetrics)))
                                                .child(step(
                                                        "testPrometheusTextMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphersAndProtocolsTest.class,
                                                                SSLWithCustomCiphersAndProtocolsTest
                                                                        ::testPrometheusTextMetrics)))
                                                .child(step(
                                                        "testPrometheusProtobufMetrics()",
                                                        withInstance(
                                                                SSLWithCustomCiphersAndProtocolsTest.class,
                                                                SSLWithCustomCiphersAndProtocolsTest
                                                                        ::testPrometheusProtobufMetrics))))
                                        .after(step(
                                                "tearDown()",
                                                withInstance(
                                                        SSLWithCustomCiphersAndProtocolsTest.class,
                                                        SSLWithCustomCiphersAndProtocolsTest::tearDown)))))
                .build();
    }

    private SSLWithCustomCiphersAndProtocolsTest(JmxExporterTestEnvironment environment) {
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

        String[] nonMatchingCiphers = {"TLS_DHE_DSS_WITH_AES_128_CBC_SHA"};
        assertThatThrownBy(() -> HttpClient.sendRequest(
                        url, initSSLContextForClientAuth(environment.getJmxExporterMode(), nonMatchingCiphers)))
                .isInstanceOf(SSLHandshakeException.class);

        HttpResponse httpResponse = HttpClient.sendRequest(
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), MATCHING_CIPHERS));
        assertThat(httpResponse.statusCode()).isEqualTo(200);
    }

    public void testDefaultTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        assertThatThrownBy(() -> {
                    HttpClient.sendRequest(
                            url, initSSLContextForClientAuth(environment.getJmxExporterMode(), new String[] {
                                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA"
                            }));
                })
                .isInstanceOf(SSLHandshakeException.class);

        HttpResponse httpResponse = HttpClient.sendRequest(
                url, initSSLContextForClientAuth(environment.getJmxExporterMode(), MATCHING_CIPHERS));
        assertMetricsResponse(httpResponse, MetricsContentType.DEFAULT);
    }

    public void testOpenMetricsTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.OPEN_METRICS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), MATCHING_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.OPEN_METRICS_TEXT_METRICS);
    }

    public void testPrometheusTextMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_TEXT_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), MATCHING_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_TEXT_METRICS);
    }

    public void testPrometheusProtobufMetrics() throws Exception {
        String url = environment.getUrl(JmxExporterPath.METRICS);

        HttpResponse httpResponse = HttpClient.sendRequest(
                url,
                HttpHeader.ACCEPT,
                MetricsContentType.PROMETHEUS_PROTOBUF_METRICS.toString(),
                initSSLContextForClientAuth(environment.getJmxExporterMode(), MATCHING_CIPHERS));

        assertMetricsResponse(httpResponse, MetricsContentType.PROMETHEUS_PROTOBUF_METRICS);
    }

    private void assertMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertMetricsContentType(httpResponse, metricsContentType);

        Map<String, Collection<Metric>> metrics = parseMap(httpResponse);
        String mode = environment.getJmxExporterMode().name();
        String javaDockerImage = environment.getJavaDockerImage();

        assertMetrics(SSLWithCustomCiphersAndProtocolsTest.class, javaDockerImage, mode, metrics);
    }
}
