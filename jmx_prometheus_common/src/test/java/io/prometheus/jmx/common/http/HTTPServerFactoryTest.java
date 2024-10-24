package io.prometheus.jmx.common.http;

import static org.junit.Assert.assertTrue;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class HTTPServerFactoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = TemporaryFolder.builder().assureDeletion().build();

    HTTPServer httpServer;

    @After
    public void stopServer() {
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    @Test
    public void createHTTPServerWithCustomAuthenticatorClass451RoundTrip() throws Exception {

        File config = temporaryFolder.newFile("ok");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println(
                "      authenticatorClass:"
                        + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticator451");
        writer.close();

        httpServer = startServer(config);

        verifyExpectedResponse(httpServer, "HTTP/1.1 451");
    }

    @Test
    public void createHTTPServerWithCustomAuthenticatorClassSubjectOkRoundTrip() throws Exception {

        File config = temporaryFolder.newFile("ok");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println(
                "      authenticatorClass:"
                    + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticatorWithSubject");
        writer.println("      subjectAttributeName: custom.subject");

        writer.close();

        httpServer = startServer(config);

        verifyExpectedResponse(httpServer, "HTTP/1.1 200 OK");
    }

    @Test
    public void createHTTPServerWithCustomAuthenticatorClassSubjectNotMatchingRoundTrip()
            throws Exception {

        File config = temporaryFolder.newFile("unmatched_subjectAttributeName");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println(
                "      authenticatorClass:"
                    + " io.prometheus.jmx.common.http.authenticator.CustomAuthenticatorWithSubject");
        writer.println("      subjectAttributeName: not.the.correct.custom.subject.attribute");

        writer.close();

        httpServer = startServer(config);

        verifyExpectedResponse(httpServer, "HTTP/1.1 403");
    }

    private void verifyExpectedResponse(HTTPServer httpServer, String expectedResponseSubString)
            throws Exception {
        Socket socket = new Socket();
        try {
            socket.setSoTimeout(1000);
            socket.connect(new InetSocketAddress("localhost", httpServer.getPort()));
            socket.getOutputStream()
                    .write("GET /metrics HTTP/1.1 \r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream()
                    .write("HOST: localhost \r\n\r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            String actualResponse = "";
            byte[] resp = new byte[500];
            int read = socket.getInputStream().read(resp, 0, resp.length);
            if (read > 0) {
                actualResponse = new String(resp, 0, read);
            }
            assertTrue(actualResponse.contains(expectedResponseSubString));
        } finally {
            socket.close();
        }
    }

    @Test(expected = ConfigurationException.class)
    public void createHTTPServerWithCustomAuthenticatorClassNOkNoConstructor() throws Exception {

        File config = temporaryFolder.newFile("error_no_constructor");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println(
                "      authenticatorClass:"
                        + " io.prometheus.jmx.common.http.authenticator.PlaintextAuthenticator");
        writer.close();

        httpServer = startServer(config);
    }

    @Test(expected = ConfigurationException.class)
    public void createHTTPServerWithCustomAuthenticatorClassNokNotFound() throws Exception {

        File config = temporaryFolder.newFile("notFound");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println(
                "      authenticatorClass:"
                        + " myio.jmx.common.notThere.authenticator.PlaintextAuthenticator");
        writer.close();

        httpServer = startServer(config);
    }

    @Test(expected = ConfigurationException.class)
    public void createHTTPServerWithCustomAuthenticatorClassNokNotString() throws Exception {

        File config = temporaryFolder.newFile("as_int");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println("       authenticatorClass: 10");
        writer.close();

        httpServer = startServer(config);
    }

    @Test(expected = ConfigurationException.class)
    public void createHTTPServerWithCustomAuthenticatorClassNokMissingString() throws Exception {

        File config = temporaryFolder.newFile("missing");
        PrintWriter writer = new PrintWriter(config);
        writer.println("httpServer:");
        writer.println("  authentication:");
        writer.println("    customAuthenticator:");
        writer.println("      authenticatorClass:");
        writer.close();

        httpServer = startServer(config);
    }

    private HTTPServer startServer(File config) throws IOException {
        return new HTTPServerFactory()
                .createHTTPServer(
                        InetAddress.getByName("0.0.0.0"),
                        0,
                        PrometheusRegistry.defaultRegistry,
                        config);
    }
}
