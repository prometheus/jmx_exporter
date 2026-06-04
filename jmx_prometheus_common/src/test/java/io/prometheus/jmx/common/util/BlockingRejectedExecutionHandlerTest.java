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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class BlockingRejectedExecutionHandlerTest {

    @Test
    public void testRejectedExecutionQueuesTaskWhenNotShutdown() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10), new BlockingRejectedExecutionHandler());

        assertThat(executor.getQueue().size()).isZero();

        executor.getRejectedExecutionHandler().rejectedExecution(() -> {}, executor);

        assertThat(executor.getQueue().size()).isEqualTo(1);

        executor.shutdown();
    }

    @Test
    public void testRejectedExecutionDiscardsTaskWhenShutdown() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(10), new BlockingRejectedExecutionHandler());
        executor.shutdown();

        assertThat(executor.getQueue().size()).isZero();

        executor.getRejectedExecutionHandler().rejectedExecution(() -> {}, executor);

        assertThat(executor.getQueue().size()).isZero();
    }

    @Test
    public void testImplementRejectedExecutionHandlerInterface() {
        BlockingRejectedExecutionHandler handler = new BlockingRejectedExecutionHandler();

        assertThat(handler).isInstanceOf(java.util.concurrent.RejectedExecutionHandler.class);
    }
}
