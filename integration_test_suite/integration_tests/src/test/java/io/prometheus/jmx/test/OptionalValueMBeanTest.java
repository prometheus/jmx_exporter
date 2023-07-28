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

import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import java.util.Collection;
import org.antublue.test.engine.api.TestEngine;

public class OptionalValueMBeanTest extends BaseTest {

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequest(testState.httpClient()))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(
                        (ContentConsumer)
                                content -> {
                                    Collection<Metric> metrics = MetricsParser.parse(content);
                                    metrics.forEach(
                                            metric -> {
                                                if (metric.getName()
                                                        .equals(
                                                                "io_prometheus_jmx_optionalValue_Value")) {
                                                    assertThat(metric.getValue()).isEqualTo(345.0);
                                                }
                                            });
                                });
    }
}
