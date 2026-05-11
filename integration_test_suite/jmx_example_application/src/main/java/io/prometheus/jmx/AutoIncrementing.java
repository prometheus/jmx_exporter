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

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Implements an MBean exposing an auto-incrementing integer counter backed by an {@link AtomicInteger}.
 *
 * <p>Each call to {@link #getValue()} returns the current counter value and atomically increments it,
 * producing a monotonically increasing sequence for integration testing.
 */
public class AutoIncrementing implements AutoIncrementingMBean {

    private final AtomicInteger atomicInteger;

    /**
     * Constructs a new instance with the counter initialized to zero.
     */
    public AutoIncrementing() {
        atomicInteger = new AtomicInteger(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getValue() {
        return atomicInteger.getAndIncrement();
    }

    /**
     * Registers this MBean with the platform MBean server under the object name
     * {@code io.prometheus.jmx:type=autoIncrementing}.
     *
     * @throws Exception if MBean registration fails
     */
    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(this, new ObjectName("io.prometheus.jmx:type=autoIncrementing"));
    }
}
