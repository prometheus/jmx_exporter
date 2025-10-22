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

package io.prometheus.jmx;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Class to get a sample value from a PrometheusRegistry */
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
        DataPointSnapshot dataPointSnapshot = findDataPointSnapshot(name, labels);
        return dataPointSnapshot != null ? getDataPointSnapshotValue(dataPointSnapshot) : null;
    }

    /**
     * Method to get the type of metric from the PrometheusRegistry
     *
     * @param name name
     * @param labelNames labelNames
     * @param labelValues labelValues
     * @return the metric type, or null if it doesn't exist
     */
    public String getSampleType(String name, String[] labelNames, String[] labelValues) {
        return getSampleType(name, Labels.of(labelNames, labelValues));
    }

    /**
     * Method to get the type of metric from the PrometheusRegistry
     *
     * @param name name
     * @param labels labels
     * @return the metric type, or null if it doesn't exist
     */
    public String getSampleType(String name, Labels labels) {
        DataPointSnapshot dataPointSnapshot = findDataPointSnapshot(name, labels);
        return dataPointSnapshot != null ? getDataPointSnapshotType(dataPointSnapshot) : null;
    }

    private DataPointSnapshot findDataPointSnapshot(String name, Labels labels) {
        List<DataPointSnapshot> dataPoints = new ArrayList<>();

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
                                        .ifPresent(dataPoints::add));

        return !dataPoints.isEmpty() ? dataPoints.get(0) : null;
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

    private static String getDataPointSnapshotType(DataPointSnapshot dataPointSnapshot) {
        if (dataPointSnapshot instanceof GaugeSnapshot.GaugeDataPointSnapshot) {
            return "GAUGE";
        } else if (dataPointSnapshot instanceof CounterSnapshot.CounterDataPointSnapshot) {
            return "COUNTER";
        } else if (dataPointSnapshot instanceof UnknownSnapshot.UnknownDataPointSnapshot) {
            return "UNKNOWN";
        }
        // TODO add other DataPoint types

        return null;
    }
}
