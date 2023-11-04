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

import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.ResponseAssertions;
import io.prometheus.jmx.test.support.legacy.ContentConsumer;
import io.prometheus.jmx.test.support.legacy.MetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsResponseLegacy;
import java.util.Collection;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.shaded.com.google.common.util.concurrent.AtomicDouble;

@TestEngine.Disabled
public class AutoIncrementingMBeanTest extends BaseTest {

    @TestEngine.Test
    public void testHealthy() {
        new HealthyRequest(testState.httpClient())
                .execute()
                .accept(ResponseAssertions::assertHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        AtomicDouble value1 = new AtomicDouble();
        AtomicDouble value2 = new AtomicDouble();
        AtomicDouble value3 = new AtomicDouble();

        // TODO refactor
        assertThatResponseForRequest(new MetricsRequestLegacy(testState.httpClient()))
                .isSuperset(MetricsResponseLegacy.RESULT_200)
                .dispatch(
                        (ContentConsumer)
                                content -> {
                                    Collection<Metric> metrics =
                                            TextResponseMetricsParser.parse(content);
                                    metrics.forEach(
                                            metric -> {
                                                if (metric.getName()
                                                        .startsWith(
                                                                "io_prometheus_jmx_autoIncrementing")) {
                                                    assertThat(metric.getValue())
                                                            .isGreaterThanOrEqualTo(1);
                                                    value1.set(metric.getValue());
                                                }
                                            });
                                });

        assertThatResponseForRequest(new MetricsRequestLegacy(testState.httpClient()))
                .isSuperset(MetricsResponseLegacy.RESULT_200)
                .dispatch(
                        (ContentConsumer)
                                content -> {
                                    Collection<Metric> metrics =
                                            TextResponseMetricsParser.parse(content);
                                    metrics.forEach(
                                            metric -> {
                                                if (metric.getName()
                                                        .startsWith(
                                                                "io_prometheus_jmx_autoIncrementing")) {
                                                    value2.set(metric.getValue());
                                                }
                                            });
                                });

        assertThatResponseForRequest(new MetricsRequestLegacy(testState.httpClient()))
                .isSuperset(MetricsResponseLegacy.RESULT_200)
                .dispatch(
                        (ContentConsumer)
                                content -> {
                                    Collection<Metric> metrics =
                                            TextResponseMetricsParser.parse(content);
                                    metrics.forEach(
                                            metric -> {
                                                if (metric.getName()
                                                        .startsWith(
                                                                "io_prometheus_jmx_autoIncrementing")) {
                                                    value3.set(metric.getValue());
                                                }
                                            });
                                });

        // Use value1 as a baseline value
        assertThat(value2.get()).isEqualTo(value1.get() + 1);
        assertThat(value3.get()).isEqualTo(value2.get() + 1);
    }
}
