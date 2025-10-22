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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/** Class to implement PerformanceMetrics */
public class PerformanceMetrics implements PerformanceMetricsMBean {

    private final CompositeData compositeData;

    /**
     * Constructor
     *
     * @throws OpenDataException OpenDataException
     */
    public PerformanceMetrics() throws OpenDataException {
        compositeData = build();
    }

    @Override
    public CompositeData getPerformanceMetrics() {
        return compositeData;
    }

    private CompositeData build() throws OpenDataException {
        Map<String, Number> data = new LinkedHashMap<>();

        data.put("ActiveSessions", 2L);
        data.put("Bootstraps", 4L);
        data.put("BootstrapsDeferred", 6L);

        String[] names = {"ActiveSessions", "Bootstraps", "BootstrapsDeferred"};
        String[] descriptions = {"ActiveSessions", "Bootstraps", "BootstrapsDeferred"};
        OpenType<?>[] types = {SimpleType.LONG, SimpleType.LONG, SimpleType.LONG};

        CompositeType compositeType =
                new CompositeType(
                        "PerformanceMetrics", "Example composite data", names, descriptions, types);

        return new CompositeDataSupport(compositeType, data);
    }

    /**
     * Method to register the MBean
     *
     * @throws Exception If an error occurs during registration
     */
    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(
                new PerformanceMetrics(),
                new ObjectName("io.prometheus.jmx.test:name=PerformanceMetricsMBean"));
    }
}
