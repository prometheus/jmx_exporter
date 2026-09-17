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

import java.util.LinkedHashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;

/**
 * A minimal {@link DynamicMBean} exposing a fixed set of numeric attributes.
 *
 * <p>Used by the collector benchmarks to register a configurable number of beans and attributes
 * without hand-writing a {@code *MBean} interface and implementation.
 */
public final class BenchmarkMBean implements DynamicMBean {

    private final Map<String, Object> values;
    private final MBeanInfo mBeanInfo;

    /**
     * Constructor
     *
     * @param attributeCount the number of numeric attributes to expose
     */
    public BenchmarkMBean(int attributeCount) {
        values = new LinkedHashMap<>(attributeCount);
        MBeanAttributeInfo[] attributeInfos = new MBeanAttributeInfo[attributeCount];
        for (int i = 0; i < attributeCount; i++) {
            String name = "attribute_" + i;
            values.put(name, (double) i);
            attributeInfos[i] = new MBeanAttributeInfo(name, "double", name, true, false, false);
        }
        mBeanInfo = new MBeanInfo(getClass().getName(), "collector benchmark bean", attributeInfos, null, null, null);
    }

    @Override
    public Object getAttribute(String attribute) {
        return values.get(attribute);
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList(attributes.length);
        for (String attribute : attributes) {
            Object value = values.get(attribute);
            if (value != null) {
                result.add(new Attribute(attribute, value));
            }
        }
        return result;
    }

    @Override
    public void setAttribute(Attribute attribute) {
        throw new UnsupportedOperationException("read-only benchmark bean");
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        throw new UnsupportedOperationException("read-only benchmark bean");
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException("read-only benchmark bean");
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return mBeanInfo;
    }
}
