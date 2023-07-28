/*
 * Copyright (C) 2018-2023 The Prometheus jmx_exporter Authors
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

import static java.util.Arrays.asList;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects jmx_exporter build version info.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * new BuildInfoCollector().register();
 * }</pre>
 *
 * Metrics being exported:
 *
 * <pre>
 *   jmx_exporter_build_info{version="3.2.0",name="jmx_prometheus_httpserver",} 1.0
 * </pre>
 */
public class BuildInfoCollector extends Collector {

    private final List<Collector.MetricFamilySamples> metricFamilySamples;

    /** Constructor */
    public BuildInfoCollector() {
        super();

        metricFamilySamples = new ArrayList<Collector.MetricFamilySamples>();

        GaugeMetricFamily artifactInfo =
                new GaugeMetricFamily(
                        "jmx_exporter_build_info",
                        "A metric with a constant '1' value labeled with the version of the JMX"
                                + " exporter.",
                        asList("version", "name"));

        Package pkg = this.getClass().getPackage();
        String version = pkg.getImplementationVersion();
        String name = pkg.getImplementationTitle();

        artifactInfo.addMetric(
                asList(version != null ? version : "unknown", name != null ? name : "unknown"), 1L);

        metricFamilySamples.add(artifactInfo);
    }

    /**
     * Method to get the List of MetricFamilySamples
     *
     * @return the return value
     */
    @Override
    public List<Collector.MetricFamilySamples> collect() {
        return metricFamilySamples;
    }
}
