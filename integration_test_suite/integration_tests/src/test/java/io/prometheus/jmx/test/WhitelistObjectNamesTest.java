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

import static io.prometheus.jmx.test.support.ResponseAssertions.assertHasBody;
import static io.prometheus.jmx.test.support.ResponseAssertions.assertHasHeader;
import static io.prometheus.jmx.test.support.ResponseAssertions.assertHasHeaders;
import static io.prometheus.jmx.test.support.ResponseAssertions.assertOk;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.*;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import org.antublue.test.engine.api.TestEngine;

public class WhitelistObjectNamesTest extends BaseTest implements Consumer<Response> {

    @TestEngine.Test
    public void testHealthy() {
        new HealthyRequest()
                .exchange(testContext.httpClient())
                .accept(ResponseAssertions::assertHealthyResponse);
    }

    @TestEngine.Test
    public void testMetrics() {
        new MetricsRequest().exchange(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsOpenMetricsFormat() {
        new OpenMetricsRequest().exchange(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusFormat() {
        new PrometheusMetricsRequest().exchange(testContext.httpClient()).accept(this);
    }

    @TestEngine.Test
    public void testMetricsPrometheusProtobufFormat() {
        new PrometheusProtobufMetricsRequest().exchange(testContext.httpClient()).accept(this);
    }

    @Override
    public void accept(Response response) {
        assertOk(response);
        assertHasHeaders(response);
        assertHasHeader(response, Header.CONTENT_TYPE);
        assertHasBody(response);

        Collection<Metric> metrics = MetricsParser.parse(response);

        if (Objects.requireNonNull(response.headers().get(Header.CONTENT_TYPE))
                .contains(ContentType.PROTOBUF)) {
            System.out.println("TODO Protobuf support");
            return;
        }

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
}
