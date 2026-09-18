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

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks the package-private name-conversion helpers in {@link JmxCollector}.
 *
 * <p>This benchmark lives in the {@code io.prometheus.jmx} package so it can exercise the internal
 * hot paths directly.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class NameConversionBenchmark {

    /**
     * Attribute/bean name to convert.
     */
    @Param({"someAttributeName", "some_attribute_name", "URLValue", "a.b.c-D", "already_safe"})
    public String name;

    /**
     * Benchmarks {@link JmxCollector#toSafeName(String)}.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void toSafeName(Blackhole blackhole) {
        blackhole.consume(JmxCollector.toSafeName(name));
    }

    /**
     * Benchmarks {@link JmxCollector#toSnakeAndLowerCase(String)}.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void toSnakeAndLowerCase(Blackhole blackhole) {
        blackhole.consume(JmxCollector.toSnakeAndLowerCase(name));
    }
}
