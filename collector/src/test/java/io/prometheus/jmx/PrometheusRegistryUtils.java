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

package io.prometheus.jmx;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Class to get a sample value from a PrometheusRegistery */
@SuppressWarnings("unchecked")
public class PrometheusRegistryUtils {

    private final PrometheusRegistry prometheusRegistry;

    /**
     * Constructor
     *
     * @param prometheusRegistry prometheusRegistry
     */
    public PrometheusRegistryUtils(PrometheusRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }

    /**
     * Method to get a specific value from the PrometheusRegistry
     *
     * @param name name
     * @param labelNames labelNames
     * @param labelValues labelValues
     * @return the metric value, or null if it doesn't exist
     */
    public Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return getSampleValue(name, Labels.of(labelNames, labelValues));
    }

    /**
     * Method to get a specific value from the PrometheusRegistry
     *
     * @param name name
     * @param labels labels
     * @return the metric value, or null if it doesn't exist
     */
    public Double getSampleValue(String name, Labels labels) {
        List<Double> values = new ArrayList<>();

        prometheusRegistry.scrape(s -> s.equals(name)).stream()
                .filter(metricSnapshot -> metricSnapshot.getMetadata().getName().equals(name))
                .forEach(
                        metricSnapshot ->
                                metricSnapshot.getDataPoints().stream()
                                        .filter(
                                                (Predicate<DataPointSnapshot>)
                                                        dataPointSnapshot ->
                                                                dataPointSnapshot
                                                                                .getLabels()
                                                                                .compareTo(labels)
                                                                        == 0)
                                        .findFirst()
                                        .ifPresent(
                                                (Consumer<DataPointSnapshot>)
                                                        dataPointSnapshot -> {
                                                            values.add(
                                                                    getDataPointSnapshotValue(
                                                                            dataPointSnapshot));
                                                        }));

        if (!values.isEmpty()) {
            return values.get(0);
        } else {
            return null;
        }
    }

    private static Double getDataPointSnapshotValue(DataPointSnapshot dataPointSnapshot) {
        Double value = null;

        if (dataPointSnapshot instanceof GaugeSnapshot.GaugeDataPointSnapshot) {
            value = ((GaugeSnapshot.GaugeDataPointSnapshot) dataPointSnapshot).getValue();
        } else if (dataPointSnapshot instanceof CounterSnapshot.CounterDataPointSnapshot) {
            value = ((CounterSnapshot.CounterDataPointSnapshot) dataPointSnapshot).getValue();
        } else if (dataPointSnapshot instanceof UnknownSnapshot.UnknownDataPointSnapshot) {
            value = ((UnknownSnapshot.UnknownDataPointSnapshot) dataPointSnapshot).getValue();
        }
        // TODO add other DataPoint types

        return value;
    }
}
