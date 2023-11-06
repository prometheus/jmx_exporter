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

package io.prometheus.jmx.test.support.metrics.text;

import io.prometheus.jmx.test.support.metrics.text.util.TextMetricLabelsFilter;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

public class TextCounterMetricAssertion {

    private final Collection<TextMetric> metrics;
    private String name;
    private final TreeMap<String, String> labels;
    private Double value;

    public TextCounterMetricAssertion(Collection<TextMetric> metrics) {
        this.metrics = metrics;
        this.labels = new TreeMap<>();
    }

    public TextCounterMetricAssertion name(String name) {
        this.name = name;
        return this;
    }

    public TextCounterMetricAssertion label(String name, String value) {
        labels.put(name, value);
        return this;
    }

    public TextCounterMetricAssertion value(Double value) {
        this.value = value;
        return this;
    }

    public void isPresent() {
        List<TextCounterMetric> metrics =
                this.metrics.stream()
                        .filter(metric -> TextCounterMetric.MetricType.COUNTER == metric.getType())
                        .filter(metric -> name.equals(metric.getName()))
                        .filter(new TextMetricLabelsFilter(labels))
                        .map(textMetric -> (TextCounterMetric) textMetric)
                        .filter(metric -> value == null || metric.getValue() == value)
                        .collect(Collectors.toList());

        if (metrics.size() != 1) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] name [%s] labels [%s] value [%f] is not present",
                            TextMetric.MetricType.COUNTER, name, labels, value));
        }
    }

    public void isPresent(boolean isPresent) {
        boolean found = false;

        try {
            isPresent();
            found = true;
        } catch (AssertionFailedError e) {
            // Expected
        }

        if (found != isPresent) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] name [%s] labels [%s] value [%f] is present",
                            TextMetric.MetricType.COUNTER, name, labels, value));
        }
    }

    public void isNotPresent() {
        isPresent(false);
    }

    public void isNotPresent(boolean isNotPresent) {
        isPresent(!isNotPresent);
    }
}
