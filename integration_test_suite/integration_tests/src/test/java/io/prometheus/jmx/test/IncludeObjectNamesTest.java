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

import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufMetricsParser;
import io.prometheus.jmx.test.support.metrics.text.TextMetric;
import io.prometheus.jmx.test.support.metrics.text.TextMetricsParser;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.util.Collection;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class IncludeObjectNamesTest extends AbstractTest implements Consumer<HttpResponse> {

    @TestEngine.Test
    public void testHealthy() {
        new HttpHealthyRequest()
                .send(testContext.httpClient())
                .accept(HttpResponseAssertions::assertHttpHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        new HttpMetricsRequest().send(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        new HttpOpenMetricsRequest().send(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        new HttpPrometheusMetricsRequest().send(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        new HttpPrometheusProtobufMetricsRequest().send(testContext.httpClient()).accept(this);
    }

    @Override
    public void accept(HttpResponse httpResponse) {
        assertHttpMetricsResponse(httpResponse);

        if (isProtoBufFormat(httpResponse)) {
            assertProtobufFormatResponse(httpResponse);
        } else {
            assertTextFormatResponse(httpResponse);
        }
    }

    private void assertTextFormatResponse(HttpResponse httpResponse) {
        Collection<TextMetric> metrics = TextMetricsParser.parse(httpResponse);

        /*
         * We have to filter metrics that start with ...
         *
         * jmx_exporter
         * jmx_config
         * jmx_scrape
         * jvm_
         * process_
         *
         * ... because they are registered directly and are not MBeans
         */
        metrics.stream()
                .filter(metric -> !metric.getName().toLowerCase().startsWith("jmx_exporter"))
                .filter(metric -> !metric.getName().toLowerCase().startsWith("jmx_config"))
                .filter(metric -> !metric.getName().toLowerCase().startsWith("jmx_scrape"))
                .filter(metric -> !metric.getName().toLowerCase().startsWith("jvm_"))
                .filter(metric -> !metric.getName().toLowerCase().startsWith("process_"))
                .filter(
                        metric ->
                                !metric.getName()
                                        .toLowerCase()
                                        .startsWith("service_time_seconds_total"))
                .filter(metric -> !metric.getName().toLowerCase().startsWith("temperature_celsius"))
                .forEach(
                        metric -> {
                            String name = metric.getName();
                            boolean match =
                                    name.startsWith("java_lang")
                                            || name.startsWith("io_prometheus_jmx");
                            assertThat(match).isTrue();
                        });
    }

    private void assertProtobufFormatResponse(HttpResponse httpResponse) {
        Collection<Metrics.MetricFamily> metricFamilies = ProtobufMetricsParser.parse(httpResponse);

        /*
         * We have to filter metrics that start with ...
         *
         * jmx_exporter
         * jmx_config
         * jmx_scrape
         * jvm_
         * process_
         *
         * ... because they are registered directly and are not MBeans
         */
        metricFamilies.stream()
                .filter(
                        metricFamily ->
                                !metricFamily.getName().toLowerCase().startsWith("jmx_exporter"))
                .filter(
                        metricFamily ->
                                !metricFamily.getName().toLowerCase().startsWith("jmx_config"))
                .filter(
                        metricFamily ->
                                !metricFamily.getName().toLowerCase().startsWith("jmx_scrape"))
                .filter(metricFamily -> !metricFamily.getName().toLowerCase().startsWith("jvm_"))
                .filter(
                        metricFamily ->
                                !metricFamily.getName().toLowerCase().startsWith("process_"))
                .filter(
                        metricFamily ->
                                !metricFamily
                                        .getName()
                                        .toLowerCase()
                                        .startsWith("service_time_seconds_total"))
                .filter(
                        metricFamily ->
                                !metricFamily
                                        .getName()
                                        .toLowerCase()
                                        .startsWith("temperature_celsius"))
                .forEach(
                        metric -> {
                            String name = metric.getName();
                            boolean match =
                                    name.startsWith("java_lang")
                                            || name.startsWith("io_prometheus_jmx");
                            assertThat(match).isTrue();
                        });
    }
}
