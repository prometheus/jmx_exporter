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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.prometheus.metrics.model.snapshots.DuplicateLabelsException;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DuplicateLabelsTest {

    Map<String, UnknownSnapshot.Builder> unknownMap;

    @BeforeEach
    public void setUp() {
        unknownMap = new HashMap<>();
    }

    @Test
    public void testDuplicateLabels() {
        UnknownSnapshot.Builder unknownBuilder =
                unknownMap.computeIfAbsent(
                        "test",
                        name ->
                                UnknownSnapshot.builder()
                                        .name("test_metric")
                                        .help("test_metric help"));
        unknownBuilder.dataPoint(
                UnknownSnapshot.UnknownDataPointSnapshot.builder()
                        .value(1.12345678)
                        .labels(Labels.of("label1", "value1"))
                        .build());

        unknownMap.put("test", unknownBuilder);

        unknownBuilder =
                unknownMap.computeIfAbsent(
                        "test",
                        name ->
                                UnknownSnapshot.builder()
                                        .name("test_metric")
                                        .help("test_metric help"));
        unknownBuilder.dataPoint(
                UnknownSnapshot.UnknownDataPointSnapshot.builder()
                        .value(2.2468)
                        .labels(Labels.of("label1", "value1"))
                        .build());

        unknownMap.put("test2", unknownBuilder);

        MetricSnapshots.Builder result = MetricSnapshots.builder();

        assertThatExceptionOfType(DuplicateLabelsException.class)
                .isThrownBy(
                        () -> {
                            for (UnknownSnapshot.Builder unknown : unknownMap.values()) {
                                UnknownSnapshot unknownSnapshot = unknown.build();
                                result.metricSnapshot(unknownSnapshot);
                            }
                        })
                .withMessage("Duplicate labels for metric \"test_metric\": {label1=\"value1\"}");
    }
}
