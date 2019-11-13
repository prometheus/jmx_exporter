package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.util.Date;

public interface CamelMBean {
    long EXPECTED_DATE_IN_MILLISECONDS = 1573285945111L;
    Date getLastExchangeFailureTimestamp();
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
    public Date getLastExchangeFailureTimestamp() {
        return new Date(EXPECTED_DATE_IN_MILLISECONDS);
    }
}
