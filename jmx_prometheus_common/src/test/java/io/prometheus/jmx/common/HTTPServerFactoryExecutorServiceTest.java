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

package io.prometheus.jmx.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Regression test for the shared {@code EXECUTOR_SERVICE}, used for SSL certificate reload
 * scheduling and per-request deadlines.
 *
 * <p>Its worker thread must be a daemon thread. If it is not, the JVM never exits once {@code
 * httpServer.ssl} is configured, even after the application's own {@code main()} has returned or
 * thrown: the thread is created the first time something is scheduled on it and is never
 * explicitly stopped until JVM shutdown itself begins, which a non-daemon thread prevents.
 */
class HTTPServerFactoryExecutorServiceTest {

    @Test
    void executorServiceUsesADaemonThread() throws Exception {
        Field field = HTTPServerFactory.class.getDeclaredField("EXECUTOR_SERVICE");
        field.setAccessible(true);
        ScheduledExecutorService executorService = (ScheduledExecutorService) field.get(null);

        Thread[] workerThread = new Thread[1];
        executorService.submit(() -> workerThread[0] = Thread.currentThread()).get(5, TimeUnit.SECONDS);

        assertThat(workerThread[0].isDaemon()).isTrue();
        assertThat(workerThread[0].getName()).startsWith("prometheus-http-");
    }
}
