package io.prometheus.jmx;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class PrometheusRegistryUtils {

    private final PrometheusRegistry prometheusRegistry;

    public PrometheusRegistryUtils(PrometheusRegistry prometheusRegistry) {
        this.prometheusRegistry = prometheusRegistry;
    }

    public Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return getSampleValue(name, Labels.of(labelNames, labelValues));
    }

    public Double getSampleValue(String name, Labels labels) {
        Set<Label> labelSet = new LinkedHashSet<>();

        if (labels != null) {
            labelSet = labels.stream().collect(Collectors.toCollection(LinkedHashSet::new));
        }

        MetricSnapshots metricSnapshots = prometheusRegistry.scrape(); // s -> s.equals(name));

        List<MetricSnapshot> metricSnapshotList =
                metricSnapshots.stream().collect(Collectors.toList());

        for (MetricSnapshot metricSnapshot : metricSnapshotList) {
            // System.out.println("name [" + metricSnapshot.getMetadata().getName() + "]");
            if (name.equals(metricSnapshot.getMetadata().getName())) {
                // TODO
                // System.out.println("name            [" + metricSnapshot.getMetadata().getName() +
                // "]");
                // System.out.println("prometheus name [" +
                // metricSnapshot.getMetadata().getPrometheusName() + "]");

                List<? extends DataPointSnapshot> dataPointSnapshots =
                        metricSnapshot.getDataPoints();

                for (DataPointSnapshot dataPointSnapshot : dataPointSnapshots) {
                    Labels dataPointSnapshotLabels = dataPointSnapshot.getLabels();
                    if (dataPointSnapshotLabels.compareTo(labels) == 0) {
                        if (dataPointSnapshot instanceof GaugeSnapshot.GaugeDataPointSnapshot) {
                            return ((GaugeSnapshot.GaugeDataPointSnapshot) dataPointSnapshot)
                                    .getValue();
                        } else if (dataPointSnapshot
                                instanceof CounterSnapshot.CounterDataPointSnapshot) {
                            return ((CounterSnapshot.CounterDataPointSnapshot) dataPointSnapshot)
                                    .getValue();
                        } else if (dataPointSnapshot
                                instanceof UnknownSnapshot.UnknownDataPointSnapshot) {
                            return ((UnknownSnapshot.UnknownDataPointSnapshot) dataPointSnapshot)
                                    .getValue();
                        }
                    }
                }
            }
        }

        return null;
    }
}
