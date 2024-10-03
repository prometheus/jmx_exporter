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

package io.prometheus.jmx.test.http.ssl;

import static io.prometheus.jmx.test.support.http.HttpResponseAssertions.assertHttpMetricsResponse;
import static io.prometheus.jmx.test.support.metrics.MetricAssertion.assertMetric;

import io.prometheus.jmx.test.common.ExporterTestEnvironment;
import io.prometheus.jmx.test.common.PKCS12KeyStoreExporterTestEnvironmentFilter;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricsParser;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.Verifyica;

public class SSLWithPKCS12KeyStoreTest extends AbstractSSLTest
        implements BiConsumer<ExporterTestEnvironment, HttpResponse> {

    /**
     * Method to get the Stream of test environments
     *
     * @return the Stream of test environments
     */
    @Verifyica.ArgumentSupplier
    public static Stream<ExporterTestEnvironment> arguments() {
        // Filter Java versions that don't support the PKCS12 keystore
        // format or don't support the required TLS cipher suites
        return AbstractSSLTest.arguments()
                .filter(new PKCS12KeyStoreExporterTestEnvironmentFilter());
    }

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

        Map<String, Collection<Metric>> metrics = MetricsParser.parseMap(httpResponse);

        boolean isJmxExporterModeJavaAgent =
                exporterTestEnvironment.getJmxExporterMode() == JmxExporterMode.JavaAgent;

        String buildInfoName =
                isJmxExporterModeJavaAgent
                        ? "jmx_prometheus_javaagent"
                        : "jmx_prometheus_standalone";

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_exporter_build_info")
                .withLabel("name", buildInfoName)
                .withValue(1d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jmx_scrape_error")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.COUNTER)
                .withName("jmx_config_reload_success_total")
                .withValue(0d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "nonheap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.GAUGE)
                .withName("jvm_memory_used_bytes")
                .withLabel("area", "heap")
                .isPresentWhen(isJmxExporterModeJavaAgent);

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size")
                .withLabel("source", "/dev/sda1")
                .withValue(7.516192768E9d)
                .isPresent();

        assertMetric(metrics)
                .ofType(Metric.Type.UNTYPED)
                .withName("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent")
                .withLabel("source", "/dev/sda2")
                .withValue(0.8d)
                .isPresent();
    }
}
