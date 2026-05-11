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

/**
 * Implements an MBean that exposes a fixed text string value.
 *
 * <p>Used in integration testing to verify scraping of string-typed MBean attributes.
 */
public class StringValue implements StringValueMBean {

    /**
     * Constructs a new instance.
     */
    public StringValue() {
        // INTENTIONALLY BLANK
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "value";
    }

    /**
     * Registers a new {@link StringValue} MBean with the platform MBean server under the object name
     * {@code io.prometheus.jmx:type=stringValue}.
     *
     * @throws Exception if MBean registration fails
     */
    public void register() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        mBeanServer.registerMBean(new StringValue(), new ObjectName("io.prometheus.jmx:type=stringValue"));
    }
}
