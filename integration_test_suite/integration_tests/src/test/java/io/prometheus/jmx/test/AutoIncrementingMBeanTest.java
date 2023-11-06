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

import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import org.antublue.test.engine.api.TestEngine;

public class AutoIncrementingMBeanTest extends AbstractTest {

    @TestEngine.Test
    public void testHealthy() {
        new HttpHealthyRequest()
                .send(testContext.httpClient())
                .accept(HttpResponseAssertions::assertHttpHealthyResponse);
    }

    // TODO fix
    /*
    @TestEngine.Test
    public void testMetrics() {
        double value1 = collect();
        double value2 = collect();
        double value3 = collect();

        assertThat(value1).isGreaterThan(0);

        assertThat(value2).isGreaterThan(value1);
        assertThat(value2).isEqualTo(value1 + 1);

        assertThat(value3).isGreaterThan(value2);
        assertThat(value3).isEqualTo(value2 + 1);
    }

    private double collect() {
        final AtomicDouble value = new AtomicDouble();

        HttpResponse httpResponse =
                new HttpPrometheusMetricsRequest().send(testContext.httpClient());

        assertHttpMetricsResponse(httpResponse);

        Collection<TextMetric> metrics = TextMetricsParser.parse(httpResponse);

        metrics.forEach(
                metric -> {
                    if (metric.getName().startsWith("io_prometheus_jmx_autoIncrementing")) {
                        assertThat(metric.getValue()).isGreaterThanOrEqualTo(1);
                        value.set(metric.getValue());
                    }
                });

        return value.doubleValue();
    }
    */
}
