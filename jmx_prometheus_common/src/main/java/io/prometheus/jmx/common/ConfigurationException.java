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

package io.prometheus.jmx.common;

import java.util.function.Supplier;

/**
 * Runtime exception thrown when configuration parsing or validation fails.
 *
 * <p>This exception is used throughout the JMX exporter to report configuration errors such as
 * invalid YAML syntax, missing required fields, invalid values, or type mismatches.
 *
 * <p>This class is thread-safe.
 */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructs a configuration exception with the specified detail message.
     *
     * @param message the detail message describing the configuration error
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a configuration exception with the specified detail message and cause.
     *
     * @param message the detail message describing the configuration error
     * @param throwable the cause of this exception
     */
    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a supplier that produces a ConfigurationException with the specified message.
     *
     * <p>This is useful for lazy exception creation in functional methods like
     * {@link java.util.Optional#orElseThrow(java.util.function.Supplier)}.
     *
     * @param message the detail message for the exception
     * @return a supplier that creates a new ConfigurationException with the given message
     */
    public static Supplier<ConfigurationException> supplier(String message) {
        return () -> new ConfigurationException(message);
    }
}
