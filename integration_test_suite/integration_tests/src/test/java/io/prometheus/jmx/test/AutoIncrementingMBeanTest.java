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

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpMetricsResponse;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import org.antublue.verifyica.api.ArgumentContext;
import org.antublue.verifyica.api.Verifyica;
import org.testcontainers.shaded.com.google.common.util.concurrent.AtomicDouble;

public class AutoIncrementingMBeanTest extends MinimalTest {

    @Verifyica.Test
    @Verifyica.Order(order = Integer.MAX_VALUE)
    public void testAutoIncrementingMBean(ArgumentContext argumentContext) {
        HttpClient httpClient =
                argumentContext
                        .getTestArgument(ExporterTestEnvironment.class)
                        .getPayload()
                        .getHttpClient();

        // Collect the auto incrementing MBean values
        double value1 = collect(httpClient);
        double value2 = collect(httpClient);
        double value3 = collect(httpClient);

        // Assert that each collection is the previous value + 1
        assertThat(value2).isGreaterThan(value1);
        assertThat(value2).isEqualTo(value1 + 1);
        assertThat(value3).isGreaterThan(value2);
        assertThat(value3).isEqualTo(value2 + 1);
    }

    /**
     * Method to collect a value from the auto incrementing MBean
     *
     * @return the auto incrementing MBean value
     */
    private double collect(HttpClient httpClient) {
        final AtomicDouble value = new AtomicDouble();

        HttpResponse httpResponse = new HttpPrometheusMetricsRequest().send(httpClient);

        assertHttpMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        metrics.stream()
                .filter(metric -> metric.name().startsWith("io_prometheus_jmx_autoIncrementing"))
                .map(Metric::value)
                .limit(1)
                .findFirst()
                .ifPresent(value::set);

        return value.doubleValue();
    }
}
