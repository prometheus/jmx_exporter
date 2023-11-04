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

import static io.prometheus.jmx.test.support.ResponseAssertions.assertOk;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.*;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class IncludeObjectNamesTest extends BaseTest implements Consumer<Response> {

    @TestEngine.Test
    public void testHealthy() {
        new HealthyRequest(testState.httpClient())
                .execute()
                .accept(ResponseAssertions::assertHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        new MetricsRequest(testState.httpClient()).execute().accept(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        new OpenMetricsRequest(testState.httpClient()).execute().accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        new PrometheusMetricsRequest(testState.httpClient()).execute().accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        new PrometheusProtobufMetricsRequest(testState.httpClient()).execute().accept(this);
    }

    @Override
    public void accept(Response response) {
        assertOk(response);
        assertThat(response.headers()).isNotNull();
        assertThat(response.headers().get(Header.CONTENT_TYPE)).isNotNull();
        assertThat(response.body()).isNotNull();

        if (Objects.requireNonNull(response.headers().get(Header.CONTENT_TYPE))
                .contains(ContentType.PROTOBUF)) {
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
        Collection<Metric> metrics = TextResponseMetricsParser.parse(response);

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
                .forEach(
                        metric -> {
                            String name = metric.getName();
                            boolean match =
                                    name.startsWith("java_lang")
                                            || name.startsWith("io_prometheus_jmx");
                            assertThat(match).isTrue();
                        });
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
