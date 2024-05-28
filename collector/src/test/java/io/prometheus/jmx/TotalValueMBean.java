package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface TotalValueMBean {

    int getTotal();
}

class TotalValue implements TotalValueMBean {

    public TotalValue() {
        // DO NOTHING
    }

    @Override
    public int getTotal() {
        return 345;
    }

    static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mxbeanName = new ObjectName("io.prometheus.jmx.test:type=Total-Value");
        TotalValue mxbean = new TotalValue();
        mbs.registerMBean(mxbean, mxbeanName);

        mxbeanName = new ObjectName("io.prometheus.jmx.test:type=Total.Value");
        mxbean = new TotalValue();
        mbs.registerMBean(mxbean, mxbeanName);
    }
}
