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

package io.prometheus.jmx.test.support.util;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;

/**
 * Class to implement TestSupport
 */
public class TestSupport {

    private static final String BUILD_INFO_JAVAAGENT = "jmx_prometheus_javaagent";

    private static final String BUILD_INFO_STANDALONE = "jmx_prometheus_standalone";

    /**
     * Constructor
     */
    private TestSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get the build info name based on the JMX exporter mode
     *
     * @param jmxExporterMode jmxExporterMode
     * @return the build info name
     */
    public static String getBuildInfoName(JmxExporterMode jmxExporterMode) {
        return jmxExporterMode == JmxExporterMode.JavaAgent ? BUILD_INFO_JAVAAGENT : BUILD_INFO_STANDALONE;
    }
}
