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

/** Class to implement AutoIncrementing */
public class AutoIncrementing implements AutoIncrementingMBean {

    private final AtomicInteger atomicInteger;

    /** Constructor */
    public AutoIncrementing() {
        atomicInteger = new AtomicInteger(0);
    }

    @Override
    public int getValue() {
        return atomicInteger.getAndIncrement();
    }

    /**
     * Method to register the MBean
     *
     * @throws Exception If an error occurs during registration
     */
    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(this, new ObjectName("io.prometheus.jmx:type=autoIncrementing"));
    }
}
