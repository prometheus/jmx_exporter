package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class JmxExampleApplication {

  public static void main(String[] args) throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName tabularMBean = new ObjectName("io.prometheus.jmx:type=tabularData");
    server.registerMBean(new TabularMBean(), tabularMBean);
    System.out.println("registered");
    Thread.currentThread().join(); // wait forever
  }
}
