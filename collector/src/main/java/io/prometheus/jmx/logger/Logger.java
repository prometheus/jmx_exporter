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

/** Class to implement a Logger */
public class Logger {

    private final java.util.logging.Logger LOGGER;

    private final boolean JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG =
            "true".equals(System.getenv("JMX_PROMETHEUS_EXPORTER_DEVELOPER_DEBUG"))
                    || "true".equals(System.getProperty("jmx.prometheus.exporter.developer.debug"));

    private static final ThreadLocal<SimpleDateFormat> SIMPLE_DATE_FORMAT_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));

    /**
     * Constructor
     *
     * @param clazz the class
     */
    Logger(Class<?> clazz) {
        LOGGER = java.util.logging.Logger.getLogger(clazz.getName());

        // Override the default formatter for the logger if it is SimpleFormatter
        for (Handler handler : LOGGER.getHandlers()) {
            if (null != handler.getFormatter()
                    && handler.getFormatter()
                            .getClass()
                            .getName()
                            .endsWith(SimpleFormatter.class.getName())) {
                handler.setFormatter(new LoggerFormatter());
            }
        }
    }

    /**
     * Method to return whether TRACE logging is enabled
     *
     * @return true if TRACE logging is enabled, else false
     */
    public boolean isTraceEnabled() {
        return isLoggable(Level.TRACE);
    }

    /**
     * Method to return whether INFO logging is enabled
     *
     * @return true if INFO logging is enabled, else false
     */
    public boolean isInfoEnabled() {
        return isLoggable(Level.INFO);
    }

    /**
     * Method to return whether WARN logging is enabled
     *
     * @return true if WARN logging is enabled, else false
     */
    public boolean isWarnEnabled() {
        return isLoggable(Level.WARN);
    }

    /**
     * Method to return whether ERROR logging is enabled
     *
     * @return true if ERROR logging is enabled, else false
     */
    public boolean isErrorEnabled() {
        return isLoggable(Level.ERROR);
    }

    /**
     * Method to log an TRACE message
     *
     * @param message the message
     */
    public void trace(String message) {
        trace("%s", message);
    }

    /**
     * Method to log an TRACE message
     *
     * @param format the format
     * @param object the object
     */
    public void trace(String format, Object object) {
        log(Level.TRACE, format, object);
    }

    /**
     * Method to log an TRACE message
     *
     * @param format the format
     * @param objects the objects
     */
    public void trace(String format, Object... objects) {
        log(Level.TRACE, format, objects);
    }

    /**
     * Method to log an INFO message
     *
     * @param message the message
     */
    public void info(String message) {
        info("%s", message);
    }

    /**
     * Method to log an INFO message
     *
     * @param format the format
     * @param object the object
     */
    public void info(String format, Object object) {
        log(Level.INFO, format, object);
    }

    /**
     * Method to log an INFO message
     *
     * @param format the format
     * @param objects the objects
     */
    public void info(String format, Object... objects) {
        log(Level.INFO, format, objects);
    }

    /**
     * Method to log an WARN message
     *
     * @param message the message
     */
    public void warn(String message) {
        warn("%s", message);
    }

    /**
     * Method to log an WARN message
     *
     * @param format the format
     * @param object the object
     */
    public void warn(String format, Object object) {
        log(Level.WARN, format, object);
    }

    /**
     * Method to log an WARN message
     *
     * @param format the format
     * @param objects the objects
     */
    public void warn(String format, Object... objects) {
        log(Level.WARN, format, objects);
    }

    /**
     * Method to log an ERROR message
     *
     * @param message the message
     */
    public void error(String message) {
        error("%s", message);
    }

    /**
     * Method to log an ERROR message
     *
     * @param format the format
     * @param objects the objects
     */
    public void error(String format, Object... objects) {
        log(Level.ERROR, format, objects);
    }

    /**
     * Method to return whether a log level is enabled
     *
     * @param level the level
     * @return true if the log level is enabled, else false
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

            System.out.printf(
                    "%s | %s | %s | %s | %s%n",
                    timestamp, threadName, Level.TRACE, loggerName, message);
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
            case TRACE:
                {
                    julLevel = java.util.logging.Level.FINEST;
                    break;
                }
            case INFO:
                {
                    julLevel = java.util.logging.Level.INFO;
                    break;
                }
            case WARN:
                {
                    julLevel = java.util.logging.Level.WARNING;
                    break;
                }
            case ERROR:
                {
                    julLevel = java.util.logging.Level.SEVERE;
                    break;
                }
            default:
                {
                    julLevel = null;
                }
        }

        return julLevel;
    }
}
