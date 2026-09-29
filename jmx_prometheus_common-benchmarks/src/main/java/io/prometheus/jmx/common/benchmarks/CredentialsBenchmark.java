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
 * Benchmarks {@link Credentials} construction, equality, hashing, and size accounting for short and
 * long values.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class CredentialsBenchmark {

    /**
     * Credential length profile.
     */
    @Param({"short", "long"})
    public String size;

    private String username;
    private String password;
    private Credentials credentials;
    private Credentials equalCredentials;
    private Credentials otherCredentials;

    /**
     * Builds the credentials under test.
     */
    @Setup(Level.Trial)
    public void setUp() {
        if ("long".equals(size)) {
            username = repeat("username-", 64);
            password = repeat("password-", 64);
        } else {
            username = "Prometheus";
            password = "secret";
        }

        credentials = new Credentials(username, password);
        equalCredentials = new Credentials(username, password);
        otherCredentials = new Credentials(username + "x", password);
    }

    /**
     * Benchmarks {@link Credentials#Credentials(String, String)}.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void construct(Blackhole blackhole) {
        blackhole.consume(new Credentials(username, password));
    }

    /**
     * Benchmarks {@link Credentials#equals(Object)} for equal values.
     *
     * @return whether the credentials are equal
     */
    @Benchmark
    public boolean equalsEqual() {
        return credentials.equals(equalCredentials);
    }

    /**
     * Benchmarks {@link Credentials#equals(Object)} for unequal values.
     *
     * @return whether the credentials are equal
     */
    @Benchmark
    public boolean equalsUnequal() {
        return credentials.equals(otherCredentials);
    }

    /**
     * Benchmarks {@link Credentials#hashCode()}.
     *
     * @return the hash code
     */
    @Benchmark
    public int hashCode() {
        return credentials.hashCode();
    }

    /**
     * Benchmarks {@link Credentials#byteSize()}.
     *
     * @return the byte size
     */
    @Benchmark
    public int byteSize() {
        return credentials.byteSize();
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
