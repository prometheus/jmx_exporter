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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

/** Class to assert a DoubleValueMetric */
public class MetricAssertion {

    private static final Set<String> VALID_TYPES = new HashSet<>();

    static {
        VALID_TYPES.add("COUNTER");
        VALID_TYPES.add("GAUGE");
        VALID_TYPES.add("UNTYPED");
    }

    private final Collection<Metric> metrics;
    private String type;
    private String name;
    private String help;
    private TreeMap<String, String> labels;
    private Double value;

    /**
     * Constructor
     *
     * @param metrics metrics
     */
    public MetricAssertion(Collection<Metric> metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("Collection<Metrics> is null");
        }
        this.metrics = metrics;
    }

    /**
     * Method to set the type to match against
     *
     * @param type type
     * @return this DoubleValueMetricAssertion
     */
    public MetricAssertion type(String type) {
        if (type == null || !VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException(String.format("Type [%s] is null or invalid", type));
        }
        this.type = type;
        return this;
    }

    /**
     * Method to set the name to match against
     *
     * @param name name
     * @return this DoubleValueMetricAssertion
     */
    public MetricAssertion name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Method to set the help to match against
     *
     * @param help help
     * @return this DoubleValueMetricAssertion
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
     * @return this DoubleValueMetricAssertion
     */
    public MetricAssertion label(String name, String value) {
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
     * @return this DoubleValueMetricAssertion
     */
    public MetricAssertion value(Double value) {
        this.value = value;
        return this;
    }

    /** Method to assert the Metric is present */
    public MetricAssertion isPresent() {
        return isPresent(true);
    }

    /**
     * Method to assert the Metric is present
     *
     * @param isPresent isPresent
     */
    public MetricAssertion isPresent(boolean isPresent) {
        List<Metric> metrics =
                this.metrics.stream()
                        .filter(metric -> type == null || metric.type().equals(type))
                        .filter(metric -> name == null || metric.name().equals(name))
                        .filter(metric -> help == null || metric.help().equals(help))
                        .filter(
                                metric ->
                                        labels == null
                                                || new LabelsSubsetFilter(labels).test(metric))
                        .filter(metric -> value == null || metric.value() == value)
                        .collect(Collectors.toList());

        if (isPresent && metrics.size() != 1) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is not"
                                    + " present or matches multiple metrics",
                            type, help, name, labels, value));
        } else if (!isPresent && !metrics.isEmpty()) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] help [%s] name [%s] labels [%s] value [%f] is"
                                    + " present or matches multiple metrics",
                            type, help, name, labels, value));
        }

        return this;
    }

    /** Method to assert the Metric is not present */
    public MetricAssertion isNotPresent() {
        return isPresent(false);
    }

    /**
     * Method to assert the Metric is not present
     *
     * @param isNotPresent isNotPresent
     */
    public MetricAssertion isNotPresent(boolean isNotPresent) {
        return isPresent(!isNotPresent);
    }
}
