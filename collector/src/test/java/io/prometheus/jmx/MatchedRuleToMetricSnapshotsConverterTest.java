package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.MetricMetadata;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class MatchedRuleToMetricSnapshotsConverterTest {

    @Test
    public void testMatchedRuleAggregation() {
        List<MatchedRule> matchedRules = new ArrayList<>();

        matchedRules.add(
                new MatchedRule(
                        "jvm_memory_committed_bytes",
                        "java.lang<type=Memory><HeapMemoryUsage>committed: 16252928",
                        "UNKNOWN",
                        "java.lang.management.MemoryUsage"
                                + " java.lang:name=null,type=Memory,attribute=committed",
                        of("area"),
                        of("heap"),
                        1.6252928E7,
                        1.0));

        matchedRules.add(
                new MatchedRule(
                        "jvm_memory_committed_bytes",
                        "java.lang<type=Memory><NonHeapMemoryUsage>committed: 17170432",
                        "UNKNOWN",
                        "java.lang.management.MemoryUsage"
                                + " java.lang:name=null,type=Memory,attribute=committed",
                        of("area"),
                        of("nonheap"),
                        2.1757952E7,
                        1.0));

        MetricSnapshots metricSnapshots =
                MatchedRuleToMetricSnapshotsConverter.convert(matchedRules);

        assertThat(metricSnapshots).hasSize(1);

        metricSnapshots.forEach(
                metricSnapshot -> {
                    MetricMetadata metricMetadata = metricSnapshot.getMetadata();

                    assertThat(metricMetadata.getName()).isEqualTo("jvm_memory_committed_bytes");
                    assertThat(metricMetadata.getPrometheusName())
                            .isEqualTo("jvm_memory_committed_bytes");

                    List<? extends DataPointSnapshot> dataPointSnapshots =
                            metricSnapshot.getDataPoints();
                    assertThat(dataPointSnapshots).hasSize(2);
                });
    }

    private static List<String> of(String... strings) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, strings);
        return list;
    }
}
