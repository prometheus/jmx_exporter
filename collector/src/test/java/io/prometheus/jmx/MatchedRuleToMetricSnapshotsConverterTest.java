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

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MatchedRuleToMetricSnapshotsConverterTest {

    @Test
    public void testMatchedRuleAggregation() {
        List<MatchedRule> matchedRules = new ArrayList<>();

        matchedRules.add(new MatchedRule(
                "jvm_memory_committed_bytes",
                "java.lang<type=Memory><HeapMemoryUsage>committed: 16252928",
                "UNKNOWN",
                "java.lang.management.MemoryUsage" + " java.lang:name=null,type=Memory,attribute=committed",
                of("area"),
                of("heap"),
                1.6252928E7,
                1.0));

        matchedRules.add(new MatchedRule(
                "jvm_memory_committed_bytes",
                "java.lang<type=Memory><NonHeapMemoryUsage>committed: 17170432",
                "UNKNOWN",
                "java.lang.management.MemoryUsage" + " java.lang:name=null,type=Memory,attribute=committed",
                of("area"),
                of("nonheap"),
                2.1757952E7,
                1.0));

        MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

        assertThat(metricSnapshots).hasSize(1);

        metricSnapshots.forEach(metricSnapshot -> {
            MetricMetadata metricMetadata = metricSnapshot.getMetadata();

            assertThat(metricMetadata.getName()).isEqualTo("jvm_memory_committed_bytes");
            assertThat(metricMetadata.getPrometheusName()).isEqualTo("jvm_memory_committed_bytes");

            List<? extends DataPointSnapshot> dataPointSnapshots = metricSnapshot.getDataPoints();
            assertThat(dataPointSnapshots).hasSize(2);
        });
    }

    @Nested
    class CounterTypeTests {

        @Test
        public void testCounterTypeConversion() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "http_requests",
                    "http<name=server>requests: 100",
                    "COUNTER",
                    "Total HTTP requests",
                    of("method"),
                    of("get"),
                    100.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(CounterSnapshot.class);
            assertThat(snapshot.getMetadata().getName()).isEqualTo("http_requests");
        }
    }

    @Nested
    class GaugeTypeTests {

        @Test
        public void testGaugeTypeConversion() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "jvm_memory_bytes",
                    "java.lang<type=Memory><HeapMemoryUsage>used: 5000",
                    "GAUGE",
                    "JVM memory usage",
                    of("area"),
                    of("heap"),
                    5000.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(GaugeSnapshot.class);
            assertThat(snapshot.getMetadata().getName()).isEqualTo("jvm_memory_bytes");
        }
    }

    @Nested
    class MixedTypeTests {

        @Test
        public void testMixedTypesFallbackToUnknown() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "mixed_metric",
                    "domain<type=Type1>attr: 1",
                    "GAUGE",
                    "help",
                    of("label1"),
                    of("value1"),
                    1.0,
                    1.0));
            matchedRules.add(new MatchedRule(
                    "mixed_metric",
                    "domain<type=Type2>attr: 2",
                    "COUNTER",
                    "help",
                    of("label1"),
                    of("value2"),
                    2.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(UnknownSnapshot.class);
        }

        @Test
        public void testAllSameTypeReturnsThatType() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "gauge_metric",
                    "domain<type=Type1>attr: 1",
                    "GAUGE",
                    "help",
                    of("label1"),
                    of("value1"),
                    1.0,
                    1.0));
            matchedRules.add(new MatchedRule(
                    "gauge_metric",
                    "domain<type=Type2>attr: 2",
                    "GAUGE",
                    "help",
                    of("label1"),
                    of("value2"),
                    2.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(GaugeSnapshot.class);
        }
    }

    @Nested
    class DuplicateLabelsTests {

        @Test
        public void testDuplicateLabelsAddsObjectnameLabel() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "duplicate_metric",
                    "domain1<type=Type>attr: 1",
                    "UNKNOWN",
                    "help",
                    of("label1"),
                    of("value1"),
                    1.0,
                    1.0));
            matchedRules.add(new MatchedRule(
                    "duplicate_metric",
                    "domain2<type=Type>attr: 2",
                    "UNKNOWN",
                    "help",
                    of("label1"),
                    of("value1"),
                    2.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(UnknownSnapshot.class);
            assertThat(snapshot.getDataPoints()).hasSize(2);

            for (DataPointSnapshot dp : snapshot.getDataPoints()) {
                assertThat(dp.getLabels().get("_objectname")).isNotNull();
            }
        }

        @Test
        public void testUniqueLabelsDoesNotAddObjectnameLabel() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "unique_metric",
                    "domain1<type=Type>attr: 1",
                    "UNKNOWN",
                    "help",
                    of("label1"),
                    of("value1"),
                    1.0,
                    1.0));
            matchedRules.add(new MatchedRule(
                    "unique_metric",
                    "domain2<type=Type>attr: 2",
                    "UNKNOWN",
                    "help",
                    of("label1"),
                    of("value2"),
                    2.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            for (DataPointSnapshot dp : snapshot.getDataPoints()) {
                assertThat(dp.getLabels().get("_objectname")).isNull();
            }
        }
    }

    @Nested
    class DomainNameTests {

        @Test
        public void testGetDomainNameWithColonInMatchName() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "test_metric",
                    "java.lang<type=Memory>HeapMemoryUsage: 5000",
                    "GAUGE",
                    "help",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    5000.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);
        }

        @Test
        public void testGetDomainNameWithNoColonInMatchName() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "test_metric",
                    "noColonMatchName",
                    "GAUGE",
                    "help",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    1.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);
        }

        @Test
        public void testGetDomainNameWithColonAtPositionZero() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "test_metric",
                    ":starts_with_colon",
                    "GAUGE",
                    "help",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    1.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);
        }
    }

    @Nested
    class ConstructorTests {

        @Test
        public void testConstructor() {
            MatchedRuleToMetricSnapshotsConverter converter = new MatchedRuleToMetricSnapshotsConverter();
            assertThat(converter).isNotNull();
        }
    }

    @Nested
    class EmptyListTests {

        @Test
        public void testEmptyMatchedRulesList() {
            List<MatchedRule> matchedRules = new ArrayList<>();

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).isEmpty();
        }
    }

    @Nested
    class SingleRuleTests {

        @Test
        public void testSingleRuleConversion() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "single_metric",
                    "domain<type=Type>attr: 42",
                    "UNKNOWN",
                    "A single metric",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    42.0,
                    1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            assertThat(metricSnapshots).hasSize(1);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot.getMetadata().getName()).isEqualTo("single_metric");
            assertThat(snapshot.getDataPoints()).hasSize(1);
        }
    }

    @Nested
    class DuplicateLabelsWithCounterTests {

        @Test
        public void testDuplicateLabelsCounterAddsObjectname() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "counter_metric", "domain1<type=Type>attr: 10", "COUNTER", "help", of("l"), of("v"), 10.0, 1.0));
            matchedRules.add(new MatchedRule(
                    "counter_metric", "domain2<type=Type>attr: 20", "COUNTER", "help", of("l"), of("v"), 20.0, 1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(CounterSnapshot.class);
            assertThat(snapshot.getDataPoints()).hasSize(2);

            for (DataPointSnapshot dp : snapshot.getDataPoints()) {
                assertThat(dp.getLabels().get("_objectname")).isNotNull();
            }
        }
    }

    @Nested
    class DuplicateLabelsWithGaugeTests {

        @Test
        public void testDuplicateLabelsGaugeAddsObjectname() {
            List<MatchedRule> matchedRules = new ArrayList<>();
            matchedRules.add(new MatchedRule(
                    "gauge_metric", "domain1<type=Type>attr: 10", "GAUGE", "help", of("l"), of("v"), 10.0, 1.0));
            matchedRules.add(new MatchedRule(
                    "gauge_metric", "domain2<type=Type>attr: 20", "GAUGE", "help", of("l"), of("v"), 20.0, 1.0));

            MetricSnapshots metricSnapshots = MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

            MetricSnapshot snapshot = metricSnapshots.iterator().next();
            assertThat(snapshot).isInstanceOf(GaugeSnapshot.class);
            assertThat(snapshot.getDataPoints()).hasSize(2);

            for (DataPointSnapshot dp : snapshot.getDataPoints()) {
                assertThat(dp.getLabels().get("_objectname")).isNotNull();
            }
        }
    }

    private static List<String> of(String... strings) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, strings);
        return list;
    }
}
