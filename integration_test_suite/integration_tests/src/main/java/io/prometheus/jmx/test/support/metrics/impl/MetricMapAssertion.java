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

import io.prometheus.jmx.test.support.metrics.Metric;
import io.prometheus.jmx.test.support.metrics.MetricAssertion;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

/**
 * Asserts the presence or absence of metrics within a map keyed by metric name
 * using fluent matchers for type, name, help, labels, and value.
 */
public class MetricMapAssertion implements MetricAssertion {

    private final Map<String, Collection<Metric>> metrics;
    private Metric.Type type;
    private String name;
    private String help;
    private TreeMap<String, String> labels;
    private Double value;

    /**
     * Creates a metric map assertion over the specified metrics map.
     *
     * @param metrics the map of metric names to their corresponding metric collections
     * @throws IllegalArgumentException if metrics is {@code null}
     */
    public MetricMapAssertion(Map<String, Collection<Metric>> metrics) {
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
    public MetricMapAssertion ofType(Metric.Type type) {
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
    public MetricMapAssertion withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Restricts the match to metrics with the specified help text.
     *
     * @param help the metric help text to match against
     * @return this assertion for method chaining
     */
    public MetricMapAssertion withHelp(String help) {
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
    public MetricMapAssertion withLabel(String name, String value) {
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
    public MetricMapAssertion withValue(Double value) {
        this.value = value;
        return this;
    }

    /**
     * Asserts that exactly one metric matching the configured criteria is present.
     *
     * @return this assertion for method chaining
     * @throws AssertionFailedError if no matching metric is found or multiple match
     */
    public MetricMapAssertion isPresent() {
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
    public MetricMapAssertion isPresentWhen(boolean condition) {
        Collection<Metric> metrics = this.metrics.get(name);

        if (condition) {
            if (metrics == null) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not" + " present",
                        type, help, name, labels, value));
            }
        } else {
            if (metrics == null) {
                return this;
            }
        }

        Collection<Metric> subMetrics = metrics.stream()
                .filter(metric -> type == null || metric.type().equals(type))
                .filter(metric -> help == null || metric.help().equals(help))
                .filter(metric -> labels == null || metric.labels().entrySet().containsAll(labels.entrySet()))
                .filter(metric -> value == null || metric.value() == value)
                .collect(Collectors.toList());

        if (condition) {
            if (subMetrics.size() > 1) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f]" + " matches multiple metrics",
                        type, help, name, labels, value));
            } else if (subMetrics.isEmpty()) {
                throw new AssertionFailedError(format(
                        "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not" + " present",
                        type, help, name, labels, value));
            }
        } else {
            if (!subMetrics.isEmpty()) {
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
    public MetricMapAssertion isNotPresent() {
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
    public MetricMapAssertion isNotPresentWhen(boolean condition) {
        return isPresentWhen(!condition);
    }

    /**
     * Creates a metric map assertion over the specified metrics map.
     *
     * @param metrics the map of metric names to their corresponding metric collections
     * @return a new {@link MetricMapAssertion} instance
     */
    public static MetricMapAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
        return new MetricMapAssertion(metrics);
    }
}
