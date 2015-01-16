package io.prometheus.jmx;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CassandraMetricsMBean {
    // This how yammer's metrics
    // http://www.javacodegeeks.com/2012/12/yammer-metrics-a-new-way-to-monitor-your-application.html
    // look through JMX.
    public float getValue();
}

class CassandraMetrics implements CassandraMetricsMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName(
                "org.apache.cassandra.metrics:type=Compaction,name=CompletedTasks");
        CassandraMetricsMBean mbean = new CassandraMetrics();
        mbs.registerMBean(mbean, mbeanName);
    }

    public float getValue() {
        return 0.2f;
    }
}

