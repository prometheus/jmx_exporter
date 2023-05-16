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
import org.opentest4j.AssertionFailedError;

import java.util.Collection;
import java.util.Map;

/**
 * Class to implement a MetricAssertion
 */
public class MetricAssertion {

    private final Collection<Metric> metrics;
    private String name;
    private String label;
    private String labelValue;
    private Double value;

    /**
     * Constructor
     *
     * @param metrics metrics
     */
    public MetricAssertion(Collection<Metric> metrics) {
        this.metrics = metrics;
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
     * Method to set the metric label and value (only 1 pair is supported)
     *
     * @param label label
     * @param value value
     * @return the return value
     */
    public MetricAssertion withLabel(String label, String value) {
        this.label = label;
        this.labelValue = value;
        return this;
    }

    /**
     * Method to set the metric value
     *
     * @param value value
     * @return the return value
     */
    public MetricAssertion withValue(double value) {
        this.value = value;
        return this;
    }

    /**
     * Method to test if a Metric exists in the Metric Collection
     *
     * @param  expected
     * @return the return value
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
     * @return the return value
     */
    public MetricAssertion exists() {
        metrics
                .stream()
                .filter(metric -> metric.getName().equals(name))
                .filter(metric -> {
                    if (label == null) {
                        return true;
                    }
                    for (Map.Entry<String, String> entry : metric.getLabels().entrySet()) {
                        if (entry.getKey().equals(label) && entry.getValue().equals(labelValue)) {
                            return true;
                        }
                    }
                    return false;
                })
                .filter(metric -> {
                    if (value != null) {
                        return metric.getValue() == value;
                    }
                    return true;
                })
                .findFirst()
                .ifPresentOrElse(
                        metric -> { /* DO NOTHING */ },
                        () -> {
                            String message;
                            if (label != null) {
                                message =
                                        String.format(
                                                "Metric [%s] with label [%s] = [%s] does not exist",
                                                name,
                                                label,
                                                labelValue);
                            } else {
                                message =
                                        String.format("Metric [%s] does not exist", name);
                            }
                            throw new AssertionFailedError(message);
                        });

        return this;
    }

    /**
     * Method to test if a Metric does not exist in the Metric Collection
     *
     * @return the return value
     */
    public MetricAssertion doesNotExist() {
        metrics
                .stream()
                .filter(metric -> metric.getName().equals(name))
                .filter(metric -> {
                    if (label == null) {
                        return true;
                    }
                    for (Map.Entry<String, String> entry : metric.getLabels().entrySet()) {
                        if (entry.getKey().equals(label) && entry.getValue().equals(labelValue)) {
                            return true;
                        }
                    }
                    return false;
                })
                .filter(metric -> {
                    if (value != null) {
                        return metric.getValue() == value;
                    }
                    return true;
                })
                .findFirst()
                .ifPresent(metric -> {
                    String message;
                    if (label != null) {
                        message =
                                String.format(
                                        "Metric [%s] with label [%s] = [%s] should not exist",
                                        name,
                                        label,
                                        labelValue);
                    } else {
                        message =
                                String.format("Metric [%s] should not exist", name);
                    }
                    throw new AssertionFailedError(message);
                });

        return this;
    }
}
