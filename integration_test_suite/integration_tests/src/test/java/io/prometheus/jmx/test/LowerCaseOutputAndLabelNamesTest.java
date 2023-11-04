/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import static io.prometheus.jmx.test.support.legacy.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.legacy.ContentConsumer;
import io.prometheus.jmx.test.support.legacy.HealthyRequestLegacy;
import io.prometheus.jmx.test.support.legacy.HealthyResponseLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsResponseLegacy;
import java.util.Collection;
import org.antublue.test.engine.api.TestEngine;

public class LowerCaseOutputAndLabelNamesTest extends BaseTest implements ContentConsumer {

    @TestEngine.Test
    public void testHealthy() {
        assertThatResponseForRequest(new HealthyRequestLegacy(testState.httpClient()))
                .isSuperset(HealthyResponseLegacy.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequestLegacy(testState.httpClient()))
                .isSuperset(MetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatResponseForRequest(new OpenMetricsRequestLegacy(testState.httpClient()))
                .isSuperset(OpenMetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatResponseForRequest(new PrometheusMetricsRequestLegacy(testState.httpClient()))
                .isSuperset(PrometheusMetricsResponseLegacy.RESULT_200)
                .dispatch(this);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metricCollection = MetricsParser.parse(content);

        /*
         * Assert that all metrics have lower case names and lower case label names
         */
        metricCollection.forEach(
                metric -> {
                    assertThat(metric.getName()).isEqualTo(metric.getName().toLowerCase());
                    metric.getLabels()
                            .forEach((key, value) -> assertThat(key).isEqualTo(key.toLowerCase()));
                });
    }
}
