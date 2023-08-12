/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.logger;

import java.util.logging.Level;

/** Class to implement a Logger */
public class Logger {

    private final java.util.logging.Logger LOGGER;

    private final boolean JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG =
            "true".equals(System.getenv("JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG"))
                    || "true".equals(System.getProperty("jmx.prometheus.exporter.developer.debug"));

    /**
     * Constructor
     *
     * @param clazz clazz
     */
    Logger(Class<?> clazz) {
        LOGGER = java.util.logging.Logger.getLogger(clazz.getName());
    }

    /**
     * Method to return whether a log level is enabled
     *
     * @param level level
     * @return true if the log level is enabled, else false
     */
    public boolean isLoggable(Level level) {
        return LOGGER.isLoggable(level);
    }

    /**
     * Method to log a message
     *
     * @param level level
     * @param message message
     * @param objects objects
     */
    public void log(Level level, String message, Object... objects) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, String.format(message, objects));
        }

        if (JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG) {
            System.out
                    .format("[%s] %s %s", level, LOGGER.getName(), String.format(message, objects))
                    .println();
        }
    }
}
