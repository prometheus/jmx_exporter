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

package io.prometheus.jmx.javaagent.benchmarks;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Registers and unregisters the dynamic MBeans used by the javaagent registry benchmarks.
 */
final class AgentBenchmarkMBeans {

    static final String DOMAIN = "io.prometheus.jmx.javaagent.benchmark";
    static final String INCLUDE_OBJECT_NAMES = "includeObjectNames:\n- \"" + DOMAIN + ":*\"\n";

    private AgentBenchmarkMBeans() {
        // Intentionally empty
    }

    /**
     * Registers the requested number of benchmark beans.
     *
     * @param beanCount the number of beans
     * @param attributeCount the number of attributes per bean
     * @return the registered ObjectNames
     * @throws Exception if registration fails
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
     * Unregisters the given beans.
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

    /**
     * A simple DynamicMBean exposing a fixed set of numeric attributes.
     */
    private static final class BenchmarkMBean implements DynamicMBean {

        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private final MBeanInfo mBeanInfo;

        BenchmarkMBean(int attributeCount) {
            MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[attributeCount];
            for (int i = 0; i < attributeCount; i++) {
                String name = "attribute" + i;
                attributes.put(name, (double) i);
                attributeInfos[i] =
                        new MBeanAttributeInfo(name, "double", "Benchmark attribute " + i, true, false, false);
            }
            this.mBeanInfo =
                    new MBeanInfo(BenchmarkMBean.class.getName(), "Benchmark MBean", attributeInfos, null, null, null);
        }

        @Override
        public Object getAttribute(String attribute)
                throws AttributeNotFoundException, MBeanException, ReflectionException {
            Object value = attributes.get(attribute);
            if (value == null) {
                throw new AttributeNotFoundException(attribute);
            }
            return value;
        }

        @Override
        public void setAttribute(Attribute attribute)
                throws AttributeNotFoundException, InvalidAttributeValueException {
            throw new InvalidAttributeValueException("read-only");
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            AttributeList result = new AttributeList();
            for (String attribute : attributes) {
                Object value = this.attributes.get(attribute);
                if (value != null) {
                    result.add(new Attribute(attribute, value));
                }
            }
            return result;
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            return new AttributeList();
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) {
            return null;
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            return mBeanInfo;
        }
    }
}
