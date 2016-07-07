package io.prometheus.jmx;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface BoolMBean {
    public boolean getTrue();
    public boolean getFalse();
}

class Bool implements BoolMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("boolean:Type=Test");
        Bool mbean = new Bool();
        mbs.registerMBean(mbean, mbeanName);
    }

    public boolean getTrue() {
        return true;
    }

    public boolean getFalse() {
        return false;
    }
}

