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

package io.prometheus.jmx.benchmarks;

import io.prometheus.jmx.JmxCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
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
 * End-to-end benchmark of a single {@link JmxCollector#collect()} scraping a set of dynamically
 * generated MBeans from the platform MBean server.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class JmxCollectorBenchmark {

    private static final int ATTRIBUTE_COUNT = 10;

    /**
     * Number of MBeans to register and scrape.
     */
    @Param({"10", "100", "1000"})
    public int beanCount;

    private List<ObjectName> registeredBeans;

    private JmxCollector defaultExportCollector;
    private JmxCollector regexNoCacheCollector;
    private JmxCollector regexCacheCollector;

    /**
     * Registers the benchmark MBeans and builds the collectors under test.
     *
     * @throws Exception if the beans or collectors cannot be created
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        registeredBeans = BenchmarkMBeans.register(beanCount, ATTRIBUTE_COUNT);

        defaultExportCollector = createCollector(BenchmarkMBeans.INCLUDE_OBJECT_NAMES);
        regexNoCacheCollector = createCollector(BenchmarkMBeans.INCLUDE_OBJECT_NAMES + "rules:\n- pattern: \".*\"\n");
        regexCacheCollector =
                createCollector(BenchmarkMBeans.INCLUDE_OBJECT_NAMES + "rules:\n- pattern: \".*\"\n  cache: true\n");
    }

    /**
     * Unregisters the benchmark MBeans.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        BenchmarkMBeans.unregister(registeredBeans);
    }

    /**
     * Benchmarks the default export code path (no configured rules).
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void defaultExport(Blackhole blackhole) {
        blackhole.consume(defaultExportCollector.collect());
    }

    /**
     * Benchmarks a matching rule that is evaluated on every scrape.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void regexMatchNoCache(Blackhole blackhole) {
        blackhole.consume(regexNoCacheCollector.collect());
    }

    /**
     * Benchmarks a matching rule whose result is cached after the first scrape.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void regexMatchWithCache(Blackhole blackhole) {
        blackhole.consume(regexCacheCollector.collect());
    }

    private static JmxCollector createCollector(String yamlConfig) throws Exception {
        JmxCollector collector = new JmxCollector(yamlConfig);
        collector.register(new PrometheusRegistry());
        return collector;
    }
}
