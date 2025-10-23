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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

class KafkaClient implements DynamicMBean {

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName =
                new ObjectName(
                        "kafka.consumer:type=consumer-node-metrics,client-id=my-app-consumer,node-id=node-1");
        KafkaClient mbean = new KafkaClient();
        mbs.registerMBean(mbean, mbeanName);
    }

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException {
        switch (attribute) {
            case "request-rate":
                return 2;
            case "request-total":
                return 123;
            default:
                throw new AttributeNotFoundException("Unknown attribute: " + attribute);
        }
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException {
        throw new AttributeNotFoundException("No attributes are writable");
    }

    @Override
    public AttributeList getAttributes(String[] attributes) {
        AttributeList result = new AttributeList();
        for (String attribute : attributes) {
            try {
                Object value = getAttribute(attribute);
                result.add(new Attribute(attribute, value));
            } catch (Exception e) {
                // Skip invalid attributes
            }
        }
        return result;
    }

    @Override
    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList();
    }

    @Override
    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException {
        throw new MBeanException(new UnsupportedOperationException("No operations are supported"));
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        MBeanAttributeInfo[] attributes =
                new MBeanAttributeInfo[] {
                    new MBeanAttributeInfo(
                            "request-rate", "double", "Request rate", true, false, false),
                    new MBeanAttributeInfo(
                            "request-total", "double", "Request total", true, false, false)
                };

        return new MBeanInfo(
                this.getClass().getName(), "Kafka Client MBean", attributes, null, null, null);
    }
}
