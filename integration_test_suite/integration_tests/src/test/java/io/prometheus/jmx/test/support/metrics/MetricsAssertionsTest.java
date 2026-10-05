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
    void uncanonicalizedValueIsReturnedAsIs() {
        assertThat(MetricsAssertions.canonicalizeLabelValue("io_prometheus_jmx_foo", "key", "value"))
                .isEqualTo("value");
    }
}
