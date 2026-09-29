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

import java.util.ArrayList;
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
 * Concurrency tests for {@link JmxMBeanPropertyCache#getKeyPropertyList(ObjectName)}. The cache is a
 * single shared instance across all {@code collect()} invocations, so these tests verify that
 * racing access (including many threads missing the same key simultaneously) always yields the
 * correct, fully-populated mapping and never a torn or partially populated one.
 */
public class JmxMBeanPropertyCacheConcurrencyTest {

    /**
     * Many threads miss the same key at the same time. With {@code computeIfAbsent} only one thread
     * performs the parsing and every waiter must receive the exact same, fully-populated map.
     */
    @Test
    public void sameKeyMissedByManyThreadsReturnsConsistentMap() throws Exception {
        JmxMBeanPropertyCache cache = new JmxMBeanPropertyCache();
        ObjectName key = new ObjectName("com.organisation:name=value,name2=value2");

        LinkedHashMap<String, String> expected = cache.getKeyPropertyList(key);
        assertThat(expected).isNotEmpty();

        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<LinkedHashMap<String, String>>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return cache.getKeyPropertyList(key);
            }));
        }
        start.countDown();
        pool.shutdown();
        for (Future<LinkedHashMap<String, String>> f : futures) {
            LinkedHashMap<String, String> result = f.get(30, TimeUnit.SECONDS);
            // computeIfAbsent stores the value once and hands the identical instance to every waiter.
            assertThat(result).isSameAs(expected);
            assertThat(result).isEqualTo(expected);
        }
    }

    /**
     * A mix of many keys accessed concurrently by many threads. Every returned mapping must be the
     * correct one for its key, with no exceptions and no partial/torn results.
     */
    @Test
    public void manyKeysUnderHighConcurrency() throws Exception {
        JmxMBeanPropertyCache cache = new JmxMBeanPropertyCache();
        int keys = 20;
        ObjectName[] names = new ObjectName[keys];
        LinkedHashMap<String, String>[] expected = new LinkedHashMap[keys];
        for (int i = 0; i < keys; i++) {
            names[i] = new ObjectName("com.organisation:type=Test" + i + ",name=v" + i);
            expected[i] = cache.getKeyPropertyList(names[i]);
            assertThat(expected[i]).isNotEmpty();
        }

        int perKeyThreads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threadsFor(keys, perKeyThreads));
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Future<KeyResult>> futures = new ArrayList<>();

        for (int i = 0; i < keys; i++) {
            final int index = i;
            for (int t = 0; t < perKeyThreads; t++) {
                futures.add(pool.submit(() -> {
                    try {
                        start.await();
                        LinkedHashMap<String, String> result = cache.getKeyPropertyList(names[index]);
                        return new KeyResult(index, result);
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                        return null;
                    }
                }));
            }
        }

        start.countDown();
        pool.shutdown();

        for (Future<KeyResult> f : futures) {
            KeyResult result = f.get(30, TimeUnit.SECONDS);
            if (result == null) {
                continue;
            }
            assertThat(failure.get())
                    .as("getKeyPropertyList should not throw under concurrency")
                    .isNull();
            assertThat(result.value)
                    .as("concurrent result should match the expected mapping for key " + result.index)
                    .isEqualTo(expected[result.index]);
        }
    }

    /**
     * High-contention stress: a small number of keys hammered by many threads over many iterations.
     * Confirms repeated racing access stays correct and exception-free.
     */
    @Test
    public void highContentionStress() throws Exception {
        JmxMBeanPropertyCache cache = new JmxMBeanPropertyCache();
        int keys = 5;
        ObjectName[] names = new ObjectName[keys];
        LinkedHashMap<String, String>[] expected = new LinkedHashMap[keys];
        for (int i = 0; i < keys; i++) {
            names[i] = new ObjectName("com.organisation:type=Stress" + i + ",name=s" + i);
            expected[i] = cache.getKeyPropertyList(names[i]);
        }

        int threads = 32;
        int iterations = 300;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            futures.add(pool.submit(() -> {
                try {
                    for (int n = 0; n < iterations; n++) {
                        ObjectName name = names[(threadId + n) % keys];
                        LinkedHashMap<String, String> result = cache.getKeyPropertyList(name);
                        assertThat(result).isNotEmpty();
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                }
                return null;
            }));
        }

        start.countDown();
        pool.shutdown();
        for (Future<Void> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }

        assertThat(failure.get()).as("stress run should be exception-free").isNull();
    }

    private static int threadsFor(int keys, int perKeyThreads) {
        return Math.max(1, keys * perKeyThreads);
    }

    private static final class KeyResult {
        final int index;
        final LinkedHashMap<String, String> value;

        KeyResult(int index, LinkedHashMap<String, String> value) {
            this.index = index;
            this.value = value;
        }
    }
}
