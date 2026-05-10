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

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Represents a parsed Prometheus metric with type, help text, name, labels, and value.
 */
public class Metric {

    private final Type type;
    private final String name;
    private final String help;
    private final TreeMap<String, String> labels;
    private final double value;

    /**
     * Defines the supported Prometheus metric types.
     */
    public enum Type {

        /**
         * A gauge metric that can go up or down.
         */
        GAUGE,

        /**
         * A counter metric that only increases or resets to zero.
         */
        COUNTER,

        /**
         * An untyped metric with no defined semantics.
         */
        UNTYPED
    }

    /**
     * Creates a metric with the specified type, help text, name, labels, and value.
     *
     * @param type the metric type (gauge, counter, or untyped)
     * @param help the help text describing the metric
     * @param name the metric name
     * @param labels the label key-value pairs; if {@code null}, an empty map is used
     * @param value the metric value
     */
    public Metric(Type type, String help, String name, TreeMap<String, String> labels, double value) {
        this.type = type;
        this.help = help;
        this.name = name;
        this.labels = labels != null ? labels : new TreeMap<>();
        this.value = value;
    }

    /**
     * Returns the metric type.
     *
     * @return the metric type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the metric name.
     *
     * @return the metric name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the metric help text.
     *
     * @return the metric help text
     */
    public String help() {
        return help;
    }

    /**
     * Returns the metric labels as an immutable sorted map keyed by label name.
     *
     * @return the metric labels, never {@code null}
     */
    public Map<String, String> labels() {
        return labels;
    }

    /**
     * Returns the metric value.
     *
     * @return the metric value
     */
    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metric that = (Metric) o;
        return Double.compare(value, that.value) == 0
                && Objects.equals(type, that.type)
                && Objects.equals(help, that.help)
                && Objects.equals(name, that.name)
                && Objects.equals(labels, that.labels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, help, name, labels, value);
    }

    @Override
    public String toString() {
        return "type ["
                + type
                + "] name ["
                + name
                + "] help ["
                + help
                + "] labels ["
                + labels
                + "] value ["
                + value
                + "]";
    }
}
