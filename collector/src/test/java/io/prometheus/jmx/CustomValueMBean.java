/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/** Class to implement CustomValueMBean */
public interface CustomValueMBean {

    /**
     * Method to get the value
     *
     * @return value
     */
    Integer getValue();

    /**
     * Method to get the text
     *
     * @return text
     */
    String getText();
}

/** Class to implement CustomValue */
class CustomValue implements CustomValueMBean {

    @Override
    public Integer getValue() {
        return 345;
    }

    @Override
    public String getText() {
        return "value";
    }

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("io.prometheus.jmx:type=customValue");
        CustomValueMBean mbean = new CustomValue();
        mbs.registerMBean(mbean, mbeanName);
    }
}
