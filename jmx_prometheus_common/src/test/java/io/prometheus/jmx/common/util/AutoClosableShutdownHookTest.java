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

package io.prometheus.jmx.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class AutoClosableShutdownHookTest {

    @Test
    public void testRunClosesResource() {
        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable resource = () -> closed.set(true);
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(resource);

        hook.run();

        assertThat(closed).isTrue();
    }

    @Test
    public void testRunSilentlyHandlesException() {
        AutoCloseable resource = () -> {
            throw new RuntimeException("test exception");
        };
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(resource);

        hook.run();
    }

    @Test
    public void testRunSilentlyHandlesCheckedException() {
        AutoCloseable resource = () -> {
            throw new Exception("test checked exception");
        };
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(resource);

        hook.run();
    }

    @Test
    public void testRunSilentlyHandlesNullResource() {
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(null);

        hook.run();
    }

    @Test
    public void testRunCallsCloseExactlyOnce() {
        AtomicInteger callCount = new AtomicInteger(0);
        AutoCloseable resource = callCount::incrementAndGet;
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(resource);

        hook.run();

        assertThat(callCount).hasValue(1);
    }

    @Test
    public void testConstructorAcceptsNonNullResource() {
        AutoCloseable resource = () -> {};
        AutoClosableShutdownHook hook = new AutoClosableShutdownHook(resource);

        assertThat(hook).isNotNull();
    }
}
