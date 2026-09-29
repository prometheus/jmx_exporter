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

import io.prometheus.jmx.MatchedRule;
import io.prometheus.jmx.MatchedRulesCache;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * Benchmarks the {@link MatchedRulesCache} lookup and staleness-eviction code paths.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MatchedRulesCacheBenchmark {

    /**
     * Number of cached entries.
     */
    @Param({"10000"})
    public int entryCount;

    private MatchedRulesCache cache;
    private MatchedRulesCache.CacheKey[] keys;
    private MatchedRule matchedRule;
    private MatchedRulesCache.StalenessTracker freshTracker;

    /**
     * Populates the cache and staleness tracker.
     */
    @Setup(Level.Trial)
    public void setUp() {
        matchedRule = new MatchedRule(
                "benchmark_metric",
                "io.prometheus.jmx.benchmark<type=Benchmark, name=bean0><>attribute_0: 1.0",
                "GAUGE",
                "benchmark metric",
                Collections.singletonList("instance"),
                Collections.singletonList("shared"),
                1.0,
                1.0);

        cache = new MatchedRulesCache();
        freshTracker = new MatchedRulesCache.StalenessTracker();
        keys = new MatchedRulesCache.CacheKey[entryCount];

        for (int i = 0; i < entryCount; i++) {
            LinkedHashMap<String, String> beanProperties = new LinkedHashMap<>();
            beanProperties.put("type", "Benchmark");
            beanProperties.put("name", "bean" + i);
            keys[i] = new MatchedRulesCache.CacheKey(
                    "io.prometheus.jmx.benchmark", beanProperties, Collections.singletonList(""), "attribute_0");
            cache.put(keys[i], matchedRule);
            freshTracker.markAsFresh(keys[i]);
        }
    }

    /**
     * Benchmarks a cache hit.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void get(Blackhole blackhole) {
        blackhole.consume(cache.get(keys[0]));
    }

    /**
     * Benchmarks overwriting an existing cache entry.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void put(Blackhole blackhole) {
        cache.put(keys[0], matchedRule);
        blackhole.consume(keys[0]);
    }

    /**
     * Benchmarks the staleness scan when every entry is fresh (no removals).
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void evictStaleEntries(Blackhole blackhole) {
        cache.evictStaleEntries(freshTracker);
        blackhole.consume(cache);
    }
}
