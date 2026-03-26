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

import static java.lang.String.format;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/**
 * Logger wrapper providing simple logging with TRACE, INFO, WARN, and ERROR levels.
 *
 * <p>This class wraps {@link java.util.logging.Logger} and provides:
 *
 * <ul>
 *   <li>Simple logging interface with format strings
 *   <li>Developer debug mode for additional console output
 *   <li>Custom formatter for consistent log message format
 * </ul>
 *
 * <p>Developer debug mode can be enabled via:
 *
 * <ul>
 *   <li>Environment variable: {@code JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG=true}
 *   <li>System property: {@code -Djmx.prometheus.exporter.developer.debug=true}
 * </ul>
 *
 * <p>Thread-safety: This class is thread-safe. The underlying logger is thread-safe, and
 * the date format uses thread-local storage.
 */
public class Logger {

    /**
     * The underlying Java util logger.
     */
    private final java.util.logging.Logger LOGGER;

    /**
     * Flag indicating if developer debug mode is enabled.
     *
     * <p>When enabled, logs are also written to stdout in addition to the normal logging
     * destination.
     */
    private final boolean JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG =
            "true".equals(System.getenv("JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG"))
                    || "true".equals(System.getProperty("jmx.prometheus.exporter.developer.debug"));

    /**
     * Thread-local date format for log timestamps.
     */
    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    /**
     * Constructs a logger for the specified class.
     *
     * @param clazz the class for which to create a logger, must not be {@code null}
     */
    Logger(Class<?> clazz) {
        LOGGER = java.util.logging.Logger.getLogger(clazz.getName());

        // Override the default formatter for the logger if it is SimpleFormatter
        for (Handler handler : LOGGER.getHandlers()) {
            if (null != handler.getFormatter()
                    && handler.getFormatter().getClass().getName().endsWith(SimpleFormatter.class.getName())) {
                handler.setFormatter(new LoggerFormatter());
            }
        }
    }

    /**
     * Returns whether TRACE level logging is enabled.
     *
     * @return {@code true} if TRACE logging is enabled, {@code false} otherwise
     */
    public boolean isTraceEnabled() {
        return isLoggable(Level.TRACE);
    }

    /**
     * Returns whether INFO level logging is enabled.
     *
     * @return {@code true} if INFO logging is enabled, {@code false} otherwise
     */
    public boolean isInfoEnabled() {
        return isLoggable(Level.INFO);
    }

    /**
     * Returns whether WARN level logging is enabled.
     *
     * @return {@code true} if WARN logging is enabled, {@code false} otherwise
     */
    public boolean isWarnEnabled() {
        return isLoggable(Level.WARN);
    }

    /**
     * Returns whether ERROR level logging is enabled.
     *
     * @return {@code true} if ERROR logging is enabled, {@code false} otherwise
     */
    public boolean isErrorEnabled() {
        return isLoggable(Level.ERROR);
    }

    /**
     * Logs a TRACE level message.
     *
     * @param message the message to log
     */
    public void trace(String message) {
        trace("%s", message);
    }

    /**
     * Logs a TRACE level message with format string.
     *
     * @param format the format string
     * @param object the object to format
     */
    public void trace(String format, Object object) {
        log(Level.TRACE, format, object);
    }

    /**
     * Logs a TRACE level message with format string.
     *
     * @param format the format string
     * @param objects the objects to format
     */
    public void trace(String format, Object... objects) {
        log(Level.TRACE, format, objects);
    }

    /**
     * Logs an INFO level message.
     *
     * @param message the message to log
     */
    public void info(String message) {
        info("%s", message);
    }

    /**
     * Logs an INFO level message with format string.
     *
     * @param format the format string
     * @param object the object to format
     */
    public void info(String format, Object object) {
        log(Level.INFO, format, object);
    }

    /**
     * Logs an INFO level message with format string.
     *
     * @param format the format string
     * @param objects the objects to format
     */
    public void info(String format, Object... objects) {
        log(Level.INFO, format, objects);
    }

    /**
     * Logs a WARN level message.
     *
     * @param message the message to log
     */
    public void warn(String message) {
        warn("%s", message);
    }

    /**
     * Logs a WARN level message with format string.
     *
     * @param format the format string
     * @param object the object to format
     */
    public void warn(String format, Object object) {
        log(Level.WARN, format, object);
    }

    /**
     * Logs a WARN level message with format string.
     *
     * @param format the format string
     * @param objects the objects to format
     */
    public void warn(String format, Object... objects) {
        log(Level.WARN, format, objects);
    }

    /**
     * Logs an ERROR level message.
     *
     * @param message the message to log
     */
    public void error(String message) {
        error("%s", message);
    }

    /**
     * Logs an ERROR level message with format string.
     *
     * @param format the format string
     * @param objects the objects to format
     */
    public void error(String format, Object... objects) {
        log(Level.ERROR, format, objects);
    }

    /**
     * Returns whether the specified level is loggable.
     *
     * @param level the level to check
     * @return {@code true} if the level is loggable, {@code false} otherwise
     */
    private boolean isLoggable(Level level) {
        java.util.logging.Level julLevel = decode(level);
        return julLevel != null && LOGGER.isLoggable(julLevel);
    }

    /**
     * Method to log a message
     *
     * @param level the level
     * @param format the format
     * @param objects the objects
     */
    private void log(Level level, String format, Object... objects) {
        if (isLoggable(level)) {
            java.util.logging.Level julLevel = decode(level);
            if (julLevel != null) {
                LOGGER.log(julLevel, format(format, objects));
            }
        }

        if (JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG) {
            String timestamp = SIMPLE_DATE_FORMAT_THREAD_LOCAL.get().format(new Date());
            String threadName = Thread.currentThread().getName();
            String loggerName = LOGGER.getName();
            String message = format(format, objects);

            System.out.printf("%s | %s | %s | %s | %s%n", timestamp, threadName, Level.TRACE, loggerName, message);
        }
    }

    /**
     * Method to decode a Level to a java.util.logging.Level
     *
     * @param level the level
     * @return the java.util.logging.Level
     */
    private static java.util.logging.Level decode(Level level) {
        java.util.logging.Level julLevel;

        switch (level) {
            case TRACE: {
                julLevel = java.util.logging.Level.FINEST;
                break;
            }
            case INFO: {
                julLevel = java.util.logging.Level.INFO;
                break;
            }
            case WARN: {
                julLevel = java.util.logging.Level.WARNING;
                break;
            }
            case ERROR: {
                julLevel = java.util.logging.Level.SEVERE;
                break;
            }
            default: {
                julLevel = null;
            }
        }

        return julLevel;
    }
}
