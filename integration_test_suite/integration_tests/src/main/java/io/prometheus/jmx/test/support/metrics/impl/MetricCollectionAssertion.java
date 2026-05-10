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

package io.prometheus.jmx.test.support.metrics.impl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseBody;
import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricAssertion;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

/**
 * Asserts the presence or absence of a metric within a flat collection using fluent matchers
 * for type, name, help, labels, and value.
 */
public class MetricCollectionAssertion implements MetricAssertion {

    private final Collection<Metric> metrics;
    private Metric.Type type;
    private String name;
    private String help;
    private TreeMap<String, String> labels;
    private Double value;

    /**
     * Creates a metric collection assertion over the specified metrics.
     *
     * @param metrics the collection of metrics to assert against
     * @throws IllegalArgumentException if metrics is {@code null}
     */
    public MetricCollectionAssertion(Collection<Metric> metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics is null");
        }

        this.metrics = metrics;
    }

    /**
     * Restricts the match to metrics of the specified type.
     *
     * @param type the metric type to match against
     * @return this assertion for method chaining
     * @throws IllegalArgumentException if type is {@code null}
     */
    public MetricCollectionAssertion ofType(Metric.Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type is null");
        }

        this.type = type;
        return this;
    }

    /**
     * Restricts the match to metrics with the specified name.
     *
     * @param name the metric name to match against
     * @return this assertion for method chaining
     */
    public MetricCollectionAssertion withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Restricts the match to metrics with the specified help text.
     *
     * @param help the metric help text to match against
     * @return this assertion for method chaining
     */
    public MetricCollectionAssertion withHelp(String help) {
        this.help = help;
        return this;
    }

    /**
     * Adds a label name-value pair that matching metrics must contain.
     *
     * @param name the label name
     * @param value the label value
     * @return this assertion for method chaining
     * @throws IllegalArgumentException if either the label name or value is {@code null}
     */
    public MetricCollectionAssertion withLabel(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(format("Label name [%s] or value [%s] is null", name, value));
        }

        if (labels == null) {
            labels = new TreeMap<>();
        }

        labels.put(name, value);
        return this;
    }

    /**
     * Restricts the match to metrics with the specified numeric value.
     *
     * @param value the metric value to match against
     * @return this assertion for method chaining
     */
    public MetricCollectionAssertion withValue(Double value) {
        this.value = value;
        return this;
    }

    /**
     * Asserts that exactly one metric matching the configured criteria is present.
     *
     * @return this assertion for method chaining
     * @throws AssertionFailedError if no matching metric is found or multiple match
     */
    public MetricCollectionAssertion isPresent() {
        return isPresentWhen(true);
    }

    /**
     * Asserts that exactly one matching metric is present when the condition is {@code true},
     * and that no matching metric is present when the condition is {@code false}.
     *
     * @param condition the condition controlling the presence expectation
     * @return this assertion for method chaining
     * @throws AssertionFailedError if the presence expectation is violated
     */
    public MetricCollectionAssertion isPresentWhen(boolean condition) {
        List<Metric> metrics = this.metrics.stream()
                .filter(metric -> type == null || metric.type().equals(type))
                .filter(metric -> name == null || metric.name().equals(name))
                .filter(metric -> help == null || metric.help().equals(help))
                .filter(metric -> labels == null || metric.labels().entrySet().containsAll(labels.entrySet()))
                .filter(metric -> value == null || metric.value() == value)
                .collect(Collectors.toList());

        if (condition) {
            if (metrics.size() > 1) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f]" + " matches multiple metrics",
                        type, help, name, labels, value));
            } else if (metrics.isEmpty()) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not" + " present",
                        type, help, name, labels, value));
            }
        } else {
            if (!metrics.isEmpty()) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is" + " present",
                        type, help, name, labels, value));
            }
        }

        return this;
    }

    /**
     * Asserts that no metric matching the configured criteria is present.
     *
     * @return this assertion for method chaining
     * @throws AssertionFailedError if a matching metric is found
     */
    public MetricCollectionAssertion isNotPresent() {
        return isPresentWhen(false);
    }

    /**
     * Asserts that no matching metric is present when the condition is {@code true},
     * and that a matching metric may be present when the condition is {@code false}.
     *
     * @param condition the condition controlling the absence expectation
     * @return this assertion for method chaining
     * @throws AssertionFailedError if the absence expectation is violated
     */
    public MetricCollectionAssertion isNotPresentWhen(boolean condition) {
        return isPresentWhen(!condition);
    }

    /**
     * Creates a metric collection assertion over a flat collection of metrics.
     *
     * @param metrics the collection of metrics to assert against
     * @return a new {@link MetricCollectionAssertion} instance
     */
    public static MetricCollectionAssertion assertMetric(Collection<Metric> metrics) {
        return new MetricCollectionAssertion(metrics);
    }

    /**
     * Creates a metric map assertion over a map of metric names to their metric collections.
     *
     * @param metrics the map of metric names to their corresponding metric collections
     * @return a new {@link MetricMapAssertion} instance
     */
    public static MetricMapAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
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
    public static void assertCommonMetricsResponse(HttpResponse httpResponse, MetricsContentType metricsContentType) {
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
        byte[] bodyBytes = httpResponse.body().bytes();
        assertThat(bodyBytes).isNotNull();
        assertThat(bodyBytes.length).isGreaterThan(0);
    }
}
