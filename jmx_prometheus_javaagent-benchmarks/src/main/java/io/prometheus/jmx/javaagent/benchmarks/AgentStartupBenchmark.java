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
import io.prometheus.jmx.JmxCollector;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
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
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks the individual javaagent bootstrap components against a representative configuration
 * file. The agent as a whole is measured by the process-startup mode, not in-process.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AgentStartupBenchmark {

    /**
     * Number of matching rules in the configuration.
     */
    @Param({"1", "100"})
    public int ruleCount;

    private File configFile;
    private MapAccessor sharedConfig;

    /**
     * Writes the representative configuration file and parses it once.
     *
     * @throws Exception if the file cannot be written
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add(AgentBenchmarkMBeans.INCLUDE_OBJECT_NAMES.trim());
        lines.add("rules:");
        for (int i = 0; i < ruleCount; i++) {
            lines.add("  - pattern: \".*\"");
        }
        configFile = AgentBenchmarkSupport.writeConfig(lines.toArray(new String[0]));
        sharedConfig = MapAccessor.of(YamlSupport.loadYaml(configFile));
    }

    /**
     * Benchmarks parsing the configuration file into a {@link MapAccessor}.
     *
     * @return the parsed configuration
     */
    @Benchmark
    public MapAccessor parseConfiguration() throws Exception {
        return MapAccessor.of(YamlSupport.loadYaml(configFile));
    }

    /**
     * Benchmarks the parenthesized agent behaviour of parsing the same file three times (the
     * bootstrap, HTTP server, and OpenTelemetry paths).
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void parseConfigurationThreeTimes(Blackhole blackhole) throws Exception {
        blackhole.consume(MapAccessor.of(YamlSupport.loadYaml(configFile)));
        blackhole.consume(MapAccessor.of(YamlSupport.loadYaml(configFile)));
        blackhole.consume(MapAccessor.of(YamlSupport.loadYaml(configFile)));
    }

    /**
     * Benchmarks constructing a {@link JmxCollector} from the configuration file.
     *
     * @return the collector
     * @throws Exception if construction fails
     */
    @Benchmark
    public JmxCollector constructJmxCollector() throws Exception {
        return new JmxCollector(configFile, JmxCollector.Mode.AGENT);
    }

    /**
     * Benchmarks registering build info metrics.
     */
    @Benchmark
    public void registerBuildInfoMetrics() {
        new BuildInfoMetrics().register(new PrometheusRegistry());
    }

    /**
     * Benchmarks registering the JVM metrics.
     */
    @Benchmark
    public void registerJvmMetrics() {
        JvmMetrics.builder().register(new PrometheusRegistry());
    }

    /**
     * Benchmarks creating and closing the HTTP server (as the agent does once at startup).
     *
     * @throws Exception if the server cannot be started
     */
    @Benchmark
    public void createAndCloseHttpServer() throws Exception {
        PrometheusRegistry registry = new PrometheusRegistry();
        HTTPServer httpServer =
                HTTPServerFactory.createAndStartHTTPServer(registry, InetAddress.getLoopbackAddress(), 0, configFile);
        httpServer.stop();
    }

    /**
     * Benchmarks creating and closing the HTTP server from an already-parsed configuration, as the
     * agent does after H1 (it shares the {@link MapAccessor} parsed during bootstrap instead of
     * re-parsing the file).
     *
     * @throws Exception if the server cannot be started
     */
    @Benchmark
    public void createAndCloseHttpServerFromSharedConfig() throws Exception {
        PrometheusRegistry registry = new PrometheusRegistry();
        HTTPServer httpServer =
                HTTPServerFactory.createAndStartHTTPServer(registry, InetAddress.getLoopbackAddress(), 0, sharedConfig);
        httpServer.stop();
    }
}
