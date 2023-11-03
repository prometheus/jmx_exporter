package io.prometheus.jmx;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.InfoSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;

public class BuildInfoMetricsTest {

    @Before
    public void setUp() {
        new BuildInfoMetrics().register();
    }

    @Test
    public void testBuildInfoMetrics() {
        Package pkg = BuildInfoMetrics.class.getPackage();
        String expectedName = pkg.getImplementationTitle();
        expectedName = expectedName != null ? expectedName : "unknown";
        String expectedVersion = pkg.getImplementationVersion();
        expectedVersion = expectedVersion != null ? expectedVersion : "unknown";

        List<MetricSnapshot> metricSnapshots =
                PrometheusRegistry.defaultRegistry
                        .scrape(s -> s.equals("jmx_exporter_build"))
                        .stream()
                        .collect(Collectors.toList());

        assertThat(metricSnapshots).isNotNull();
        assertThat(metricSnapshots.size()).isEqualTo(1);

        MetricSnapshot metricSnapshot = metricSnapshots.get(0);

        assertThat(metricSnapshot).isInstanceOf(InfoSnapshot.class);

        InfoSnapshot infoSnapshot = (InfoSnapshot) metricSnapshot;
        List<InfoSnapshot.InfoDataPointSnapshot> infoDataPointSnapshots =
                infoSnapshot.getDataPoints();

        assertThat(infoDataPointSnapshots).isNotNull();
        assertThat(infoDataPointSnapshots.size()).isEqualTo(1);

        InfoSnapshot.InfoDataPointSnapshot infoDataPointSnapshot = infoDataPointSnapshots.get(0);
        Labels labels = infoDataPointSnapshot.getLabels();
        String actualName = labels.get("name");
        String actualVersion = labels.get("version");

        assertThat(actualName).isEqualTo(expectedName);
        assertThat(actualVersion).isEqualTo(expectedVersion);
    }
}
