package io.prometheus.jmx;

import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface HadoopDataNodeMXBean {
    public Map<String, Map<String, Long>> getDatanodeNetworkCounts();
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
