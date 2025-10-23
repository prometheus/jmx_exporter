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

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CollidingNameMBean {

    int getValue();
}

class CollidingName implements CollidingNameMBean {

    private final int value;

    public CollidingName(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return 345;
    }

    static void registerBeans(MBeanServer mbs) {
        // Loop through the entire ASCII character set
        for (int i = 0; i < 127; i++) {
            try {
                // Create and try to register an MBean with the name
                ObjectName objectName =
                        new ObjectName(
                                "io.prometheus.jmx.test:type=Colliding" + ((char) i) + "Name");
                CollidingName collidingName = new CollidingName(i);
                mbs.registerMBean(collidingName, objectName);
            } catch (Throwable t) {
                // Ignore since we are testing all ASCII characters, which may not be allowed in an
                // ObjectName
            }
        }
    }
}
