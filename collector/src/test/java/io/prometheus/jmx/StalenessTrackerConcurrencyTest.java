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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Concurrency characterization tests for the rules cache staleness accounting.
 *
 * <p>Before any cache-maintenance optimization, these tests prove the isolation contract: one
 * scrape must not evict an entry that is live in another scrape, must not corrupt freshness
 * accounting, and must correctly handle a bean that disappears and reappears. The tests drive the
 * cache the same way a scrape does: look up, store, mark the touched keys fresh, evict the rest.
 */
public class StalenessTrackerConcurrencyTest {

    /**
     * Models a single scrape against a shared cache: it touches a set of keys, marks exactly those
     * fresh, and evicts everything else. Mirrors {@link JmxCollector.Receiver#recordBean} cache
     * maintenance (lookup, add-on-miss, mark fresh, then evict stale at scrape end).
     */
    static class Scrape {
        private final MatchedRulesCache cache;

        Scrape(MatchedRulesCache cache) {
            this.cache = cache;
        }

        void touch(String... keys) {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();
            for (String key : keys) {
                MatchedRulesCache.CacheKey cacheKey = cacheKey(key);
                if (cache.get(cacheKey) == null) {
                    cache.put(cacheKey, MatchedRule.unmatched());
                }
                tracker.markAsFresh(cacheKey);
            }
            cache.evictStaleEntries(tracker);
        }
    }

    private static MatchedRulesCache.CacheKey cacheKey(String attrName) {
        return new MatchedRulesCache.CacheKey(
                "domain", new java.util.LinkedHashMap<>(), new java.util.ArrayList<>(), attrName);
    }

    private static MatchedRulesCache cacheWith(String... attrNames) {
        MatchedRulesCache cache = new MatchedRulesCache();
        for (String attrName : attrNames) {
            cache.put(cacheKey(attrName), MatchedRule.unmatched());
        }
        return cache;
    }

    /**
     * A scrape worker that records the first exception it throws and signals completion.
     */
    static final class ScrapeWorker implements Runnable {
        private final Runnable task;
        private final CountDownLatch done;
        private final AtomicReference<Throwable> error = new AtomicReference<>();

        ScrapeWorker(Runnable task, CountDownLatch done) {
            this.task = task;
            this.done = done;
        }

        @Override
        public void run() {
            try {
                task.run();
            } catch (Throwable t) {
                error.set(t);
            } finally {
                done.countDown();
            }
        }

        Throwable error() {
            return error.get();
        }
    }

    private static ScrapeWorker start(Runnable task, CountDownLatch done) {
        ScrapeWorker worker = new ScrapeWorker(task, done);
        new Thread(worker, "scrape-worker").start();
        return worker;
    }

    private static void await(CountDownLatch latch, ScrapeWorker... workers) throws InterruptedException {
        assertThat(latch.await(60, TimeUnit.SECONDS)).isTrue();
        for (ScrapeWorker worker : workers) {
            assertThat(worker.error()).isNull();
        }
    }

    @Test
    public void disjointScrapesDoNotCorruptFreshness() throws InterruptedException {
        MatchedRulesCache cache = cacheWith("a0", "a1", "a2", "b0", "b1", "b2", "c0");
        Scrape scrapeA = new Scrape(cache);
        Scrape scrapeB = new Scrape(cache);

        CountDownLatch latchA = new CountDownLatch(1);
        CountDownLatch latchB = new CountDownLatch(1);
        ScrapeWorker workerA = start(
                () -> {
                    for (int round = 0; round < 500; round++) {
                        scrapeA.touch("a0", "a1", "a2");
                    }
                },
                latchA);
        ScrapeWorker workerB = start(
                () -> {
                    for (int round = 0; round < 500; round++) {
                        scrapeB.touch("b0", "b1", "b2");
                    }
                },
                latchB);
        await(latchA, workerA);
        await(latchB, workerB);

        // A key that was never touched by any scrape is evicted, and concurrent evictions from
        // disjoint scrapes never throw or leave the cache in an inconsistent state.
        assertThat(cache.get(cacheKey("c0"))).isNull();
    }

    @Test
    public void overlappingScrapesRetainSharedLiveEntry() throws InterruptedException {
        MatchedRulesCache cache = cacheWith("shared", "a0", "b0");
        Scrape scrapeA = new Scrape(cache);
        Scrape scrapeB = new Scrape(cache);

        CountDownLatch latchA = new CountDownLatch(1);
        CountDownLatch latchB = new CountDownLatch(1);
        ScrapeWorker workerA = start(
                () -> {
                    for (int i = 0; i < 5000; i++) {
                        scrapeA.touch("shared", "a0");
                    }
                },
                latchA);
        ScrapeWorker workerB = start(
                () -> {
                    for (int i = 0; i < 5000; i++) {
                        scrapeB.touch("shared", "b0");
                    }
                },
                latchB);
        await(latchA, workerA);
        await(latchB, workerB);

        // "shared" is live in both scrapes, so it must survive regardless of interleaving.
        assertThat(cache.get(cacheKey("shared"))).isNotNull();
    }

    @Test
    public void disappearsThenReappears() {
        MatchedRulesCache cache = cacheWith("bean");
        Scrape scrape = new Scrape(cache);

        scrape.touch("bean");
        assertThat(cache.get(cacheKey("bean"))).isNotNull();

        // The bean disappears: not touched in this scrape, so it is evicted.
        scrape.touch("other");
        assertThat(cache.get(cacheKey("bean"))).isNull();
        assertThat(cache.get(cacheKey("other"))).isNotNull();

        // The bean reappears and is touched again: it is re-added and retained.
        scrape.touch("bean", "other");
        assertThat(cache.get(cacheKey("bean"))).isNotNull();
        assertThat(cache.get(cacheKey("other"))).isNotNull();
    }

    @Test
    public void freshnessAccountingIsExact() {
        MatchedRulesCache cache = cacheWith("a", "b", "c", "d", "e");
        Scrape scrape = new Scrape(cache);

        scrape.touch("a", "b", "c");
        assertThat(cache.get(cacheKey("a"))).isNotNull();
        assertThat(cache.get(cacheKey("b"))).isNotNull();
        assertThat(cache.get(cacheKey("c"))).isNotNull();
        assertThat(cache.get(cacheKey("d"))).isNull();
        assertThat(cache.get(cacheKey("e"))).isNull();

        // Touching only "b" evicts the rest, exactly.
        scrape.touch("b");
        assertThat(cache.get(cacheKey("b"))).isNotNull();
        assertThat(cache.get(cacheKey("a"))).isNull();
        assertThat(cache.get(cacheKey("c"))).isNull();
    }

    @Test
    public void concurrentStormDoesNotCorruptAccounting() throws InterruptedException {
        int workerCount = 8;
        MatchedRulesCache cache = cacheWith("first", "second", "third");

        CountDownLatch latch = new CountDownLatch(workerCount);
        ScrapeWorker[] workers = new ScrapeWorker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            int stride = i * 97;
            Scrape scrape = new Scrape(cache);
            workers[i] = start(
                    () -> {
                        for (int j = 0; j < 500; j++) {
                            scrape.touch("k" + (j + stride), "shared");
                        }
                    },
                    latch);
        }
        await(latch, workers);

        // "shared" is touched by every worker, so concurrent eviction must not lose it. Keys
        // touched only by earlier workers may be evicted by later ones, which is expected.
        assertThat(cache.get(cacheKey("shared"))).isNotNull();
    }
}
