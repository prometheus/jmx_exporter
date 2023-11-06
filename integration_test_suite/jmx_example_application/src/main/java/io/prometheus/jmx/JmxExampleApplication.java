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

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.Unit;
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

        ObjectName tabularMBean = new ObjectName("io.prometheus.jmx:type=tabularData");
        mBeanServer.registerMBean(new TabularMBean(), tabularMBean);

        ObjectName autoIncrementingMBan = new ObjectName("io.prometheus.jmx:type=autoIncrementing");
        mBeanServer.registerMBean(new AutoIncrementing(), autoIncrementingMBan);

        ObjectName existDbMXBean = new ObjectName("org.exist.management.exist:type=ProcessReport");
        mBeanServer.registerMBean(new ExistDb(), existDbMXBean);

        ObjectName optionalValueMBean = new ObjectName("io.prometheus.jmx:type=optionalValue");
        mBeanServer.registerMBean(new OptionalValue(), optionalValueMBean);

        Counter serviceTimeSeconds =
                Counter.builder()
                        .name("service_time_seconds_total")
                        .help("total time spent serving requests")
                        .unit(Unit.SECONDS)
                        .register();

        serviceTimeSeconds.inc(Unit.millisToSeconds(200));

        Gauge temperature =
                Gauge.builder()
                        .name("temperature_celsius")
                        .help("current temperature")
                        .labelNames("location")
                        .unit(Unit.CELSIUS)
                        .register();

        temperature.labelValues("Berlin").set(22.3);

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
