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

package io.prometheus.jmx.benchmarks;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Registers and unregisters the benchmark MBeans used by the collector benchmarks.
 */
final class BenchmarkMBeans {

    static final String DOMAIN = "io.prometheus.jmx.benchmark";
    static final String INCLUDE_OBJECT_NAMES = "includeObjectNames:\n- \"" + DOMAIN + ":*\"\n";

    private BenchmarkMBeans() {
        // Intentionally empty
    }

    /**
     * Registers the requested number of benchmark beans.
     *
     * @param beanCount the number of beans to register
     * @param attributeCount the number of attributes per bean
     * @return the registered ObjectNames
     * @throws Exception if a bean cannot be registered
     */
    static List<ObjectName> register(int beanCount, int attributeCount) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        List<ObjectName> objectNames = new ArrayList<>(beanCount);
        for (int i = 0; i < beanCount; i++) {
            ObjectName objectName = new ObjectName(DOMAIN + ":type=Benchmark,name=bean" + i);
            if (!mBeanServer.isRegistered(objectName)) {
                mBeanServer.registerMBean(new BenchmarkMBean(attributeCount), objectName);
            }
            objectNames.add(objectName);
        }
        return objectNames;
    }

    /**
     * Unregisters the given beans, ignoring any failures.
     *
     * @param objectNames the ObjectNames to unregister
     */
    static void unregister(List<ObjectName> objectNames) {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectName objectName : objectNames) {
            try {
                if (mBeanServer.isRegistered(objectName)) {
                    mBeanServer.unregisterMBean(objectName);
                }
            } catch (Exception ignored) {
                // Benchmarks should not fail during teardown.
            }
        }
        objectNames.clear();
    }
}
