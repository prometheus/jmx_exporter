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

package io.prometheus.jmx.logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.LogManager;

/** Class to implement LoggerFactory */
public class LoggerFactory {

    /** Constant for java.util.logging.config.file */
    private static final String JAVA_UTIL_LOGGING_CONFIG_FILE = "java.util.logging.config.file";

    /** Constant for root logger */
    private static final String ROOT_LOGGER = "";

    /** Cache for Logger instances */
    private static final ConcurrentMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    static {
        initialize();
    }

    /** Constructor */
    private LoggerFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get a Logger
     *
     * @param clazz the class for which the logger is to be created
     * @return a Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, Logger::new);
    }

    /** Method to initialize the logger */
    public static void initialize() {
        String configFile = System.getProperty(JAVA_UTIL_LOGGING_CONFIG_FILE);

        try {
            if (configFile != null && !configFile.trim().isEmpty()) {
                try (InputStream fis = Files.newInputStream(Paths.get(configFile))) {
                    LogManager.getLogManager().readConfiguration(fis);
                    return;
                }
            }

            java.util.logging.Logger root = java.util.logging.Logger.getLogger(ROOT_LOGGER);
            for (java.util.logging.Handler handler : root.getHandlers()) {
                handler.setFormatter(new LoggerFormatter());
            }
        } catch (Throwable t) {
            System.err.printf(
                    "Failed to initialize logging using system property [%s]%n",
                    JAVA_UTIL_LOGGING_CONFIG_FILE);
            t.printStackTrace(System.err);
        }
    }
}
