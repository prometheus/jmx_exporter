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

/**
 * Defines the fluent assertion API for verifying metric presence, type, name, help, labels, and value.
 */
public interface MetricAssertion {

    /**
     * Restricts the match to metrics of the specified type.
     *
     * @param type the metric type to match against
     * @return this assertion for method chaining
     */
    MetricAssertion ofType(Metric.Type type);

    /**
     * Restricts the match to metrics with the specified name.
     *
     * @param name the metric name to match against
     * @return this assertion for method chaining
     */
    MetricAssertion withName(String name);

    /**
     * Restricts the match to metrics with the specified help text.
     *
     * @param help the metric help text to match against
     * @return this assertion for method chaining
     */
    MetricAssertion withHelp(String help);

    /**
     * Adds a label name-value pair that matching metrics must contain.
     *
     * @param name the label name
     * @param value the label value
     * @return this assertion for method chaining
     */
    MetricAssertion withLabel(String name, String value);

    /**
     * Restricts the match to metrics with the specified numeric value.
     *
     * @param value the metric value to match against
     * @return this assertion for method chaining
     */
    MetricAssertion withValue(Double value);

    /**
     * Asserts that exactly one metric matching the configured criteria is present.
     *
     * @return this assertion for method chaining
     * @throws org.opentest4j.AssertionFailedError if no matching metric is found or multiple match
     */
    MetricAssertion isPresent();

    /**
     * Asserts that exactly one matching metric is present when the condition is {@code true},
     * and that no matching metric is present when the condition is {@code false}.
     *
     * @param condition the condition controlling the presence expectation
     * @return this assertion for method chaining
     * @throws org.opentest4j.AssertionFailedError if the presence expectation is violated
     */
    MetricAssertion isPresentWhen(boolean condition);

    /**
     * Asserts that no metric matching the configured criteria is present.
     *
     * @return this assertion for method chaining
     * @throws org.opentest4j.AssertionFailedError if a matching metric is found
     */
    MetricAssertion isNotPresent();

    /**
     * Asserts that no matching metric is present when the condition is {@code true},
     * and that a matching metric may be present when the condition is {@code false}.
     *
     * @param condition the condition controlling the absence expectation
     * @return this assertion for method chaining
     * @throws org.opentest4j.AssertionFailedError if the absence expectation is violated
     */
    MetricAssertion isNotPresentWhen(boolean condition);

    /**
     * Creates a metric assertion over a flat collection of metrics.
     *
     * @param metrics the collection of metrics to assert against
     * @return a {@link MetricAssertion} for fluent matching
     */
    static MetricAssertion assertMetric(Collection<Metric> metrics) {
        return new MetricCollectionAssertion(metrics);
    }

    /**
     * Creates a metric assertion over a map of metric names to their metric collections.
     *
     * @param metrics the map of metric names to their corresponding metric collections
     * @return a {@link MetricAssertion} for fluent matching
     */
    static MetricAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
        return new MetricMapAssertion(metrics);
    }

    /**
     * Asserts that the HTTP response contains metrics with the expected content type,
     * status code 200, and a non-empty body.
     *
     * @param httpResponse the HTTP response to validate
     * @param metricsContentType the expected metrics content type
     * @throws AssertionError if the response status is not 200 or the content type does not match
     */
    static void assertMetricsContentType(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody body = httpResponse.body();
            if (body != null) {
                throw new AssertionError(
                        format("Expected statusCode [%d] but was [%d] body [%s]", 200, statusCode, body.string()));
            } else {
                throw new AssertionError(format("Expected statusCode [%d] but was [%d] no body", 200, statusCode));
            }
        }

        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0)).isEqualTo(metricsContentType.toString());
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }
}
