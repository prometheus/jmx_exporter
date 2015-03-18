package io.prometheus.jmx;

import javax.management.*;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public interface TomcatServletMBean {
    public int getRequestCount();
}

class TomcatServlet implements TomcatServletMBean {

    public static void registerBean(MBeanServer mbs)
            throws javax.management.JMException {
        ObjectName mbeanName = new ObjectName(
                "Catalina:j2eeType=Servlet,WebModule=//localhost/host-manager,name=HTMLHostManager,J2EEApplication=none,J2EEServer=none");
        TomcatServlet mbean = new TomcatServlet();
        mbs.registerMBean(mbean, mbeanName);
    }

    public int getRequestCount() {
        return 1;
    }
}
