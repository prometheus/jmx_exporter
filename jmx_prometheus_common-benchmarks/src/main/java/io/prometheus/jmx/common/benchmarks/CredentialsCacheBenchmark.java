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

import io.prometheus.jmx.common.authenticator.Credentials;
import io.prometheus.jmx.common.authenticator.CredentialsCache;
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
 * Benchmarks {@link CredentialsCache} lookups, insertion, and capacity handling.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class CredentialsCacheBenchmark {

    /**
     * Number of credentials preloaded into the cache. The default cache holds roughly 100 short
     * credentials, so {@code 5000} exercises the weight-limited/eviction regime.
     */
    @Param({"50", "5000"})
    public int credentialCount;

    private CredentialsCache cache;
    private Credentials[] credentials;
    private Credentials missing;
    private int addIndex;

    /**
     * Preloads the cache with the requested number of credentials.
     */
    @Setup(Level.Trial)
    public void setUp() {
        cache = new CredentialsCache(
                CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES, CredentialsCache.DEFAULT_MAX_ENTRIES);
        credentials = new Credentials[credentialCount];
        for (int i = 0; i < credentialCount; i++) {
            credentials[i] = new Credentials("user-" + i, "password-" + i);
            cache.add(credentials[i]);
        }
        cache.getCurrentEntries();
        missing = new Credentials("absent", "absent");
    }

    /**
     * Benchmarks a cache hit.
     *
     * @return whether the credential was present
     */
    @Benchmark
    public boolean containsHit() {
        return cache.contains(credentials[0]);
    }

    /**
     * Benchmarks a cache miss.
     *
     * @return whether the credential was present
     */
    @Benchmark
    public boolean containsMiss() {
        return cache.contains(missing);
    }

    /**
     * Benchmarks adding a credential, cycling through the preloaded set so the cache weight stays
     * bounded.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void add(Blackhole blackhole) {
        Credentials credential = credentials[addIndex++ % credentials.length];
        cache.add(credential);
        blackhole.consume(credential);
    }

    /**
     * Benchmarks {@link CredentialsCache#getCurrentEntries()}.
     *
     * @return the approximate entry count
     */
    @Benchmark
    public int getCurrentEntries() {
        return cache.getCurrentEntries();
    }
}
