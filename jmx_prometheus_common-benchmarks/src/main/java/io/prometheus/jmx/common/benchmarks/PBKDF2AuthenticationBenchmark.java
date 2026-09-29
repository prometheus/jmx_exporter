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

import io.prometheus.jmx.common.authenticator.PBKDF2Authenticator;
import java.security.GeneralSecurityException;
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

/**
 * Benchmarks {@link PBKDF2Authenticator#checkCredentials(String, String)} for cache hits, invalid
 * misses, and valid misses. The miss path is dominated by key derivation by design.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class PBKDF2AuthenticationBenchmark {

    private static final String REALM = "/";
    private static final String USERNAME = "Prometheus";
    private static final String PASSWORD = "secret";
    private static final String SALT = "98LeBWIjca";
    private static final int ITERATIONS = 1000;
    private static final int KEY_LENGTH_BITS = 128;

    /**
     * PBKDF2 algorithm.
     */
    @Param({"PBKDF2WithHmacSHA1", "PBKDF2WithHmacSHA256"})
    public String algorithm;

    private String hash;
    private PBKDF2Authenticator authenticator;

    /**
     * Builds and warms the authenticator so the timed path is a cache hit.
     *
     * @throws GeneralSecurityException if the hash cannot be computed
     */
    @Setup(Level.Trial)
    public void setUp() throws GeneralSecurityException {
        hash = BenchmarkSupport.pbkdf2Hash(algorithm, SALT, ITERATIONS, KEY_LENGTH_BITS, PASSWORD);
        authenticator = new PBKDF2Authenticator(REALM, USERNAME, hash, algorithm, SALT, ITERATIONS, KEY_LENGTH_BITS);
        authenticator.checkCredentials(USERNAME, PASSWORD);
    }

    /**
     * Benchmarks the valid-credentials cache hit path.
     *
     * @return whether authentication succeeded
     */
    @Benchmark
    public boolean cacheHit() {
        return authenticator.checkCredentials(USERNAME, PASSWORD);
    }

    /**
     * Benchmarks the invalid-credentials miss path (never cached), including key derivation.
     *
     * @return whether authentication succeeded
     */
    @Benchmark
    public boolean invalidMiss() {
        return authenticator.checkCredentials("bad", "credentials");
    }

    /**
     * Benchmarks the valid-credentials miss path including authenticator construction.
     *
     * @return whether authentication succeeded
     * @throws GeneralSecurityException if the hash cannot be computed
     */
    @Benchmark
    public boolean validMiss() throws GeneralSecurityException {
        return new PBKDF2Authenticator(REALM, USERNAME, hash, algorithm, SALT, ITERATIONS, KEY_LENGTH_BITS)
                .checkCredentials(USERNAME, PASSWORD);
    }
}
