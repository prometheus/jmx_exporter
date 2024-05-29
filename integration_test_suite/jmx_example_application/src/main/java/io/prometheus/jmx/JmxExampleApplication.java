/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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

import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class JmxExampleApplication {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss.SSS", Locale.getDefault());

    public static void main(String[] args) throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        mBeanServer.registerMBean(
                new TabularMBean(), new ObjectName("io.prometheus.jmx:type=tabularData"));

        mBeanServer.registerMBean(
                new AutoIncrementing(), new ObjectName("io.prometheus.jmx:type=autoIncrementing"));

        mBeanServer.registerMBean(
                new ExistDb(), new ObjectName("org.exist.management.exist:type=ProcessReport"));

        mBeanServer.registerMBean(
                new OptionalValue(), new ObjectName("io.prometheus.jmx:type=optionalValue"));

        mBeanServer.registerMBean(
                new PerformanceMetrics(),
                new ObjectName("io.prometheus.jmx.test:name=PerformanceMetricsMBean"));

        System.out.println(
                String.format(
                        "%s | %s | INFO | %s | %s",
                        SIMPLE_DATE_FORMAT.format(new Date()),
                        Thread.currentThread().getName(),
                        JmxExampleApplication.class.getName(),
                        "Running"));

        Thread.currentThread().join(); // wait forever
    }
}
