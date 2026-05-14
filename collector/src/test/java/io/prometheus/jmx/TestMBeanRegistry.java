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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;

public class TestMBeanRegistry {

    private static final AtomicBoolean registered = new AtomicBoolean(false);

    private TestMBeanRegistry() {}

    public static synchronized void registerTestMBeans() throws Exception {
        if (registered.getAndSet(true)) {
            return;
        }

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        CollidingName.registerBeans(mbs);
        PerformanceMetrics.registerBean(mbs);
        TotalValue.registerBean(mbs);
        Cassandra.registerBean(mbs);
        CassandraMetrics.registerBean(mbs);
        Hadoop.registerBean(mbs);
        HadoopDataNode.registerBean(mbs);
        ExistDb.registerBean(mbs);
        BeanWithEnum.registerBean(mbs);
        TomcatServlet.registerBean(mbs);
        Bool.registerBean(mbs);
        Camel.registerBean(mbs);
        CustomValue.registerBean(mbs);
        StringValue.registerBean(mbs);
        KafkaClient.registerBean(mbs);
        DateValue.registerBean(mbs);
        NullValue.registerBean(mbs);
        ArrayValue.registerBean(mbs);
        OptionalValue.registerBean(mbs);
        UnsupportedType.registerBean(mbs);
    }
}
