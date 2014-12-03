package io.prometheus.jmx;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CassandraMBean {
    public int getActiveCount();
}

class Cassandra implements CassandraMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName(
                "org.apache.cassandra.concurrent:type=CONSISTENCY-MANAGER");
        Cassandra mbean = new Cassandra();
        mbs.registerMBean(mbean, mbeanName);
    }

    public int getActiveCount() {
        return 100;
    }
}

