package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface BeanWithEnumMBean {
    public State getState();
}

class BeanWithEnum implements BeanWithEnumMBean {

    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName("org.bean.enum:type=StateMetrics");
        BeanWithEnum mbean = new BeanWithEnum();
        mbs.registerMBean(mbean, mbeanName);
    }

    public State getState() {
        return State.RUNNING;
    }
}
