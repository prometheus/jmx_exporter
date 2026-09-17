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

import io.prometheus.jmx.ObjectNameAttributeFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.management.ObjectName;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks the include/exclude lookups of {@link ObjectNameAttributeFilter}.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class ObjectNameAttributeFilterBenchmark {

    private static final int ATTRIBUTE_COUNT = 10;

    private ObjectName objectName;
    private ObjectNameAttributeFilter filter;

    /**
     * Builds a filter with configured include and exclude attribute lists.
     *
     * @throws Exception if the ObjectName is malformed
     */
    @Setup(Level.Trial)
    public void setUp() throws Exception {
        objectName = new ObjectName("io.prometheus.jmx.benchmark:type=Benchmark,name=bean0");

        List<String> attributeNames = new ArrayList<>(ATTRIBUTE_COUNT);
        for (int i = 0; i < ATTRIBUTE_COUNT; i++) {
            attributeNames.add("attribute_" + i);
        }

        Map<Object, Object> excludeObjectNameAttributes = new HashMap<>();
        excludeObjectNameAttributes.put(objectName.getCanonicalName(), attributeNames);

        Map<Object, Object> includeObjectNameAttributes = new HashMap<>();
        includeObjectNameAttributes.put(objectName.getCanonicalName(), new ArrayList<>(attributeNames));

        Map<String, Object> config = new HashMap<>();
        config.put(ObjectNameAttributeFilter.EXCLUDE_OBJECT_NAME_ATTRIBUTES, excludeObjectNameAttributes);
        config.put(ObjectNameAttributeFilter.INCLUDE_OBJECT_NAME_ATTRIBUTES, includeObjectNameAttributes);

        filter = ObjectNameAttributeFilter.create(config);
    }

    /**
     * Benchmarks an excluded attribute lookup.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void excludeHit(Blackhole blackhole) {
        blackhole.consume(filter.exclude(objectName, "attribute_5"));
    }

    /**
     * Benchmarks a non-excluded attribute lookup.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void excludeMiss(Blackhole blackhole) {
        blackhole.consume(filter.exclude(objectName, "attribute_500"));
    }

    /**
     * Benchmarks an included attribute lookup.
     *
     * @param blackhole JMH blackhole
     */
    @Benchmark
    public void include(Blackhole blackhole) {
        blackhole.consume(filter.include(objectName, "attribute_5"));
    }
}
