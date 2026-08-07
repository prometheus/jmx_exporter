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

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/** Logging backend that delegates to Java Util Logging (JUL). */
final class JulLoggerBackend implements LoggerBackend {

    private static final String ROOT_LOGGER = "";

    private final java.util.logging.Logger logger;

    static {
        configureSimpleFormatters(java.util.logging.Logger.getLogger(ROOT_LOGGER));
    }

    /**
     * Constructs a JUL backend for the specified logger name.
     *
     * @param loggerName logger name
     */
    JulLoggerBackend(String loggerName) {
        logger = java.util.logging.Logger.getLogger(loggerName);
        configureSimpleFormatters(logger);
    }

    private static void configureSimpleFormatters(java.util.logging.Logger logger) {
        for (Handler handler : logger.getHandlers()) {
            Formatter formatter = handler.getFormatter();
            if (formatter != null && formatter.getClass().getName().endsWith(SimpleFormatter.class.getName())) {
                handler.setFormatter(new LoggerFormatter());
            }
        }
    }

    @Override
    public boolean isEnabled(Level level) {
        return logger.isLoggable(toJulLevel(level));
    }

    @Override
    public void log(Level level, String message) {
        logger.log(toJulLevel(level), message);
    }

    static java.util.logging.Level toJulLevel(Level level) {
        switch (level) {
            case TRACE:
                return java.util.logging.Level.FINEST;
            case INFO:
                return java.util.logging.Level.INFO;
            case WARN:
                return java.util.logging.Level.WARNING;
            case ERROR:
                return java.util.logging.Level.SEVERE;
            default:
                throw new IllegalArgumentException("Unknown logging level: " + level);
        }
    }
}
