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

package io.prometheus.jmx.logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for creating Logger instances.
 *
 * <p>Creates and caches Logger instances for classes. The factory overrides the default
 * {@link SimpleFormatter} with {@link LoggerFormatter} for consistent log message formatting.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. Logger instances are cached in a
 * {@link ConcurrentHashMap}.
 */
public class LoggerFactory {

    public enum Backend {
        NATIVE,
        JUL
    }

    public static final String BACKEND_PROPERTY = "jmx.prometheus.exporter.logging.backend";
    public static final String BACKEND_ENVIRONMENT_VARIABLE = "JMX_PROMETHEUS_EXPORTER_LOGGING_BACKEND";

    /**
     * Cache for Logger instances.
     *
     * <p>Maps class objects to their corresponding Logger instances.
     */
    private static final ConcurrentMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    private static volatile Backend defaultBackend = Backend.JUL;

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private LoggerFactory() {
        // Intentionally empty
    }

    public static void setDefaultBackend(Backend backend) {
        defaultBackend = backend;
    }

    static boolean useJul() {
        String value = System.getProperty(BACKEND_PROPERTY);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(BACKEND_ENVIRONMENT_VARIABLE);
        }
        if (value == null || value.trim().isEmpty()) {
            return defaultBackend == Backend.JUL;
        }
        if ("jul".equalsIgnoreCase(value.trim())) {
            return true;
        }
        if ("native".equalsIgnoreCase(value.trim())) {
            return false;
        }
        throw new IllegalArgumentException("Invalid logging backend [" + value + "], expected [native] or [jul]");
    }

    /**
     * Returns a Logger instance for the specified class.
     *
     * <p>Logger instances are cached and reused for subsequent requests for the same class.
     *
     * @param clazz the class for which to create a logger, must not be {@code null}
     * @return a Logger instance for the specified class
     */
    public static Logger getLogger(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, Logger::new);
    }
}
