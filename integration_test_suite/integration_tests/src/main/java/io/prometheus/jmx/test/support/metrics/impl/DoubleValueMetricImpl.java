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

package io.prometheus.jmx.test.support.metrics.impl;

import io.prometheus.jmx.test.support.metrics.DoubleValueMetric;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Class to implement a concrete DoubleValueMetric */
public class DoubleValueMetricImpl implements DoubleValueMetric {

    private final String type;
    private final String name;
    private final String help;
    private final TreeMap<String, String> labels;
    private final double value;

    /**
     * Constructor
     *
     * @param type type
     * @param help help
     * @param name name
     * @param labels labels
     * @param value value
     */
    public DoubleValueMetricImpl(
            String type, String help, String name, TreeMap<String, String> labels, double value) {
        this.type = type;
        this.help = help;
        this.name = name;
        this.labels = labels != null ? labels : new TreeMap<>();
        this.value = value;
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String help() {
        return help;
    }

    @Override
    public Map<String, String> labels() {
        return labels;
    }

    @Override
    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DoubleValueMetricImpl that = (DoubleValueMetricImpl) o;
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
