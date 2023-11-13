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

import io.prometheus.jmx.test.support.Mode;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import io.prometheus.jmx.test.support.metrics.DoubleValueMetricAssertion;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class MinimalTest extends AbstractTest implements Consumer<HttpResponse> {

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

        Collection<Metric> metrics = MetricsParser.parse(httpResponse);

        String buildInfoName =
                testArgument.mode() == Mode.JavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_httpserver";

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jmx_exporter_build_info")
                .label("name", buildInfoName)
                .value(1d)
                .isPresent();

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jmx_scrape_error")
                .value(0d)
                .isPresent();

        new DoubleValueMetricAssertion(metrics)
                .type("COUNTER")
                .name("jmx_config_reload_success_total")
                .value(0d)
                .isPresent();

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jvm_memory_used_bytes")
                .label("area", "nonheap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new DoubleValueMetricAssertion(metrics)
                .type("GAUGE")
                .name("jvm_memory_used_bytes")
                .label("area", "heap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        new DoubleValueMetricAssertion(metrics)
                .type("UNTYPED")
                .name("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .label("source", "/dev/sda1")
                .value(7.516192768E9d)
                .isPresent();

        new DoubleValueMetricAssertion(metrics)
                .type("UNTYPED")
                .name("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .label("source", "/dev/sda2")
                .value(0.8d)
                .isPresent();
    }
}
