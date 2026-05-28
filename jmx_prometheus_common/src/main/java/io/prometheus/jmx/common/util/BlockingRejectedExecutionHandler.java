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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Rejected execution handler that blocks when the thread pool queue is full.
 *
 * <p>Instead of rejecting tasks when the queue is full, this handler attempts to put the task
 * into the queue, blocking until space is available.
 */
public class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {

    /**
     * Blocks the calling thread by attempting to put the rejected task into the executor's queue,
     * waiting until space becomes available.
     *
     * <p>If the executor has been shut down, the task is silently discarded.
     *
     * @param runnable the rejected runnable
     * @param threadPoolExecutor the executor that rejected the task
     */
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        if (!threadPoolExecutor.isShutdown()) {
            try {
                threadPoolExecutor.getQueue().put(runnable);
            } catch (InterruptedException e) {
                // Intentionally empty
            }
        }
    }
}
