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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A ThreadPoolExecutor that marks threads executing tasks with a ThreadLocal flag.
 *
 * <p>This allows code running on a pool thread to detect that it is a pool thread, which is used
 * to distinguish between the normal execution path and the rejected-execution (CallerRunsPolicy)
 * path.
 */
public class MarkedThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * ThreadLocal flag indicating whether the current thread is a pool thread executing a task.
     * Returns {@code true} during task execution on a pool thread, {@code false} otherwise.
     */
    public static final ThreadLocal<Boolean> IS_POOL_THREAD = ThreadLocal.withInitial(() -> false);

    /**
     * Constructs a MarkedThreadPoolExecutor.
     *
     * @param corePoolSize the core pool size
     * @param maximumPoolSize the maximum pool size
     * @param keepAliveTime the keep-alive time
     * @param unit the time unit
     * @param workQueue the work queue
     * @param threadFactory the thread factory
     * @param handler the rejected execution handler
     */
    public MarkedThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        IS_POOL_THREAD.set(true);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        IS_POOL_THREAD.set(false);
    }
}
