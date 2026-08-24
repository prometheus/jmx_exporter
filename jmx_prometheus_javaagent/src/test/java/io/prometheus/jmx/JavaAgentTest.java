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

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JavaAgent} error handling behavior.
 *
 * <p>Verifies that the agent exits the JVM in fail-fast mode and keeps running in graceful error
 * handling mode.
 */
public class JavaAgentTest {

    /**
     * Verifies that graceful error handling does not trigger the exit action.
     */
    @Test
    public void testHandleErrorGracefulDoesNotExit() {
        AtomicBoolean exited = new AtomicBoolean(false);
        RuntimeException error = new RuntimeException("boom");

        JavaAgent.handleError(error, null, null, true, code -> exited.set(true));

        assertThat(exited).isFalse();
    }

    /**
     * Verifies that fail-fast mode triggers the exit action.
     */
    @Test
    public void testHandleErrorFailFastExits() {
        AtomicBoolean exited = new AtomicBoolean(false);
        RuntimeException error = new RuntimeException("boom");

        JavaAgent.handleError(error, null, null, false, code -> exited.set(true));

        assertThat(exited).isTrue();
    }
}
