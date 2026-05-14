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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import org.junit.jupiter.api.Test;

public class LoggerFactoryTest {

    @Test
    public void testGetLoggerReturnsSameInstanceForSameClass() {
        Logger logger1 = LoggerFactory.getLogger(LoggerFactoryTest.class);
        Logger logger2 = LoggerFactory.getLogger(LoggerFactoryTest.class);

        assertThat(logger1).isSameAs(logger2);
    }

    @Test
    public void testGetLoggerReturnsDifferentInstancesForDifferentClasses() {
        Logger logger1 = LoggerFactory.getLogger(LoggerFactoryTest.class);
        Logger logger2 = LoggerFactory.getLogger(String.class);

        assertThat(logger1).isNotSameAs(logger2);
    }

    @Test
    public void testGetLoggerReturnsNonNull() {
        Logger logger = LoggerFactory.getLogger(LoggerFactoryTest.class);
        assertThat(logger).isNotNull();
    }
}
