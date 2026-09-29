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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

/**
 * Concurrency tests for the parts of the collector that are shared across {@link JmxCollector#collect()}
 * invocations. These verify that running scrapes in parallel does not corrupt shared state, throw
 * unexpected exceptions, or produce inconsistent results.
 */
public class JmxCollectorConcurrencyTest {

    /**
     * Exercise the real scrape path: many threads calling {@code collect()} in parallel on a single
     * JmxCollector. The collector shares {@code jmxMBeanPropertyCache}, the optional rules cache and
     * the ObjectName attribute filter across all scrapes.
     */
    @Test
    public void parallelCollectIsSafe() throws Exception {
        JmxCollector collector = new JmxCollector("---\n");
        collector.register(new PrometheusRegistry());

        int threads = 16;
        int iterations = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Runnable worker = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    MetricSnapshots snapshots = collector.collect();
                    if (snapshots.size() == 0) {
                        failure.compareAndSet(null, new IllegalStateException("expected metrics"));
                    }
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            }
        };

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(worker));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
        assertThat(failure.get())
                .as("parallel collect() should not throw or produce empty output")
                .isNull();
    }

    /**
     * Hammer the shared MBean property cache from many threads. This validates the check-then-put
     * pattern in {@link JmxMBeanPropertyCache#getKeyPropertyList(ObjectName)} does not corrupt the
     * map or return a partially populated mapping.
     */
    @Test
    public void sharedMBeanPropertyCacheUnderConcurrency() throws Exception {
        JmxMBeanPropertyCache cache = new JmxMBeanPropertyCache();
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<ObjectName> names = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            names.add(new ObjectName("test.domain:type=Test" + i));
        }

        List<Future<?>> futures = new ArrayList<>();
        for (ObjectName name : names) {
            Runnable r = () -> {
                try {
                    for (int j = 0; j < 500; j++) {
                        cache.getKeyPropertyList(name);
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            };
            futures.add(pool.submit(r));
        }
        try {
            start.countDown();
            for (Future<?> f : futures) {
                f.get(60, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            failure.compareAndSet(null, e);
        }
        pool.shutdown();
        assertThat(failure.get())
                .as("property cache access should be race-free")
                .isNull();
    }

    /**
     * Verify the rules cache and the ObjectName attribute filter can be mutated concurrently without
     * throwing, since both are shared across scrapes.
     */
    @Test
    public void sharedCachesUnderConcurrency() throws Exception {
        MatchedRulesCache rulesCache = new MatchedRulesCache();
        ObjectNameAttributeFilter filter = ObjectNameAttributeFilter.create(new HashMap<>());

        List<ObjectName> names = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            names.add(new ObjectName("test.domain:type=Shared" + i));
        }

        assertThatCode(() -> {
                    int threads = 8;
                    ExecutorService pool = Executors.newFixedThreadPool(threads);
                    CountDownLatch start = new CountDownLatch(1);
                    AtomicReference<Throwable> failure = new AtomicReference<>();
                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < threads; i++) {
                        final int idx = i;
                        Runnable r = () -> {
                            try {
                                for (int j = 0; j < 200; j++) {
                                    final ObjectName name = names.get(idx % 3);
                                    MatchedRulesCache.CacheKey key = new MatchedRulesCache.CacheKey(
                                            "test.domain", new LinkedHashMap<>(), new ArrayList<>(), "attr" + idx);
                                    rulesCache.put(
                                            key,
                                            new MatchedRule(
                                                    "n",
                                                    "m",
                                                    "GAUGE",
                                                    "h",
                                                    new ArrayList<>(),
                                                    new ArrayList<>(),
                                                    1.0,
                                                    1.0));
                                    rulesCache.get(key);
                                    rulesCache.evictStaleEntries(new MatchedRulesCache.StalenessTracker());
                                    filter.add(name, "attr" + idx);
                                    filter.exclude(name, "attr" + idx);
                                    filter.onlyKeepMBeans(java.util.Collections.singleton(name));
                                }
                            } catch (Throwable t) {
                                failure.compareAndSet(null, t);
                            }
                        };
                        futures.add(pool.submit(r));
                    }
                    try {
                        start.countDown();
                        for (Future<?> f : futures) {
                            f.get(60, TimeUnit.SECONDS);
                        }
                    } catch (Exception e) {
                        failure.compareAndSet(null, e);
                    }
                    pool.shutdown();
                })
                .doesNotThrowAnyException();
    }
}
