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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logger wrapper providing simple logging with TRACE, INFO, WARN, and ERROR levels.
 *
 * <p>This class delegates to the configured logging backend and provides:
 *
 * <ul>
 *   <li>Simple logging interface with format strings
 *   <li>Native logging that does not initialize Java Util Logging (JUL)
 *   <li>Optional JUL integration
 *   <li>Developer debug mode for additional console output
 * </ul>
 *
 * <p>Developer debug mode can be enabled via:
 *
 * <ul>
 *   <li>Environment variable: {@code JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG=true}
 *   <li>System property: {@code -Djmx.prometheus.exporter.developer.debug=true}
 * </ul>
 *
 * <p>Thread-safety: This class is thread-safe. Logging backends are responsible for serializing
 * writes where required, and date formatting uses a thread-safe {@link DateTimeFormatter}.
 */
public class Logger {

    /**
     * The configured logging backend.
     */
    private final LoggerBackend backend;

    /**
     * Cached logger name for developer debug output.
     */
    private final String loggerName;

    /**
     * Flag indicating if developer debug mode is enabled.
     *
     * <p>When enabled, logs are also written to stdout in addition to the configured logging
     * backend. Evaluated once at class load time.
     */
    private static volatile boolean DEVELOPER_DEBUG =
            "true".equals(System.getenv("JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG"))
                    || "true".equals(System.getProperty("jmx.prometheus.exporter.developer.debug"));

    /**
     * Thread-safe date formatter for developer debug timestamps.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Constructs a logger for the specified class.
     *
     * @param clazz the class for which to create a logger, must not be {@code null}
     */
    Logger(Class<?> clazz) {
        loggerName = clazz.getName();
        backend = LoggerFactory.createBackend(loggerName);
    }

    /**
     * Returns whether TRACE level logging is enabled.
     *
     * @return {@code true} if TRACE logging is enabled, {@code false} otherwise
     */
    public boolean isTraceEnabled() {
        return backend.isEnabled(Level.TRACE);
    }

    /**
     * Returns whether INFO level logging is enabled.
     *
     * @return {@code true} if INFO logging is enabled, {@code false} otherwise
     */
    public boolean isInfoEnabled() {
        return backend.isEnabled(Level.INFO);
    }

    /**
     * Returns whether WARN level logging is enabled.
     *
     * @return {@code true} if WARN logging is enabled, {@code false} otherwise
     */
    public boolean isWarnEnabled() {
        return backend.isEnabled(Level.WARN);
    }

    /**
     * Returns whether ERROR level logging is enabled.
     *
     * @return {@code true} if ERROR logging is enabled, {@code false} otherwise
     */
    public boolean isErrorEnabled() {
        return backend.isEnabled(Level.ERROR);
    }

    /**
     * Logs a TRACE level message.
     *
     * @param message the message to log
     */
    public void trace(String message) {
        log(Level.TRACE, message);
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
        log(Level.INFO, message);
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
        log(Level.WARN, message);
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
        log(Level.ERROR, message);
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
     * Logs a pre-formatted message at the given level.
     *
     * <p>Skips {@link String#format} since the message is already a plain string.
     *
     * @param level the level
     * @param message the pre-formatted message
     */
    private void log(Level level, String message) {
        boolean loggable = backend.isEnabled(level);
        boolean debug = DEVELOPER_DEBUG;
        if (loggable) {
            backend.log(level, message);
        }
        if (debug) {
            developerDebug(level, message);
        }
    }

    /**
     * Logs a formatted message at the given level.
     *
     * @param level the level
     * @param format the format string
     * @param objects the objects to format
     */
    private void log(Level level, String format, Object... objects) {
        boolean loggable = backend.isEnabled(level);
        boolean debug = DEVELOPER_DEBUG;
        if (loggable || debug) {
            String message = format(format, objects);
            if (loggable) {
                backend.log(level, message);
            }
            if (debug) {
                developerDebug(level, message);
            }
        }
    }

    /**
     * Writes a developer debug message to stdout with timestamp, thread, level, and logger name.
     */
    private void developerDebug(Level level, String message) {
        String timestamp = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        String threadName = Thread.currentThread().getName();
        System.out.printf("%s | %s | %s | %s | %s%n", timestamp, threadName, level, loggerName, message);
    }
}
