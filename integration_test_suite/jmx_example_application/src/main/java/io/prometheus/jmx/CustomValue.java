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
import javax.management.MBeanServer;
import javax.management.ObjectName;

/** Class to implement CustomValue */
public class CustomValue implements CustomValueMBean {

    /** Constructor */
    public CustomValue() {
        // INTENTIONALLY BLANK
    }

    @Override
    public Integer getValue() {
        return 345;
    }

    @Override
    public String getText() {
        return "value";
    }

    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(
                new CustomValue(), new ObjectName("io.prometheus.jmx:type=customValue"));
    }
}
