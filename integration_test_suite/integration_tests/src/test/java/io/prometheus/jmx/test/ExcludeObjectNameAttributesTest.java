/*
 * Copyright (C) 2023-present The Prometheus jmx_exporter Authors
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

import static io.prometheus.jmx.test.support.Assertions.assertCommonMetricsResponse;
import static io.prometheus.jmx.test.support.Assertions.assertHealthyResponse;
import static org.assertj.core.api.Assertions.fail;

import io.prometheus.jmx.test.common.ExporterPath;
import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.common.ExporterTestEnvironmentFactory;
import io.prometheus.jmx.test.common.ExporterTestSupport;
import io.prometheus.jmx.test.common.MetricsType;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;
import org.verifyica.api.Verifyica;

public class ExcludeObjectNameAttributesTest {

    @Verifyica.ArgumentSupplier(parallelism = 4)
    public static Stream<ExporterTestEnvironment> arguments() {
        return ExporterTestEnvironmentFactory.createExporterTestEnvironments();
    }

    @Verifyica.Prepare
    public static void prepare(ClassContext classContext) {
        ExporterTestSupport.getOrCreateNetwork(classContext);
    }

    @Verifyica.BeforeAll
    public void beforeAll(ArgumentContext argumentContext) {
        Class<?> testClass = argumentContext.classContext().testClass();
        Network network = ExporterTestSupport.getOrCreateNetwork(argumentContext);
        ExporterTestSupport.initializeExporterTestEnvironment(argumentContext, network, testClass);
    }

    @Verifyica.Test
    @Verifyica.Order(1)
    public void testHealthy(ExporterTestEnvironment exporterTestEnvironment) throws IOException {
        String url = exporterTestEnvironment.getBaseUrl() + ExporterPath.HEALTHY;
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertHealthyResponse(httpResponse);
    }

    @Verifyica.Test
    public void testDefaultTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);
        HttpResponse httpResponse = HttpClient.sendRequest(url);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testOpenMetricsTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, HttpHeader.CONTENT_TYPE, MetricsType.OPEN_METRICS_TEXT_METRICS);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testPrometheusTextMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, HttpHeader.CONTENT_TYPE, MetricsType.PROMETHEUS_TEXT_METRICS);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.Test
    public void testPrometheusProtobufMetrics(ExporterTestEnvironment exporterTestEnvironment)
            throws IOException {
        String url = exporterTestEnvironment.getUrl(ExporterPath.METRICS);
        HttpResponse httpResponse =
                HttpClient.sendRequest(
                        url, HttpHeader.CONTENT_TYPE, MetricsType.PROMETHEUS_PROTOBUF_METRICS);

        assertMetricsResponse(exporterTestEnvironment, httpResponse);
    }

    @Verifyica.AfterAll
    public void afterAll(ArgumentContext argumentContext) throws Throwable {
        List<Trap> traps = new ArrayList<>();

        traps.add(
                new Trap(
                        () -> ExporterTestSupport.destroyExporterTestEnvironment(argumentContext)));
        traps.add(new Trap(() -> ExporterTestSupport.destroyNetwork(argumentContext)));

        Trap.assertEmpty(traps);
    }

    @Verifyica.Conclude
    public static void conclude(ClassContext classContext) throws Throwable {
        ExporterTestSupport.destroyNetwork(classContext);
    }

    private void assertMetricsResponse(
            ExporterTestEnvironment exporterTestEnvironment, HttpResponse httpResponse) {
        assertCommonMetricsResponse(httpResponse);

        Collection<Metric> metrics = MetricsParser.parseCollection(httpResponse);

        Set<String> excludeAttributeNameSet = new HashSet<>();
        excludeAttributeNameSet.add("_ClassPath");
        excludeAttributeNameSet.add("_SystemProperties");

        Set<String> excludeJavaLangMemoryAttributeSet = new HashSet<>();
        excludeJavaLangMemoryAttributeSet.add("NonHeapMemoryUsage");
        excludeJavaLangMemoryAttributeSet.add("Verbose");
        excludeJavaLangMemoryAttributeSet.add("ObjectPendingFinalizationCount");

        /*
         * Assert that we don't have any metrics that start with ...
         *
         * name = java_lang*
         * attribute = _ClassPath
         * attribute = __SystemProperties
         *
         * ... or...
         *
         * name = java_lang_Memory
         * attribute = _Verbose
         */
        metrics.forEach(
                metric -> {
                    String name = metric.name();
                    if (name.equals("java_lang_Memory")) {
                        for (String attributeName : excludeJavaLangMemoryAttributeSet) {
                            if (name.equals(attributeName)) {
                                fail("metric [" + metric + "] found");
                            }
                        }
                    } else {
                        for (String attributeName : excludeAttributeNameSet) {
                            if (name.contains(attributeName)) {
                                fail("metric [" + metric + "] found");
                            }
                        }
                    }
                });
    }
}
