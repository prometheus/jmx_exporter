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

package io.prometheus.jmx.test.rmi.ssl;

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpMetricsResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;

import io.prometheus.jmx.test.AbstractTest;
import io.prometheus.jmx.test.support.Mode;
import io.prometheus.jmx.test.support.TestArgument;
import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;

public class MinimalRMISSLTest extends AbstractTest implements Consumer<HttpResponse> {

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    protected static Stream<TestArgument> arguments() {
        // Filter the arguments..
        //
        // 1. only run the Standalone exporter
        // 2. filter out the GraalVM 1.8 JVM - exception is that SunJSSE is not found
        // 3. filter out all ibmjava* JVMs - exception is that SunJSSE is not found
        //
        return AbstractTest.arguments()
                .filter(testArgument -> testArgument.name().contains("Standalone"))
                .filter(
                        testArgument1 ->
                                !testArgument1.dockerImageName().contains("graalvm/jdk:java8"))
                .filter(testArgument1 -> !testArgument1.dockerImageName().contains("ibmjava"));
    }

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

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType("COUNTER")
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresent(testArgument.mode() == Mode.JavaAgent);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        assertMetric(metrics)
                .ofType("GAUGE")
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isNotPresent(testArgument.mode() == Mode.Standalone);

        assertMetric(metrics)
                .ofType("UNTYPED")
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType("UNTYPED")
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();
    }
}