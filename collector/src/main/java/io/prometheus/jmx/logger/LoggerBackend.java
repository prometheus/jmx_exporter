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

/** Internal logging backend used by the Logger facade. */
interface LoggerBackend {

    /**
     * Returns whether a logging level is enabled.
     *
     * @param level logging level
     * @return {@code true} when the level is enabled
     */
    boolean isEnabled(Level level);

    /**
     * Logs a pre-formatted message.
     *
     * @param level logging level
     * @param message message to log
     */
    void log(Level level, String message);
}
