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

package io.prometheus.jmx.javaagent.benchmarks;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Helpers for writing temporary javaagent benchmark configuration files.
 */
final class AgentBenchmarkSupport {

    private AgentBenchmarkSupport() {
        // Intentionally empty
    }

    /**
     * Writes a temporary YAML configuration file that is deleted on JVM exit.
     *
     * @param lines the configuration lines
     * @return the temporary file
     * @throws Exception if the file cannot be written
     */
    static File writeConfig(String... lines) throws Exception {
        File file = File.createTempFile("javaagent-benchmark-", ".yaml");
        file.deleteOnExit();
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.println(line);
            }
        }
        return file;
    }

    /**
     * Returns a minimal HTTP-only configuration restricted to the benchmark MBean domain.
     *
     * @return the configuration lines
     */
    static String[] httpOnlyConfig() {
        return new String[] {"httpServer:", "  metrics:", "    path: /metrics"};
    }

    /**
     * Returns a JMX collector configuration restricted to the benchmark MBean domain.
     *
     * @param cache whether the matching rule is cached
     * @return the configuration lines
     */
    static String[] collectorConfig(boolean cache) {
        return new String[] {
            AgentBenchmarkMBeans.INCLUDE_OBJECT_NAMES.trim(),
            "rules:",
            "  - pattern: \".*\"",
            cache ? "    cache: true" : "    cache: false"
        };
    }
}
