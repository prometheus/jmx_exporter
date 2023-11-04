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

import static io.prometheus.jmx.test.support.MetricsAssertions.assertThatMetricIn;
import static io.prometheus.jmx.test.support.ResponseAssertions.assertOk;
import static io.prometheus.jmx.test.support.legacy.RequestResponseAssertions.assertThatResponseForRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.ContentType;
import io.prometheus.jmx.test.support.Header;
import io.prometheus.jmx.test.support.HealthyRequest;
import io.prometheus.jmx.test.support.MetricsRequest;
import io.prometheus.jmx.test.support.OpenMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusMetricsRequest;
import io.prometheus.jmx.test.support.PrometheusProtobufMetricsRequest;
import io.prometheus.jmx.test.support.Response;
import io.prometheus.jmx.test.support.ResponseAssertions;
import io.prometheus.jmx.test.support.legacy.ContentConsumer;
import io.prometheus.jmx.test.support.legacy.HealthyRequestLegacy;
import io.prometheus.jmx.test.support.legacy.HealthyResponseLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.MetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.OpenMetricsResponseLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsRequestLegacy;
import io.prometheus.jmx.test.support.legacy.PrometheusMetricsResponseLegacy;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;

import org.antublue.test.engine.api.TestEngine;

public class CompositeKeyDataTest extends BaseTest implements Consumer<Response> {

    @TestEngine.Test
    public void testHealthy() {
        new HealthyRequest(testState.httpClient())
                .execute()
                .accept(ResponseAssertions::assertHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        new MetricsRequest(testState.httpClient())
                .execute()
                .accept(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        new OpenMetricsRequest(testState.httpClient())
                .execute()
                .accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        new PrometheusMetricsRequest(testState.httpClient())
                .execute()
                .accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        new PrometheusProtobufMetricsRequest(testState.httpClient())
                .execute()
                .accept(this);
    }

    @Override
    public void accept(Response response) {
        assertOk(response);
        assertThat(response.headers()).isNotNull();
        assertThat(response.headers().get(Header.CONTENT_TYPE)).isNotNull();
        assertThat(response.body()).isNotNull();

        if (Objects.requireNonNull(response.headers().get(Header.CONTENT_TYPE)).contains(ContentType.PROTOBUF)) {
            assertProtobufResponse(response);
        } else {
            assertTextResponse(response);
        }
    }

    /**
     * Method to assert Prometheus and OpenMetrics text formats
     *
     * @param response response
     */
    private void assertTextResponse(Response response) {
        Collection<Metric> metrics = MetricsParser.parse(response.string());

        assertThatMetricIn(metrics)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "1")
                .withLabel("key_path", "/db/query1.xq")
                .exists();

        assertThatMetricIn(metrics)
                .withName("org_exist_management_exist_ProcessReport_RunningQueries_id")
                .withLabel("key_id", "2")
                .withLabel("key_path", "/db/query2.xq")
                .exists();
    }

    /**
     * Method to assert Prometheus Protobuf format
     *
     * @param response response
     */
    private void assertProtobufResponse(Response response) {
        System.out.println("TODO assertProtobufResponse()");
    }
}
