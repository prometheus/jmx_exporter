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

import io.prometheus.jmx.test.support.ContentConsumer;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.HealthyResponse;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.MetricsResponse;
import io.prometheus.jmx.test.support.OpenMetricsRequest;
import io.prometheus.jmx.test.support.OpenMetricsResponse;
import io.prometheus.jmx.test.support.PrometheusMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusMetricsResponse;
import org.antublue.test.engine.api.TestEngine;

import java.util.Collection;

import static io.prometheus.jmx.test.support.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

public class LowerCaseOutputAndLabelNamesTest extends BaseTest implements ContentConsumer {

    @TestEngine.Test
    public void testHealthy() {
        assertThatResponseForRequest(new HealthyRequest(testState.httpClient()))
                .isSuperset(HealthyResponse.RESULT_200);
    }

    @TestEngine.Test
    public void testMetrics() {
        assertThatResponseForRequest(new MetricsRequest(testState.httpClient()))
                .isSuperset(MetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        assertThatResponseForRequest(new OpenMetricsRequest(testState.httpClient()))
                .isSuperset(OpenMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        assertThatResponseForRequest(new PrometheusMetricsRequest(testState.httpClient()))
                .isSuperset(PrometheusMetricsResponse.RESULT_200)
                .dispatch(this);
    }

    @Override
    public void accept(String content) {
        Collection<Metric> metricCollection = MetricsParser.parse(content);

        /*
         * Assert that all metrics have lower case names and lower case label names
         */
        metricCollection
                .forEach(metric -> {
                        assertThat(metric.getName()).isEqualTo(metric.getName().toLowerCase());
                        metric
                                .getLabels()
                                .forEach((key, value) -> assertThat(key).isEqualTo(key.toLowerCase()));
                    });
    }
}
