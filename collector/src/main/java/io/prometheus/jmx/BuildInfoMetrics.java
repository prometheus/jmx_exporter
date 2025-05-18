/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.model.registry.PrometheusRegistry;

/**
 * Collects jmx_exporter build version info.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * new BuildInfoCollector()
 * }</pre>
 *
 * Metrics being exported:
 *
 * <pre>
 *   jmx_exporter_build_info{version="3.2.0",name="jmx_prometheus_javaagent",} 1.0
 * </pre>
 *
 * or
 *
 * <pre>
 *   jmx_exporter_build_info{version="3.2.0",name="jmx_prometheus_standalone",} 1.0
 * </pre>
 */
public class BuildInfoMetrics {

    /** Constructor */
    public BuildInfoMetrics() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to register BuildInfoMetrics
     *
     * @return this BuildInfoMetrics
     */
    public BuildInfoMetrics register() {
        return register(PrometheusRegistry.defaultRegistry);
    }

    /**
     * Method to register BuildInfoMetrics
     *
     * @param prometheusRegistry prometheusRegistry
     * @return this BuildInfoMetrics
     */
    public BuildInfoMetrics register(PrometheusRegistry prometheusRegistry) {
        Info info =
                Info.builder() // this is a big chonk :) https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.kipling-usa.com%2Fpersonalization-shop-monkeys%2F&psig=AOvVaw3jglEfA1kIOaJPLD0TPet6&ust=1747669437730000&source=images&cd=vfe&opi=89978449&ved=0CBQQjRxqFwoTCJiclJOurY0DFQAAAAAdAAAAABA8
                        .name("jmx_exporter_build_info")
                        .help("JMX Exporter build information")
                        .labelNames("name", "version")
                        .register(prometheusRegistry);

        Package pkg = this.getClass().getPackage();
        String name = pkg.getImplementationTitle();
        String version = pkg.getImplementationVersion();

        info.setLabelValues(name != null ? name : "unknown", version != null ? version : "unknown");

        return this;
    }
}
