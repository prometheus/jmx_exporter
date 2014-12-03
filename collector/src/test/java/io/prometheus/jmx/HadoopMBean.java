package io.prometheus.jmx;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface HadoopMBean {
    public int getreplaceBlockOpMinTime();
}

class Hadoop implements HadoopMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName(
                "hadoop:service=DataNode,name=DataNodeActivity-ams-hdd001-50010");
        Hadoop mbean = new Hadoop();
        mbs.registerMBean(mbean, mbeanName);
    }

    public int getreplaceBlockOpMinTime() {
        return 200;
    }
}

