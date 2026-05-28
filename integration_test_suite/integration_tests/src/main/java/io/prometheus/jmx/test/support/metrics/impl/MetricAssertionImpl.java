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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.paramixel.api.exception.FailException;

/**
 * Asserts the presence or absence of metrics using fluent matchers for type, name, help,
 * labels, and value.
 *
 * <p>Internally normalizes both {@link Map} and {@link Collection} inputs to a name-keyed
 * map, providing O(1) lookup by metric name during assertion rather than scanning all
 * metrics on every call.
 */
public class MetricAssertionImpl implements MetricAssertion {

    private final Map<String, List<Metric>> metricsByName;
    private Metric.Type type;
    private String name;
    private String help;
    private TreeMap<String, String> labels;
    private Double value;

    /**
     * Creates a metric assertion over the specified metrics map.
     *
     * <p>The map entries are defensively copied; subsequent mutations to the input
     * collection values do not affect this assertion.
     *
     * @param metrics the map of metric names to their corresponding metric collections
     * @throws NullPointerException if metrics is {@code null}
     */
    public MetricAssertionImpl(Map<String, Collection<Metric>> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");
        Map<String, List<Metric>> mutableMap = new HashMap<>(metrics.size());
        for (Map.Entry<String, Collection<Metric>> entry : metrics.entrySet()) {
            mutableMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.metricsByName = mutableMap;
    }

    /**
     * Creates a metric assertion over the specified metrics collection, grouping them by
     * name internally.
     *
     * <p>Conversion to the name-keyed map is performed once at construction time, enabling
     * O(1) name lookups during subsequent assertions.
     *
     * @param metrics the collection of metrics to assert against
     * @throws NullPointerException if metrics is {@code null}
     */
    public MetricAssertionImpl(Collection<Metric> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");
        this.metricsByName = metrics.stream().collect(Collectors.groupingBy(Metric::name));
    }

    @Override
    public MetricAssertionImpl ofType(Metric.Type type) {
        Objects.requireNonNull(type, "type is null");
        this.type = type;
        return this;
    }

    @Override
    public MetricAssertionImpl withName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public MetricAssertionImpl withHelp(String help) {
        this.help = help;
        return this;
    }

    @Override
    public MetricAssertionImpl withLabel(String name, String value) {
        Objects.requireNonNull(name, "label name is null");
        Objects.requireNonNull(value, "label value is null");
        if (labels == null) {
            labels = new TreeMap<>();
        }
        labels.put(name, value);
        return this;
    }

    @Override
    public MetricAssertionImpl withValue(Double value) {
        this.value = value;
        return this;
    }

    @Override
    public MetricAssertionImpl isPresent() {
        return isPresentWhen(true);
    }

    @Override
    public MetricAssertionImpl isPresentWhen(boolean condition) {
        Collection<Metric> candidates = resolveCandidates();

        if (condition) {
            if (candidates == null || candidates.isEmpty()) {
                throw new FailException(format(
                        "Metric type [%s] name [%s] help [%s] labels [%s] value [%f] is not present",
                        type, name, help, labels, value));
            }
        } else {
            if (candidates == null || candidates.isEmpty()) {
                return this;
            }
        }

        List<Metric> matching = candidates.stream()
                .filter(metric -> type == null || metric.type().equals(type))
                .filter(metric -> help == null || metric.help().equals(help))
                .filter(metric -> labels == null || metric.labels().entrySet().containsAll(labels.entrySet()))
                .filter(metric -> value == null || metric.value() == value)
                .collect(Collectors.toList());

        if (condition) {
            if (matching.size() > 1) {
                throw new FailException(format(
                        "Metric type [%s] name [%s] help [%s] labels [%s] value [%f]" + " matches multiple metrics",
                        type, name, help, labels, value));
            } else if (matching.isEmpty()) {
                throw new FailException(format(
                        "Metric type [%s] name [%s] help [%s] labels [%s] value [%f] is not present",
                        type, name, help, labels, value));
            }
        } else {
            if (!matching.isEmpty()) {
                throw new FailException(format(
                        "Metric type [%s] name [%s] help [%s] labels [%s] value [%f] is present",
                        type, name, help, labels, value));
            }
        }

        return this;
    }

    @Override
    public MetricAssertionImpl isNotPresent() {
        return isPresentWhen(false);
    }

    @Override
    public MetricAssertionImpl isNotPresentWhen(boolean condition) {
        if (condition) {
            return isPresentWhen(false);
        }
        return this;
    }

    /**
     * Resolves the candidate metrics for the current filter criteria.
     *
     * <p>When a name filter is set, candidates are resolved via O(1) map lookup. When
     * no name filter is set, all metrics from all entries are flattened into a single
     * list.
     *
     * @return the candidate metrics, or {@code null} when a named lookup produces no
     *         results
     */
    private Collection<Metric> resolveCandidates() {
        if (name != null) {
            return metricsByName.get(name);
        }
        return metricsByName.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }
}
