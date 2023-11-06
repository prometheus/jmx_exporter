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

package io.prometheus.jmx.test.support.metrics.protobuf;

import io.prometheus.jmx.test.support.metrics.protobuf.util.ProtobufMetricLabelsFilter;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

public class ProtobufUntypedMetricAssertion {

    private final Collection<Metrics.MetricFamily> metrics;
    private String name;
    private final TreeMap<String, String> labels;
    private Double value;

    public ProtobufUntypedMetricAssertion(Collection<Metrics.MetricFamily> metrics) {
        this.metrics = metrics;
        this.labels = new TreeMap<>();
    }

    public ProtobufUntypedMetricAssertion name(String name) {
        this.name = name;
        return this;
    }

    public ProtobufUntypedMetricAssertion label(String name, String value) {
        labels.put(name, value);
        return this;
    }

    public ProtobufUntypedMetricAssertion value(Double value) {
        this.value = value;
        return this;
    }

    public void isPresent() {
        List<Metrics.Metric> collection =
                metrics.stream()
                        .filter(metrics -> metrics.getType() == Metrics.MetricType.UNTYPED)
                        .filter(metrics -> name.equals(metrics.getName()))
                        .flatMap(new ProtobufMetricLabelsFilter(labels))
                        .filter(metric -> value == null || metric.getUntyped().getValue() == value)
                        .collect(Collectors.toList());

        if (collection.size() != 1) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] name [%s] labels [%s] value [%f] is not present",
                            Metrics.MetricType.UNTYPED, name, labels, value));
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
                            Metrics.MetricType.UNTYPED, name, labels, value));
        }
    }

    public void isNotPresent() {
        isPresent(false);
    }

    public void isNotPresent(boolean isNotPresent) {
        isPresent(!isNotPresent);
    }
}
