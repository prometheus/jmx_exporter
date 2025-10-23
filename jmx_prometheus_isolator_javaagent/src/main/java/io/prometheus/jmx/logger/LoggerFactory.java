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
import java.util.logging.Handler;
import java.util.logging.SimpleFormatter;

/** Class to implement LoggerFactory */
public class LoggerFactory {

    /** The root logger name */
    private static final String ROOT_LOGGER = "";

    /** Cache for Logger instances */
    private static final ConcurrentMap<Class<?>, Logger> CACHE = new ConcurrentHashMap<>();

    static {
        // Override the default formatter for the root logger if it is SimpleFormatter
        for (Handler handler : java.util.logging.Logger.getLogger(ROOT_LOGGER).getHandlers()) {
            if (null != handler.getFormatter()
                    && handler.getFormatter()
                            .getClass()
                            .getName()
                            .endsWith(SimpleFormatter.class.getName())) {
                handler.setFormatter(new LoggerFormatter());
            }
        }
    }

    /** Constructor */
    private LoggerFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get a Logger
     *
     * @param clazz the class for which the logger is to be created
     * @return a Logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, Logger::new);
    }
}
