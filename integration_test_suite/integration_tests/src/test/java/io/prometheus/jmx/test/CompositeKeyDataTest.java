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

import io.prometheus.jmx.test.support.http.HttpHealthyRequest;
import io.prometheus.jmx.test.support.http.HttpMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpOpenMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpPrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseAssertions;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufMetricsParser;
import io.prometheus.jmx.test.support.metrics.protobuf.ProtobufUntypedMetricAssertion;
import io.prometheus.jmx.test.support.metrics.text.TextMetric;
import io.prometheus.jmx.test.support.metrics.text.TextMetricsParser;
import io.prometheus.jmx.test.support.metrics.text.TextUntypedMetricAssertion;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.util.Collection;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class CompositeKeyDataTest extends AbstractTest implements Consumer<HttpResponse> {

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

        new TextUntypedMetricAssertion(metrics)
                .name("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .label("key_id", "1")
                .label("key_path", "/db/query1.xq")
                .isPresent();

        new TextUntypedMetricAssertion(metrics)
                .name("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .label("key_id", "2")
                .label("key_path", "/db/query2.xq")
                .isPresent();
    }

    private void assertProtobufFormatResponse(HttpResponse httpResponse) {
        Collection<Metrics.MetricFamily> metricFamilies = ProtobufMetricsParser.parse(httpResponse);

        new ProtobufUntypedMetricAssertion(metricFamilies)
                .name("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .label("key_id", "1")
                .label("key_path", "/db/query1.xq")
                .isPresent();

        new ProtobufUntypedMetricAssertion(metricFamilies)
                .name("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .label("key_id", "2")
                .label("key_path", "/db/query2.xq")
                .isPresent();
    }
}
