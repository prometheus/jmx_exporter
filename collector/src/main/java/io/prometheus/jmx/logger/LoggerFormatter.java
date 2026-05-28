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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Custom {@link Formatter} for log messages.
 *
 * <p>Produces log messages in the format:
 * <pre>
 * yyyy-MM-dd HH:mm:ss.SSS | thread | LEVEL | loggerName | message
 * </pre>
 */
class LoggerFormatter extends Formatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @Override
    public String format(LogRecord record) {
        String timestamp = DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
        String threadName = Thread.currentThread().getName();
        String level = record.getLevel().getName();
        String loggerName = record.getLoggerName();
        String message = formatMessage(record);

        return String.format("%s | %s | %s | %s | %s%n", timestamp, threadName, level, loggerName, message);
    }
}
