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

/** Class to implement MetricMapAssertion */
public class MetricMapAssertion implements MetricAssertion {

    private final Map<String, Collection<Metric>> metrics;
    private Metric.Type type;
    private String name;
    private String help;
    private TreeMap<String, String> labels;
    private Double value;

    /**
     * Constructor
     *
     * @param metrics metrics
     */
    public MetricMapAssertion(Map<String, Collection<Metric>> metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("metrics is null");
        }
        this.metrics = metrics;
    }

    /**
     * Method to set the type to match against
     *
     * @param type type
     * @return this MetricAssertion
     */
    public MetricMapAssertion ofType(Metric.Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type is null");
        }
        this.type = type;
        return this;
    }

    /**
     * Method to set the name to match against
     *
     * @param name name
     * @return this MetricAssertion
     */
    public MetricMapAssertion withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Method to set the help to match against
     *
     * @param help help
     * @return this MetricAssertion
     */
    public MetricMapAssertion withHelp(String help) {
        this.help = help;
        return this;
    }

    /**
     * Method to add a label to match against
     *
     * @param name name
     * @param value value
     * @return this MetricAssertion
     */
    public MetricMapAssertion withLabel(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(
                    format("Label name [%s] or value [%s] is null", name, value));
        }
        if (labels == null) {
            labels = new TreeMap<>();
        }
        labels.put(name, value);
        return this;
    }

    /**
     * Method to set the value to match against
     *
     * @param value value
     * @return this MetricAssertion
     */
    public MetricMapAssertion withValue(Double value) {
        this.value = value;
        return this;
    }

    /**
     * Method to assert the Metric is present
     *
     * @return this MetricAssertion
     */
    public MetricMapAssertion isPresent() {
        return isPresentWhen(true);
    }

    /**
     * Method to assert the Metric is present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    public MetricMapAssertion isPresentWhen(boolean condition) {
        Collection<Metric> metrics = this.metrics.get(name);

        if (condition) {
            if (metrics == null) {
                throw new AssertionFailedError(
                        format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not"
                                        + " present",
                                type, help, name, labels, value));
            }
        } else {
            if (metrics == null) {
                return this;
            }
        }

        Collection<Metric> subMetrics =
                metrics.stream()
                        .filter(metric -> type == null || metric.type().equals(type))
                        .filter(metric -> help == null || metric.help().equals(help))
                        .filter(
                                metric ->
                                        labels == null
                                                || metric.labels()
                                                        .entrySet()
                                                        .containsAll(labels.entrySet()))
                        .filter(metric -> value == null || metric.value() == value)
                        .collect(Collectors.toList());

        if (condition) {
            if (subMetrics.size() > 1) {
                throw new AssertionFailedError(
                        format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f]"
                                        + " matches multiple metrics",
                                type, help, name, labels, value));
            } else if (subMetrics.isEmpty()) {
                throw new AssertionFailedError(
                        format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not"
                                        + " present",
                                type, help, name, labels, value));
            }
        } else {
            if (!subMetrics.isEmpty()) {
                throw new AssertionFailedError(
                        format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is"
                                        + " present",
                                type, help, name, labels, value));
            }
        }

        return this;
    }

    /**
     * Method to assert the Metric is not present
     *
     * @return this MetricAssertion
     */
    public MetricMapAssertion isNotPresent() {
        return isPresentWhen(false);
    }

    /**
     * Method to assert the Metric is not present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    public MetricMapAssertion isNotPresentWhen(boolean condition) {
        return isPresentWhen(!condition);
    }

    /**
     * Method to create a MetricAssertion
     *
     * @param metrics the collection of metrics
     * @return a MetricAssertion
     */
    public static MetricMapAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
        return new MetricMapAssertion(metrics);
    }
}
