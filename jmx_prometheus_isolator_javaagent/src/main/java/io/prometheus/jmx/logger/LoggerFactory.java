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
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

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

    /**
     * The root logger name.
     *
     * <p>Empty string refers to the root logger in java.util.logging.
     */
    private static final String ROOT_LOGGER = "";

    /**
     * Cache for Logger instances.
     *
     * <p>Maps class objects to their corresponding Logger instances.
     */
    private static final ConcurrentMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    static {
        // Override the default formatter for the root logger if it is SimpleFormatter
        for (Handler handler : java.util.logging.Logger.getLogger(ROOT_LOGGER).getHandlers()) {
            Formatter formatter = handler.getFormatter();
            if (null != formatter && formatter.getClass().getName().endsWith(SimpleFormatter.class.getName())) {
                handler.setFormatter(new LoggerFormatter());
            }
        }
    }

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private LoggerFactory() {
        // INTENTIONALLY BLANK
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
