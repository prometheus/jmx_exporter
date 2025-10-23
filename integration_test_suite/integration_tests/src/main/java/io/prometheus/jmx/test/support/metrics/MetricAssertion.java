/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support.metrics;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseBody;
import io.prometheus.jmx.test.support.metrics.impl.MetricCollectionAssertion;
import io.prometheus.jmx.test.support.metrics.impl.MetricMapAssertion;
import java.util.Collection;
import java.util.Map;

/** Class to implement MetricAssertion */
public interface MetricAssertion {

    /**
     * Method to set the type to match against
     *
     * @param type type
     * @return this MetricAssertion
     */
    MetricAssertion ofType(Metric.Type type);

    /**
     * Method to set the name to match against
     *
     * @param name name
     * @return this MetricAssertion
     */
    MetricAssertion withName(String name);

    /**
     * Method to set the help to match against
     *
     * @param help help
     * @return this MetricAssertion
     */
    MetricAssertion withHelp(String help);

    /**
     * Method to add a label to match against
     *
     * @param name name
     * @param value value
     * @return this MetricAssertion
     */
    MetricAssertion withLabel(String name, String value);

    /**
     * Method to set the value to match against
     *
     * @param value value
     * @return this MetricAssertion
     */
    MetricAssertion withValue(Double value);

    /**
     * Method to assert the Metric is present
     *
     * @return this MetricAssertion
     */
    MetricAssertion isPresent();

    /**
     * Method to assert the Metric is present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    MetricAssertion isPresentWhen(boolean condition);

    /**
     * Method to assert the Metric is not present
     *
     * @return this MetricAssertion
     */
    MetricAssertion isNotPresent();

    /**
     * Method to assert the Metric is not present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    MetricAssertion isNotPresentWhen(boolean condition);

    /**
     * Method to create a MetricAssertion
     *
     * @param metrics the collection of metrics
     * @return a MetricAssertion
     */
    static MetricAssertion assertMetric(Collection<Metric> metrics) {
        return new MetricCollectionAssertion(metrics);
    }

    /**
     * Method to create a MetricAssertion
     *
     * @param metrics the collection of metrics
     * @return a MetricAssertion
     */
    static MetricAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
        return new MetricMapAssertion(metrics);
    }

    /**
     * Assert common metrics response
     *
     * @param httpResponse httpResponse
     * @param metricsContentType metricsContentType
     */
    static void assertMetricsContentType(
            HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody body = httpResponse.body();
            if (body != null) {
                throw new AssertionError(
                        format(
                                "Expected statusCode [%d] but was [%d] body [%s]",
                                200, statusCode, body.string()));
            } else {
                throw new AssertionError(
                        format("Expected statusCode [%d] but was [%d] no body", 200, statusCode));
            }
        }

        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0))
                .isEqualTo(metricsContentType.toString());
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }
}
