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

import io.prometheus.jmx.common.authenticator.MessageDigestAuthenticator;
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
 * Benchmarks {@link MessageDigestAuthenticator#checkCredentials(String, String)} for cache hits,
 * invalid misses, and valid misses across digest algorithms.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MessageDigestAuthenticationBenchmark {

    private static final String REALM = "/";
    private static final String USERNAME = "Prometheus";
    private static final String PASSWORD = "secret";
    private static final String SALT = "98LeBWIjca";

    /**
     * Message digest algorithm.
     */
    @Param({"SHA-256", "SHA-512"})
    public String algorithm;

    private String hash;
    private MessageDigestAuthenticator authenticator;

    /**
     * Builds and warms the authenticator so the timed path is a cache hit.
     *
     * @throws GeneralSecurityException if the hash cannot be computed
     */
    @Setup(Level.Trial)
    public void setUp() throws GeneralSecurityException {
        hash = BenchmarkSupport.messageDigestHash(algorithm, SALT, PASSWORD);
        authenticator = new MessageDigestAuthenticator(REALM, USERNAME, hash, algorithm, SALT);
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
     * Benchmarks the invalid-credentials miss path (never cached), including the digest computation.
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
        return new MessageDigestAuthenticator(REALM, USERNAME, hash, algorithm, SALT)
                .checkCredentials(USERNAME, PASSWORD);
    }
}
