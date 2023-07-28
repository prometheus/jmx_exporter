package io.prometheus.jmx;

import java.util.Date;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface CamelMBean {
    double EXPECTED_SECONDS = 1.573285945111E9;

    Date getLastExchangeFailureTimestamp();
}

class Camel implements CamelMBean {
    public static void registerBean(MBeanServer mbs) throws javax.management.JMException {
        ObjectName mbeanName =
                new ObjectName(
                        "org.apache.camel:context=my-camel-context,type=routes,name=\"my-route-name\"");
        Camel mbean = new Camel();
        mbs.registerMBean(mbean, mbeanName);
    }

    @Override
    public Date getLastExchangeFailureTimestamp() {
        return new Date((long) (EXPECTED_SECONDS * 1000));
    }
}
