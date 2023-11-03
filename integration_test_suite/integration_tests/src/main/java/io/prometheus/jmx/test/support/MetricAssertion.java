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

package io.prometheus.jmx.test.support;

import io.prometheus.jmx.test.Metric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentest4j.AssertionFailedError;

/** Class to implement a MetricAssertion */
public class MetricAssertion {

    private final Collection<Metric> metrics;
    private String name;
    private final List<LabelTuple> labelTuples;
    private Double value;

    /**
     * Constructor
     *
     * @param metrics metrics
     */
    public MetricAssertion(Collection<Metric> metrics) {
        this.metrics = metrics;
        this.labelTuples = new ArrayList<>();
    }

    /**
     * Method to set the metric name
     *
     * @param name name
     * @return the return value
     */
    public MetricAssertion withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Method to add a metric label and value
     *
     * @param label label
     * @param value value
     * @return this
     */
    public MetricAssertion withLabel(String label, String value) {
        labelTuples.add(new LabelTuple(label, value));
        return this;
    }

    /**
     * Method to a add a metric Label
     *
     * @param label label
     * @return this;
     */
    public MetricAssertion withLabel(Label label) {
        labelTuples.add(new LabelTuple(label.name(), label.value()));
        return this;
    }

    /**
     * Method to set the metric value
     *
     * @param value value
     * @return this
     */
    public MetricAssertion withValue(double value) {
        this.value = value;
        return this;
    }

    /**
     * Method to test if a Metric exists in the Metric Collection
     *
     * @param expected expected
     * @return this
     */
    public MetricAssertion exists(boolean expected) {
        if (expected) {
            exists();
        } else {
            doesNotExist();
        }

        return this;
    }

    /**
     * Method to test if a Metric exists in the Metric Collection
     *
     * @return this
     */
    public MetricAssertion exists() {
        metrics.stream()
                .filter(metric -> metric.getName().equals(name))
                .filter(
                        metric -> {
                            if (labelTuples.size() == 0) {
                                return true;
                            }
                            List<LabelTuple> labelTuples = toLabelTupleList(metric);
                            return labelTuples.containsAll(this.labelTuples);
                        })
                .filter(
                        metric -> {
                            if (value != null) {
                                return metric.getValue() == value;
                            }
                            return true;
                        })
                .findFirst()
                .ifPresentOrElse(
                        metric -> {
                            /* DO NOTHING */
                        },
                        () -> {
                            String message;
                            if (labelTuples.size() > 0) {
                                message =
                                        String.format(
                                                "Metric [%s] with labels / values %s does not"
                                                        + " exist",
                                                name, toLabelTupleString(labelTuples));
                            } else {
                                message = String.format("Metric [%s] does not exist", name);
                            }
                            throw new AssertionFailedError(message);
                        });

        return this;
    }

    /**
     * Method to test if a Metric does not exist in the Metric Collection
     *
     * @return this
     */
    public MetricAssertion doesNotExist() {
        metrics.stream()
                .filter(metric -> metric.getName().equals(name))
                .filter(
                        metric -> {
                            if (labelTuples.size() == 0) {
                                return true;
                            }
                            List<LabelTuple> labelTuples = toLabelTupleList(metric);
                            return labelTuples.containsAll(this.labelTuples);
                        })
                .filter(
                        metric -> {
                            if (value != null) {
                                return metric.getValue() == value;
                            }
                            return true;
                        })
                .findFirst()
                .ifPresent(
                        metric -> {
                            String message;
                            if (labelTuples.size() > 0) {
                                message =
                                        String.format(
                                                "Metric [%s] with labels / values %s should not"
                                                        + " exist",
                                                name, toLabelTupleString(labelTuples));
                            } else {
                                message = String.format("Metric [%s] should not exist", name);
                            }
                            throw new AssertionFailedError(message);
                        });

        return this;
    }

    private static List<LabelTuple> toLabelTupleList(Metric metric) {
        List<LabelTuple> labelTuples = new ArrayList<>();
        for (Map.Entry<String, String> entry : metric.getLabels().entrySet()) {
            labelTuples.add(new LabelTuple(entry.getKey(), entry.getValue()));
        }
        return labelTuples;
    }

    private static String toLabelTupleString(List<LabelTuple> labelTuples) {
        StringBuilder stringBuilder = new StringBuilder();
        labelTuples.forEach(
                labelTuple -> {
                    stringBuilder.append(labelTuple);
                    stringBuilder.append(", ");
                });
        return stringBuilder.substring(0, stringBuilder.length() - 2);
    }

    private static class LabelTuple {

        private final String label;
        private final String value;

        public LabelTuple(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "[" + label + "] = [" + value + "]";
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LabelTuple that = (LabelTuple) o;
            return Objects.equals(label, that.label) && Objects.equals(value, that.value);
        }
    }
}
