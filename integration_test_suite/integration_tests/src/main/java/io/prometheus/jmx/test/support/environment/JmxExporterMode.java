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

package io.prometheus.jmx.test.support.environment;

/**
 * Defines the two operational modes of the JMX exporter: Java Agent and Standalone.
 */
public enum JmxExporterMode {

    /**
     * The Java Agent mode, where the exporter runs as a JVM agent using
     * the {@code jmx_prometheus_javaagent} build artifact.
     */
    JavaAgent("jmx_prometheus_javaagent"),

    /**
     * The Standalone mode, where the exporter runs as a separate process using
     * the {@code jmx_prometheus_standalone} build artifact.
     */
    Standalone("jmx_prometheus_standalone");

    private final String buildInfoName;

    JmxExporterMode(String buildInfoName) {
        this.buildInfoName = buildInfoName;
    }

    /**
     * Returns the build artifact name associated with this exporter mode.
     *
     * @return the build artifact name (e.g., {@code jmx_prometheus_javaagent})
     */
    public String getBuildInfoName() {
        return buildInfoName;
    }
}
