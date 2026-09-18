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

package io.prometheus.jmx.javaagent.benchmarks;

import io.prometheus.jmx.Arguments;
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
 * Benchmarks {@link Arguments#parse(String)} for every documented argument form.
 *
 * <p>Argument parsing runs once per agent start; results are informational unless a bootstrap
 * profile shows it matters.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ArgumentsBenchmark {

    private static final String CONFIG_FILE = "/tmp/jmx-exporter-benchmark.yaml";

    /**
     * Agent argument form.
     */
    @Param({"portFile", "hostPortFile", "ipv6", "fileOnly"})
    public String form;

    private String argument;

    /**
     * Selects the argument under test.
     */
    @Setup(Level.Trial)
    public void setUp() {
        switch (form) {
            case "hostPortFile":
                argument = "localhost:9404:" + CONFIG_FILE;
                break;
            case "ipv6":
                argument = "[::1]:9404:" + CONFIG_FILE;
                break;
            case "fileOnly":
                argument = CONFIG_FILE;
                break;
            case "portFile":
            default:
                argument = "9404:" + CONFIG_FILE;
                break;
        }
    }

    /**
     * Benchmarks {@link Arguments#parse(String)}.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void parse(Blackhole blackhole) {
        blackhole.consume(Arguments.parse(argument));
    }
}
