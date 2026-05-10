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
 * Defines HTTP endpoint path constants for the JMX exporter health and metrics endpoints.
 */
public class JmxExporterPath {

    /**
     * The health endpoint path, used to verify the exporter is responsive.
     */
    public static final String HEALTHY = "/-/healthy";

    /**
     * The default metrics endpoint path.
     */
    public static final String METRICS = "/metrics";

    /**
     * The custom metrics endpoint path, as configured in the YAML configuration file.
     */
    public static final String CUSTOM_METRICS = "/custom/metrics";

    /**
     * Private constructor to prevent instantiation.
     */
    private JmxExporterPath() {
        // INTENTIONALLY BLANK
    }
}
