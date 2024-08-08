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

package io.prometheus.jmx.test.support.metrics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

/** Class to assert a MetricAssertion */
public class MetricAssertion {

    private final Collection<Metric> metrics;
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
    private MetricAssertion(Collection<Metric> metrics) {
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
    public MetricAssertion ofType(Metric.Type type) {
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
    public MetricAssertion withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Method to set the help to match against
     *
     * @param help help
     * @return this MetricAssertion
     */
    public MetricAssertion help(String help) {
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
    public MetricAssertion withLabel(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException(
                    String.format("Label name [%s] or value [%s] is null", name, value));
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
    public MetricAssertion withValue(Double value) {
        this.value = value;
        return this;
    }

    /**
     * Method to assert the Metric is present
     *
     * @return this MetricAssertion
     */
    public MetricAssertion isPresent() {
        return isPresentWhen(true);
    }

    /**
     * Method to assert the Metric is present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    public MetricAssertion isPresentWhen(boolean condition) {
        List<Metric> metrics =
                this.metrics.stream()
                        .filter(metric -> type == null || metric.type().equals(type))
                        .filter(metric -> name == null || metric.name().equals(name))
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
            if (metrics.size() > 1) {
                throw new AssertionFailedError(
                        String.format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f]"
                                        + " matches multiple metrics",
                                type, help, name, labels, value));
            } else if (metrics.isEmpty()) {
                throw new AssertionFailedError(
                        String.format(
                                "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not"
                                        + " present",
                                type, help, name, labels, value));
            }
        } else {
            if (!metrics.isEmpty()) {
                throw new AssertionFailedError(
                        String.format(
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
    public MetricAssertion isNotPresent() {
        return isPresentWhen(false);
    }

    /**
     * Method to assert the Metric is not present
     *
     * @param condition condition
     * @return this MetricAssertion
     */
    public MetricAssertion isNotPresentWhen(boolean condition) {
        return isPresentWhen(!condition);
    }

    /**
     * Method to create a MetricAssertion
     *
     * @param metrics the collection of metrics
     * @return a MetricAssertion
     */
    public static MetricAssertion assertMetric(Collection<Metric> metrics) {
        return new MetricAssertion(metrics);
    }

    /**
     * Method to create a MetricAssertion
     *
     * @param metrics the collection of metrics
     * @return a MetricAssertion
     */
    public static MapMetricAssertion assertMetric(Map<String, Collection<Metric>> metrics) {
        return new MapMetricAssertion(metrics);
    }
}
