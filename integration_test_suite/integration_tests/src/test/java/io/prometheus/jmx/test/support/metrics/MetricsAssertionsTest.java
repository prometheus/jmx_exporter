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

package io.prometheus.jmx.test.support.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class MetricsAssertionsTest {

    @Test
    void buildInfoVersionLabelIsWildcarded() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("jmx_exporter_build_info", "version", "1.6.0-POST"))
                .isEqualTo("*");
        assertThat(MetricsAssertions.canonicalizeLabelValue("jmx_exporter_build_info", "version", "1.7.0"))
                .isEqualTo("*");
    }

    @Test
    void buildInfoNameLabelIsPreserved() {
        assertThat(MetricsAssertions.canonicalizeLabelValue(
                        "jmx_exporter_build_info", "name", "jmx_prometheus_standalone"))
                .isEqualTo("jmx_prometheus_standalone");
    }

    @Test
    void unrelatedVersionLabelIsPreserved() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("some_other_metric", "version", "1.2.3"))
                .isEqualTo("1.2.3");
    }

    @Test
    void knownSuffixIsStillCanonicalized() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("jvm_memory_pool_used_bytes", "pool", "G1 Eden Space"))
                .isEqualTo("*Eden Space");
        assertThat(MetricsAssertions.canonicalizeLabelValue(
                        "jvm_memory_pool_used_bytes", "pool", "CodeHeap 'non-nmethods'"))
                .isEqualTo("CodeHeap 'non-nmethods'");
    }

    @Test
    void azulPrimeCollectorAndMemoryPoolMetricsAreExcluded() {
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_GPGC_New_Cycles_LastGcInfo_GcThreadCount"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_GPGC_Old_Pauses_CollectionCount"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_GenPauseless_New_Gen_CollectionUsage_used"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_CodeCache_Usage_used"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_Native_Heap_Usage_used"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_NativeHeap_Valid"))
                .isTrue();
    }

    @Test
    void azulPrimeExclusionsIgnoreCase() {
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_gpgc_new_cycles_lastgcinfo_duration"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_genpauseless_old_gen_usage_used"))
                .isTrue();
    }

    @Test
    void standardJavaLangMetricsAreNotExcluded() {
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_Memory_HeapMemoryUsage_used"))
                .isFalse();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("java_lang_Threading_ThreadCount"))
                .isFalse();
    }

    @Test
    void uncanonicalizedValueIsReturnedAsIs() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("io_prometheus_jmx_foo", "key", "value"))
                .isEqualTo("value");
    }

    @Test
    void jvmRuntimeInfoVersionLabelIsWildcarded() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("jvm_runtime_info", "version", "21.0.9+10-LTS"))
                .isEqualTo("*");
        assertThat(MetricsAssertions.canonicalizeLabelValue("jvm_runtime_info", "vendor", "Eclipse Adoptium"))
                .isEqualTo("Eclipse Adoptium");
    }

    @Test
    void jvmRuntimeInfoIsWrittenAndMatchesOtherJvmVersions() {
        List<Metric> generated = List.of(metric(
                Metric.Type.GAUGE,
                "jvm_runtime_info",
                labels("runtime", "OpenJDK Runtime Environment", "vendor", "Eclipse Adoptium", "version", "21.0.8+9"),
                1.0));
        List<Metric> updated = List.of(metric(
                Metric.Type.GAUGE,
                "jvm_runtime_info",
                labels("runtime", "OpenJDK Runtime Environment", "vendor", "Eclipse Adoptium", "version", "21.0.9+10"),
                1.0));

        String lines = MetricsAssertions.formatMatchLines(generated);

        assertThat(lines)
                .isEqualTo("match GAUGE jvm_runtime_info{runtime=\"OpenJDK Runtime Environment\","
                        + "vendor=\"Eclipse Adoptium\",version=\"*\"} *\n");
        assertThatCode(() -> MetricsAssertions.verifyContent("test", lines, updated))
                .doesNotThrowAnyException();
    }

    @Test
    void collidingSuffixCanonicalizationKeepsLiteralLabelValues() {
        List<Metric> azulPrimePools = List.of(
                poolMetric("CodeCache"),
                poolMetric("GenPauseless New Gen"),
                poolMetric("GenPauseless Old Gen"),
                poolMetric("GenPauseless Perm Gen"));

        String lines = MetricsAssertions.formatMatchLines(azulPrimePools);

        assertThat(lines)
                .contains("{pool=\"GenPauseless New Gen\"}")
                .contains("{pool=\"GenPauseless Old Gen\"}")
                .contains("{pool=\"GenPauseless Perm Gen\"}")
                .doesNotContain("*Gen");
        assertThatCode(() -> MetricsAssertions.verifyContent("test", lines, azulPrimePools))
                .doesNotThrowAnyException();
    }

    @Test
    void nonCollidingSuffixCanonicalizationIsStillApplied() {
        List<Metric> g1Pools =
                List.of(poolMetric("G1 Eden Space"), poolMetric("G1 Old Gen"), poolMetric("G1 Survivor Space"));
        List<Metric> serialPools =
                List.of(poolMetric("Eden Space"), poolMetric("Tenured Gen"), poolMetric("Survivor Space"));

        String lines = MetricsAssertions.formatMatchLines(g1Pools);

        assertThat(lines)
                .contains("{pool=\"*Eden Space\"}")
                .contains("{pool=\"*Gen\"}")
                .contains("{pool=\"*Survivor Space\"}");
        assertThatCode(() -> MetricsAssertions.verifyContent("test", lines, serialPools))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectedRequestsValueIsWildcarded() {
        String lines = MetricsAssertions.formatMatchLines(
                List.of(metric(Metric.Type.COUNTER, "jmx_http_requests_rejected_total", labels(), 0.0)));

        assertThat(lines).isEqualTo("match COUNTER jmx_http_requests_rejected_total *\n");
        assertThatCode(() -> MetricsAssertions.verifyContent(
                        "test",
                        lines,
                        List.of(metric(Metric.Type.COUNTER, "jmx_http_requests_rejected_total", labels(), 3.0))))
                .doesNotThrowAnyException();
    }

    @Test
    void cracMetricsAreExcluded() {
        assertThat(MetricsAssertions.isRuntimeNameExcluded("jdk_management_crac_RestoreTime"))
                .isTrue();
        assertThat(MetricsAssertions.isRuntimeNameExcluded("jdk_crac_management_CRaC_RestoreTime"))
                .isTrue();
    }

    private static Metric poolMetric(String pool) {
        return metric(Metric.Type.GAUGE, "jvm_memory_pool_used_bytes", labels("pool", pool), 1024.0);
    }

    private static Metric metric(Metric.Type type, String name, TreeMap<String, String> labels, double value) {
        return new Metric(type, null, name, labels, value);
    }

    private static TreeMap<String, String> labels(String... keyValues) {
        TreeMap<String, String> labels = new TreeMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            labels.put(keyValues[i], keyValues[i + 1]);
        }
        return labels;
    }
}
