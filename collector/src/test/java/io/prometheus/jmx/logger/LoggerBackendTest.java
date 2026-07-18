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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoggerBackendTest {

    private final String originalBackend = System.getProperty(LoggerFactory.BACKEND_PROPERTY);

    @AfterEach
    void restoreBackend() {
        if (originalBackend == null) {
            System.clearProperty(LoggerFactory.BACKEND_PROPERTY);
        } else {
            System.setProperty(LoggerFactory.BACKEND_PROPERTY, originalBackend);
        }
    }

    @Test
    void nativeBackendWritesWithoutJulLogger() {
        System.setProperty(LoggerFactory.BACKEND_PROPERTY, "native");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalError = System.err;
        System.setErr(new PrintStream(output, true));
        try {
            Logger logger = new Logger(LoggerBackendTest.class);
            logger.info("native message");

            assertThat(output.toString()).contains("native message");
            assertThat(output.toString()).contains(LoggerBackendTest.class.getName());
        } finally {
            System.setErr(originalError);
        }
    }

    @Test
    void julBackendRemainsAvailable() {
        System.setProperty(LoggerFactory.BACKEND_PROPERTY, "jul");

        Logger logger = new Logger(LoggerBackendTest.class);

        assertThat(logger.isInfoEnabled()).isTrue();
    }

    @Test
    void invalidBackendIsRejected() {
        System.setProperty(LoggerFactory.BACKEND_PROPERTY, "invalid");

        assertThatThrownBy(() -> new Logger(InvalidBackendLogger.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected [native] or [jul]");
    }

    private static final class InvalidBackendLogger {}
}
