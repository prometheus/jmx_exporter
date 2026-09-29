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

package io.prometheus.jmx.javaagent.benchmarks;

import io.prometheus.jmx.BuildInfoMetrics;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * End-to-end benchmark of an agent-composed registry served by a real {@link HTTPServerFactory}
 * HTTP server on a loopback ephemeral port.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class HttpAgentScrapeBenchmark {

    private static final String USERNAME = "Prometheus";
    private static final String PASSWORD = "secret";

    /**
     * Requested endpoint.
     */
    @Param({"metrics", "healthy"})
    public String endpoint;

    /**
     * Whether basic authentication is configured and used.
     */
    @Param({"noAuth", "auth"})
    public String authentication;

    private HTTPServer server;
    private File configFile;
    private HttpClient client;
    private HttpRequest request;
    private HttpResponse.BodyHandler<String> bodyHandler;

    /**
     * Builds the agent-configured registry and starts the HTTP server.
     *
     * @throws Exception if setup fails
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        PrometheusRegistry registry = new PrometheusRegistry();
        new BuildInfoMetrics().register(registry);
        JvmMetrics.builder().register(registry);

        String[] config;
        if ("auth".equals(authentication)) {
            config = new String[] {
                "httpServer:",
                "  authentication:",
                "    basic:",
                "      username: " + USERNAME,
                "      password: " + PASSWORD
            };
        } else {
            config = new String[] {"httpServer:"};
        }
        configFile = AgentBenchmarkSupport.writeConfig(config);

        server = HTTPServerFactory.createAndStartHTTPServer(registry, InetAddress.getLoopbackAddress(), 0, configFile);

        String path = "metrics".equals(endpoint) ? "/metrics" : "/-/healthy";
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + path))
                .GET();
        if ("auth".equals(authentication)) {
            String token =
                    Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            builder.header("Authorization", "Basic " + token);
        }
        request = builder.build();
        bodyHandler = HttpResponse.BodyHandlers.ofString();

        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        sendRequest();
    }

    /**
     * Stops the server and removes the temporary configuration file.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
        if (configFile != null) {
            configFile.delete();
        }
    }

    /**
     * Benchmarks one HTTP request/response round trip.
     *
     * @param blackhole JMH blackhole
     * @throws Exception if the request fails
     */
    @Benchmark
    public void request(Blackhole blackhole) throws Exception {
        HttpResponse<String> response = sendRequest();
        blackhole.consume(response.statusCode());
        blackhole.consume(response.body());
    }

    private HttpResponse<String> sendRequest() throws Exception {
        return client.send(request, bodyHandler);
    }
}
