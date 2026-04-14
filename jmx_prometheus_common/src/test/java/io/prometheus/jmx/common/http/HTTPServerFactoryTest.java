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

package io.prometheus.jmx.common.http;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.prometheus.jmx.common.ConfigurationException;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class HTTPServerFactoryTest {

    @TempDir
    File temporaryFolder;

    HTTPServer httpServer;

    @AfterEach
    public void stopServer() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClass451RoundTrip(String path) throws Exception {
        File config = new File(temporaryFolder, "ok");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:" + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticator451");
        writer.close();

        httpServer = startServer(config, path);

        verifyExpectedResponse(httpServer, "HTTP/1.1 451");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassSubjectOkRoundTrip(String path) throws Exception {
        File config = new File(temporaryFolder, "ok");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:" + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticatorWithSubject");
        writer.println("      subjectAttributeName: custom.subject");

        writer.close();

        httpServer = startServer(config, path);

        verifyExpectedResponse(httpServer, "HTTP/1.1 200 OK");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassSubjectNotMatchingRoundTrip(String path)
            throws Exception {
        File config = new File(temporaryFolder, "unmatched_subjectAttributeName");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:" + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticatorWithSubject");
        writer.println("      subjectAttributeName: not.the.correct.custom.subject.attribute");

        writer.close();

        httpServer = startServer(config, path);

        verifyExpectedResponse(httpServer, "HTTP/1.1 403");
    }

    private void verifyExpectedResponse(HTTPServer httpServer, String expectedResponseSubString) throws Exception {
        try (Socket socket = new Socket()) {
            socket.setSoTimeout(1000);
            socket.connect(new InetSocketAddress("localhost", httpServer.getPort()));
            socket.getOutputStream().write("GET /metrics HTTP/1.1 \r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().write("HOST: localhost \r\n\r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            String actualResponse = "";
            byte[] resp = new byte[500];
            int read = socket.getInputStream().read(resp, 0, resp.length);
            if (read > 0) {
                actualResponse = new String(resp, 0, read);
            }
            assertThat(actualResponse).contains(expectedResponseSubString);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassNOkNoConstructor(String path) throws Exception {
        File config = new File(temporaryFolder, "error_no_constructor");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:" + " io.prometheus.jmx.common.authenticator.PlaintextAuthenticator");
        writer.close();

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> httpServer = startServer(config, path));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassNokNotFound(String path) throws Exception {
        File config = new File(temporaryFolder, "notFound");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:" + " myio.jmx.common.notThere.authenticator.PlaintextAuthenticator");
        writer.close();

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> httpServer = startServer(config, path));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassNokNotString(String path) throws Exception {
        File config = new File(temporaryFolder, "as_int");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("       class: 10");
        writer.close();

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> httpServer = startServer(config, path));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"/custom_metrics"})
    public void createAndStartHTTPServerWithCustomAuthenticatorClassNokMissingString(String path) throws Exception {
        File config = new File(temporaryFolder, "missing");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    plugin:");
        writer.println("      class:");
        writer.close();

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> httpServer = startServer(config, path));
    }

    private HTTPServer startServer(File config, String path) throws IOException {
        return HTTPServerFactory.createAndStartHTTPServer(
                PrometheusRegistry.defaultRegistry, InetAddress.getByName("0.0.0.0"), 0, path, config);
    }
}
