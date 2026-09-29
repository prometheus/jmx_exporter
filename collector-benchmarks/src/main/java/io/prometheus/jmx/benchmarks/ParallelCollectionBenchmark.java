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
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * Benchmarks parallel collection on a single {@link JmxCollector}.
 *
 * <p>The collector is created with a scrape thread pool sized to {@link #poolSize}. The {@code
 * parallelCollect} benchmark starts {@code poolSize} concurrent scrapes (one per pool thread) and
 * waits for all of them, while {@code sequentialCollect} performs the same number of scrapes one
 * after another. Comparing the two shows whether collection is actually running in parallel.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ParallelCollectionBenchmark {

    private static final int BEAN_COUNT = 100;
    private static final int ATTRIBUTE_COUNT = 10;

    /**
     * Scrape thread pool size, which is also the number of concurrent scrapes.
     */
    @Param({"1", "2", "4", "8"})
    public int poolSize;

    private JmxCollector collector;
    private List<ObjectName> registeredBeans;
    private ExecutorService callerExecutor;

    /**
     * Registers the benchmark MBeans and creates the collector under test.
     *
     * @throws Exception if the beans or collector cannot be created
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        registeredBeans = BenchmarkMBeans.register(BEAN_COUNT, ATTRIBUTE_COUNT);

        collector = new JmxCollector(BenchmarkMBeans.INCLUDE_OBJECT_NAMES, poolSize);
        collector.register(new PrometheusRegistry());

        callerExecutor = Executors.newFixedThreadPool(poolSize);
    }

    /**
     * Unregisters the benchmark MBeans and shuts down the caller executor.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        callerExecutor.shutdownNow();
        BenchmarkMBeans.unregister(registeredBeans);
    }

    /**
     * Runs {@code poolSize} scrapes concurrently.
     *
     * @param blackhole JMH blackhole
     * @throws Exception if a scrape fails
     */
    @Benchmark
    public void parallelCollect(Blackhole blackhole) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<MetricSnapshots>> futures = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            futures.add(callerExecutor.submit(() -> {
                start.await();
                return collector.collect();
            }));
        }
        start.countDown();
        for (Future<MetricSnapshots> future : futures) {
            blackhole.consume(future.get(60, TimeUnit.SECONDS));
        }
    }

    /**
     * Runs {@code poolSize} scrapes one after another.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void sequentialCollect(Blackhole blackhole) {
        for (int i = 0; i < poolSize; i++) {
            blackhole.consume(collector.collect());
        }
    }
}
