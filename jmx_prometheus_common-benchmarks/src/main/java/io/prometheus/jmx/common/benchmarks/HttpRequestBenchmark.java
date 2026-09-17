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

package io.prometheus.jmx.common.benchmarks;

import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.PrintWriter;
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
 * End-to-end benchmark of a real {@link HTTPServerFactory} server on a loopback ephemeral port.
 *
 * <p>The request path covers security headers, authentication, optional request-deadline scheduling,
 * and the metrics handler. The benchmark reuses a single keep-alive {@link HttpClient} connection so
 * its own allocation is constant across baseline and candidate; it is intended for differential
 * measurement, not absolute client+server latency.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class HttpRequestBenchmark {

    private static final String USERNAME = "Prometheus";
    private static final String PASSWORD = "secret";

    /**
     * Authentication mode.
     */
    @Param({"noAuth", "auth"})
    public String authentication;

    /**
     * Requested endpoint.
     */
    @Param({"metrics", "healthy"})
    public String endpoint;

    /**
     * Configured {@code maximumRequestSeconds}; {@code 0} disables the request deadline.
     */
    @Param({"0", "30"})
    public int maximumRequestSeconds;

    private HTTPServer server;
    private File configFile;
    private HttpClient client;
    private HttpRequest request;
    private HttpResponse.BodyHandler<String> bodyHandler;

    /**
     * Starts the server, registers metrics, and warms the connection.
     *
     * @throws Exception if the server cannot be started
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        PrometheusRegistry registry = new PrometheusRegistry();
        Counter counter = Counter.builder()
                .name("benchmark_requests_total")
                .help("Benchmark request counter")
                .labelNames("path")
                .register(registry);
        counter.labelValues("metrics").inc(42);
        counter.labelValues("healthy").inc(1);

        configFile = writeConfig();
        server = HTTPServerFactory.createAndStartHTTPServer(registry, InetAddress.getLoopbackAddress(), 0, configFile);

        String path = "metrics".equals(endpoint) ? "/metrics" : "/-/healthy";
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + server.getPort() + path))
                .GET();
        if ("auth".equals(authentication)) {
            String token =
                    Base64.getEncoder().encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
            requestBuilder.header("Authorization", "Basic " + token);
        }
        request = requestBuilder.build();
        bodyHandler = HttpResponse.BodyHandlers.ofString();

        client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        sendRequest(); // warm the connection and any lazy server state
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

    private File writeConfig() throws Exception {
        File config = File.createTempFile("common-benchmark-", ".yaml");
        config.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(config, StandardCharsets.UTF_8)) {
            writer.println("httpServer:");
            if (maximumRequestSeconds > 0) {
                writer.println("  maximumRequestSeconds: " + maximumRequestSeconds);
            }
            if ("auth".equals(authentication)) {
                writer.println("  authentication:");
                writer.println("    basic:");
                writer.println("      username: " + USERNAME);
                writer.println("      password: " + PASSWORD);
            }
        }
        return config;
    }
}
