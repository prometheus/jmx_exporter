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
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
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
 * Benchmarks an agent-configured {@link PrometheusRegistry#scrape()} containing build info, JVM
 * metrics, and a JMX collector.
 *
 * <p>This is an integration measurement; changes that affect registry aggregation should be routed
 * to the module that owns them.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class AgentRegistryScrapeBenchmark {

    private static final int ATTRIBUTE_COUNT = 10;

    /**
     * Number of registered MBeans.
     */
    @Param({"10", "100", "1000"})
    public int beanCount;

    /**
     * Export mode for the JMX collector.
     */
    @Param({"default", "regex", "cache"})
    public String exportMode;

    private List<ObjectName> registeredBeans;
    private PrometheusRegistry registry;

    /**
     * Registers the MBeans and builds the agent-configured registry.
     *
     * @throws Exception if setup fails
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        registeredBeans = AgentBenchmarkMBeans.register(beanCount, ATTRIBUTE_COUNT);

        registry = new PrometheusRegistry();
        new BuildInfoMetrics().register(registry);
        JvmMetrics.builder().register(registry);

        String[] config;
        switch (exportMode) {
            case "default":
                config = new String[] {AgentBenchmarkMBeans.INCLUDE_OBJECT_NAMES.trim()};
                break;
            case "cache":
                config = AgentBenchmarkSupport.collectorConfig(true);
                break;
            case "regex":
            default:
                config = AgentBenchmarkSupport.collectorConfig(false);
                break;
        }
        File configFile = AgentBenchmarkSupport.writeConfig(config);
        new JmxCollector(configFile, JmxCollector.Mode.AGENT).register(registry);

        registry.scrape();
    }

    /**
     * Unregisters the benchmark MBeans.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        AgentBenchmarkMBeans.unregister(registeredBeans);
    }

    /**
     * Benchmarks a full registry scrape.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void scrape(Blackhole blackhole) {
        blackhole.consume(registry.scrape());
    }
}
