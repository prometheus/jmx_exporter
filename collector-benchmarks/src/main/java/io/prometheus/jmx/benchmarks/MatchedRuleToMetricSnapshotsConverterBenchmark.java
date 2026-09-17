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

package io.prometheus.jmx.benchmarks;

import io.prometheus.jmx.MatchedRule;
import io.prometheus.jmx.MatchedRuleToMetricSnapshotsConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * Benchmarks {@link MatchedRuleToMetricSnapshotsConverter#convert(List)} for varying numbers of
 * matched rules and for unique versus colliding label sets.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class MatchedRuleToMetricSnapshotsConverterBenchmark {

    private static final int METRIC_NAME_COUNT = 10;

    /**
     * Number of matched rules to convert.
     */
    @Param({"100", "1000"})
    public int ruleCount;

    /**
     * Whether the rules within a metric group have unique label values.
     */
    @Param({"true", "false"})
    public boolean uniqueLabels;

    private List<MatchedRule> matchedRules;

    /**
     * Builds the matched rules under test.
     */
    @Setup(Level.Trial)
    public void setUp() {
        matchedRules = new ArrayList<>(ruleCount);
        for (int i = 0; i < ruleCount; i++) {
            String metricName = "benchmark_metric_" + (i % METRIC_NAME_COUNT);
            String matchName =
                    "io.prometheus.jmx.benchmark<type=Benchmark, name=bean" + i + "><>attribute_" + (i % 10) + ": 1.0";
            matchedRules.add(new MatchedRule(
                    metricName,
                    matchName,
                    "GAUGE",
                    "benchmark metric",
                    Collections.singletonList("instance"),
                    Collections.singletonList(uniqueLabels ? "instance-" + i : "shared"),
                    1.0,
                    1.0));
        }
    }

    /**
     * Converts matched rules into metric snapshots.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void convert(Blackhole blackhole) {
        blackhole.consume(MatchedRuleToMetricSnapshotsConverter.convert(matchedRules));
    }
}
