package io.prometheus.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Collects jmx_exporter build version info.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new BuildInfoCollector().register();
 * }
 * </pre>
 * Metrics being exported:
 * <pre>
 *   jmx_exporter_build_info{version="3.2.0",name="jmx_prometheus_httpserver",} 1.0
 * </pre>
 */
public class BuildInfoCollector extends Collector {
  public List<Collector.MetricFamilySamples> collect() {
    List<Collector.MetricFamilySamples> mfs = new ArrayList<Collector.MetricFamilySamples>();

    GaugeMetricFamily artifactInfo = new GaugeMetricFamily(
            "jmx_exporter_build_info",
            "A metric with a constant '1' value labeled with the version of the JMX exporter.",
            asList("version", "name"));

    Package pkg = this.getClass().getPackage();
    String version = pkg.getImplementationVersion();
    String name = pkg.getImplementationTitle();

    artifactInfo.addMetric(asList(
            version != null ? version : "unknown",
            name != null ? name : "unknown"
    ), 1L);
    mfs.add(artifactInfo);

    return mfs;
  }
}
