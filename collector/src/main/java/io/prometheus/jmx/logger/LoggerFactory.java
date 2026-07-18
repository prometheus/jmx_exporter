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

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Factory for creating Logger instances.
 *
 * <p>Creates and caches Logger instances for classes. The selected backend is fixed when a logger
 * is first created. Backend selection itself does not initialize Java Util Logging (JUL).
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. Logger instances are cached in a {@link
 * ConcurrentHashMap}.
 */
public final class LoggerFactory {

    /** Supported logging backends. */
    public enum Backend {
        NATIVE,
        JUL
    }

    /** System property used to select the logging backend. */
    public static final String BACKEND_PROPERTY = "jmx.prometheus.exporter.logging.backend";

    /** Environment variable used to select the logging backend. */
    public static final String BACKEND_ENVIRONMENT_VARIABLE = "JMX_PROMETHEUS_EXPORTER_LOGGING_BACKEND";

    /** Maps class objects to their corresponding Logger instances. */
    private static final ConcurrentMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    /** Backend used when neither the system property nor environment variable is configured. */
    private static volatile Backend defaultBackend = Backend.JUL;

    /** Private constructor to prevent instantiation. */
    private LoggerFactory() {
        // Intentionally empty
    }

    /**
     * Sets the artifact default without overriding explicit user configuration. Must be called
     * before the first logger is requested.
     */
    public static void setDefaultBackend(Backend backend) {
        defaultBackend = backend;
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

    /**
     * Creates the configured backend for a logger name.
     *
     * @param loggerName logger name
     * @return configured logging backend
     */
    static LoggerBackend createBackend(String loggerName) {
        Backend backend = configuredBackend();
        if (backend == Backend.NATIVE) {
            return new NativeLoggerBackend(loggerName);
        }
        return JulBackendHolder.create(loggerName);
    }

    private static Backend configuredBackend() {
        String value = System.getProperty(BACKEND_PROPERTY);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(BACKEND_ENVIRONMENT_VARIABLE);
        }
        if (value == null || value.trim().isEmpty()) {
            return defaultBackend;
        }
        try {
            return Backend.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid logging backend [" + value + "], expected [native] or [jul]", e);
        }
    }

    /**
     * This holder is the only non-JUL class that mentions the JUL implementation, keeping its
     * initialization lazy when native logging is selected.
     */
    private static final class JulBackendHolder {
        private static LoggerBackend create(String loggerName) {
            return new JulLoggerBackend(loggerName);
        }
    }
}
