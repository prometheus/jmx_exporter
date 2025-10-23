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

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface HadoopDataNodeMXBean {

    Map<String, Map<String, Long>> getDatanodeNetworkCounts();
}

class HadoopDataNode implements HadoopDataNodeMXBean {

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("Hadoop:name=DataNodeInfo,service=DataNode");
        HadoopDataNode mbean = new HadoopDataNode();
        mbs.registerMBean(mbean, mbeanName);
    }

    public Map<String, Map<String, Long>> getDatanodeNetworkCounts() {
        Map<String, Long> inner = new HashMap<>();
        inner.put("networkErrors", 338L);
        Map<String, Map<String, Long>> outer = new HashMap<>();
        outer.put("1.2.3.4", inner);
        return outer;
    }
}
