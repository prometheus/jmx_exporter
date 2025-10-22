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

/** Class to implement ConfigurationException */
public class ConfigurationException extends RuntimeException {

    /**
     * Constructor
     *
     * @param message message
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message message
     * @param throwable throwable
     */
    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Method to create a ConfigurationException supplier
     *
     * @param message message
     * @return a ConfigurationException supplier
     */
    public static Supplier<ConfigurationException> supplier(String message) {
        return () -> new ConfigurationException(message);
    }
}
