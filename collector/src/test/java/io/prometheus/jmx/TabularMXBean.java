package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.Map;

public interface TabularMXBean {
    Map<String, String> getTable();

    int getFoo();
}

class Tabular implements TabularMXBean {
    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("tabular:type=Test");
        Tabular mbean = new Tabular();
        mbs.registerMBean(mbean, mbeanName);
    }

    public Map<String, String> getTable() {
        return Collections.singletonMap(null, "null key");
    }

    public int getFoo() {
        return 1;
    }
}
