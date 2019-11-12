package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CamelMBean {
    long getLastExchangeFailureTimestamp();
}

class Camel implements CamelMBean{
    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName(
                "org.apache.camel:context=my-camel-context,type=routes,name=\"my-route-name\"");
        Camel mbean = new Camel();
        mbs.registerMBean(mbean, mbeanName);
    }

    @Override
    public long getLastExchangeFailureTimestamp() {
        return 1573285945806L;
    }
}
