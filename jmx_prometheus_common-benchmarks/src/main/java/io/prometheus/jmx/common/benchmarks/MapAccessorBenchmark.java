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

import io.prometheus.jmx.common.util.MapAccessor;
import java.util.LinkedHashMap;
import java.util.Map;
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
 * Benchmarks {@link MapAccessor} path traversal for shallow, deep, and missing paths.
 *
 * <p>This is configuration-time work; results are informational unless profiling shows a material
 * startup or reload cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MapAccessorBenchmark {

    /**
     * Path profile exercised by the benchmark.
     */
    @Param({"shallow", "deep", "missing"})
    public String pathProfile;

    private MapAccessor accessor;
    private String path;

    /**
     * Builds the configuration map and selects the path under test.
     */
    @Setup(Level.Trial)
    public void setUp() {
        Map<Object, Object> leaf = new LinkedHashMap<>();
        leaf.put("c", "value");
        Map<Object, Object> middle = new LinkedHashMap<>();
        middle.put("b", leaf);
        Map<Object, Object> root = new LinkedHashMap<>();
        root.put("x", "y");
        root.put("a", middle);
        accessor = MapAccessor.of(root);

        switch (pathProfile) {
            case "shallow":
                path = "/x";
                break;
            case "deep":
                path = "/a/b/c";
                break;
            default:
                path = "/a/b/missing";
                break;
        }
    }

    /**
     * Benchmarks {@link MapAccessor#containsPath(String)}.
     *
     * @return whether the path is present
     */
    @Benchmark
    public boolean containsPath() {
        return accessor.containsPath(path);
    }

    /**
     * Benchmarks {@link MapAccessor#getPath(String)}.
     *
     * @return the value at the path, or {@code null}
     */
    @Benchmark
    public Object getPath() {
        return accessor.getPath(path).orElse(null);
    }

    /**
     * Benchmarks {@link MapAccessor#getPath(String, Class)}.
     *
     * @return the typed value at the path, or {@code null}
     */
    @Benchmark
    public String getPathTyped() {
        return accessor.getPath(path, String.class).orElse(null);
    }
}
