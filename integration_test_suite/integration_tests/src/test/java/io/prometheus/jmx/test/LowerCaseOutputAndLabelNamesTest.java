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

import io.prometheus.jmx.test.common.AbstractExporterTest;
import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.Locale;
import java.util.function.BiConsumer;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Verifyica;

public class LowerCaseOutputAndLabelNamesTest extends AbstractExporterTest
        implements BiConsumer<ExporterTestEnvironment, HttpResponse> {

    @Verifyica.Test
    public void testHealthy(ArgumentContext argumentContext) {
        super.testHealthy(argumentContext);
    }

    @Verifyica.Test
    public void testMetrics(ArgumentContext argumentContext) {
        super.testMetrics(argumentContext);
    }

    @Verifyica.Test
    public void testMetricsOpenMetricsFormat(ArgumentContext argumentContext) {
        super.testMetricsOpenMetricsFormat(argumentContext);
    }

    @Verifyica.Test
    public void testMetricsPrometheusFormat(ArgumentContext argumentContext) {
        super.testMetricsPrometheusFormat(argumentContext);
    }

    @Verifyica.Test
    public void testMetricsPrometheusProtobufFormat(ArgumentContext argumentContext) {
        super.testMetricsPrometheusProtobufFormat(argumentContext);
    }

    @Override
    public void accept(ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertHttpMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        /*
         * Assert that all metrics have lower case names and lower case label names
         */
        metrics.forEach(
                metric -> {
                    assertThat(metric.name()).isEqualTo(metric.name().toLowerCase(Locale.ENGLISH));
                    metric.labels()
                            .forEach(
                                    (key, value) ->
                                            assertThat(key)
                                                    .isEqualTo(key.toLowerCase(Locale.ENGLISH)));
                });
    }
}
