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

/**
 * Enum representing the logging levels.
 *
 * <p>This enum is used to define the different levels of logging that can be used in the
 * application. The levels are ordered from the most verbose (TRACE) to the least verbose (ERROR).
 */
public enum Level {

    /**
     * Trace level logging. This is the most verbose level and is used for detailed debugging
     * information.
     */
    TRACE,

    /**
     * Info level logging. This level is used for informational messages that highlight the progress
     * of the application.
     */
    INFO,

    /**
     * Warn level logging. This level is used for potentially harmful situations that should be
     * looked at.
     */
    WARN,

    /**
     * Error level logging. This level is used for error events that might still allow the
     * application to continue running.
     */
    ERROR,
}
