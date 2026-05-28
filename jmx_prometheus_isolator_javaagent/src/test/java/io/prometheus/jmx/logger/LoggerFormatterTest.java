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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;

public class LoggerFormatterTest {

    @Test
    public void testFormatOutputContainsAllFields() {
        LoggerFormatter formatter = new LoggerFormatter();
        LogRecord record = new LogRecord(Level.INFO, "test message");
        record.setLoggerName("test.Logger");
        record.setMillis(System.currentTimeMillis());

        String formatted = formatter.format(record);

        assertThat(formatted).contains("INFO");
        assertThat(formatted).contains("test.Logger");
        assertThat(formatted).contains("test message");
        assertThat(formatted.endsWith(System.lineSeparator())).isTrue();
    }

    @Test
    public void testFormatOutputContainsPipeDelimiters() {
        LoggerFormatter formatter = new LoggerFormatter();
        LogRecord record = new LogRecord(Level.WARNING, "warn message");
        record.setLoggerName("my.Logger");
        record.setMillis(System.currentTimeMillis());

        String formatted = formatter.format(record);

        long pipeCount = formatted.chars().filter(c -> c == '|').count();
        assertThat(pipeCount).isEqualTo(4);
    }
}
