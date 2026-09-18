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

import io.prometheus.jmx.common.util.YamlSupport;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks {@link YamlSupport} YAML loading for empty and representative configurations.
 *
 * <p>This is configuration-time work; results are informational unless profiling shows a material
 * startup or reload cost.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class YamlSupportBenchmark {

    private static final String REPRESENTATIVE = "httpServer:\n"
            + "  metrics:\n"
            + "    path: /metrics\n"
            + "  threads:\n"
            + "    minimum: 1\n"
            + "    maximum: 10\n"
            + "    keepAliveTime: 120\n"
            + "rules:\n"
            + "  - pattern: '.*'\n";

    /**
     * Configuration profile.
     */
    @Param({"empty", "representative"})
    public String profile;

    private String yaml;
    private File file;

    /**
     * Prepares the YAML string and temp file for the selected profile.
     *
     * @throws Exception if the temp file cannot be written
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        yaml = "empty".equals(profile) ? "" : REPRESENTATIVE;
        file = File.createTempFile("yaml-benchmark-", ".yaml");
        file.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writer.print(yaml);
        }
    }

    /**
     * Removes the temp file.
     */
    @TearDown(Level.Trial)
    public void tearDown() {
        if (file != null) {
            file.delete();
        }
    }

    /**
     * Benchmarks {@link YamlSupport#loadYaml(String)}.
     *
     * @return the loaded configuration
     */
    @Benchmark
    public Map<Object, Object> loadYamlFromString() {
        return YamlSupport.loadYaml(yaml);
    }

    /**
     * Benchmarks {@link YamlSupport#loadYaml(File)}.
     *
     * @return the loaded configuration
     * @throws Exception if the file cannot be read
     */
    @Benchmark
    public Map<Object, Object> loadYamlFromFile() throws Exception {
        return YamlSupport.loadYaml(file);
    }
}
