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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** JUL-independent logging backend that writes messages to standard error. */
final class NativeLoggerBackend implements LoggerBackend {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final String loggerName;

    /**
     * Constructs a native backend for the specified logger name.
     *
     * @param loggerName logger name
     */
    NativeLoggerBackend(String loggerName) {
        this.loggerName = loggerName;
    }

    @Override
    public boolean isEnabled(Level level) {
        return level.ordinal() >= Level.INFO.ordinal();
    }

    @Override
    public void log(Level level, String message) {
        String timestamp = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String threadName = Thread.currentThread().getName();
        synchronized (System.err) {
            System.err.printf("%s | %s | %s | %s | %s%n", timestamp, threadName, level, loggerName, message);
        }
    }
}
