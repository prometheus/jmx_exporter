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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

public class MarkedThreadPoolExecutorTest {

    @Test
    public void poolThreadFlagIsTrue() throws Exception {
        MarkedThreadPoolExecutor executor = new MarkedThreadPoolExecutor(
                1,
                1,
                120,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                },
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        try {
            AtomicBoolean flagValue = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);

            executor.execute(() -> {
                flagValue.set(MarkedThreadPoolExecutor.IS_POOL_THREAD.get());
                latch.countDown();
            });

            latch.await(5, TimeUnit.SECONDS);
            assertThat(flagValue.get()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testThreadFlagIsFalse() {
        assertThat(MarkedThreadPoolExecutor.IS_POOL_THREAD.get()).isFalse();
    }

    @Test
    public void flagClearedAfterTask() throws Exception {
        MarkedThreadPoolExecutor executor = new MarkedThreadPoolExecutor(
                1,
                1,
                120,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                },
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        try {
            AtomicBoolean flagDuringTask = new AtomicBoolean(false);
            CountDownLatch taskDone = new CountDownLatch(1);

            executor.execute(() -> {
                flagDuringTask.set(MarkedThreadPoolExecutor.IS_POOL_THREAD.get());
                taskDone.countDown();
            });

            taskDone.await(5, TimeUnit.SECONDS);
            assertThat(flagDuringTask.get()).isTrue();
            assertThat(MarkedThreadPoolExecutor.IS_POOL_THREAD.get()).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void callerRunsPolicyFlagIsFalse() throws Exception {
        MarkedThreadPoolExecutor executor = new MarkedThreadPoolExecutor(
                1,
                1,
                120,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    return t;
                },
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        try {
            CountDownLatch holdPoolThread = new CountDownLatch(1);
            CountDownLatch firstTaskStarted = new CountDownLatch(1);
            AtomicBoolean secondTaskFlag = new AtomicBoolean(true);

            // First task blocks the single pool thread
            executor.execute(() -> {
                firstTaskStarted.countDown();
                try {
                    holdPoolThread.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            firstTaskStarted.await(5, TimeUnit.SECONDS);

            // Second task will be rejected and run via CallerRunsPolicy on the test thread
            executor.execute(() -> {
                secondTaskFlag.set(MarkedThreadPoolExecutor.IS_POOL_THREAD.get());
            });

            assertThat(secondTaskFlag.get()).isFalse();

            holdPoolThread.countDown();
        } finally {
            executor.shutdownNow();
        }
    }
}
