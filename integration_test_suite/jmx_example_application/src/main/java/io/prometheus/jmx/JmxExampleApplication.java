package io.prometheus.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JmxExampleApplication {

  private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
          new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss.SSS", Locale.getDefault());

  public static void main(String[] args) throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    ObjectName tabularMBean = new ObjectName("io.prometheus.jmx:type=tabularData");
    server.registerMBean(new TabularMBean(), tabularMBean);
    System.out.println(
            String.format("%s | %s | INFO | %s | %s",
                    SIMPLE_DATE_FORMAT.format(new Date()),
                    Thread.currentThread().getName(),
                    JmxExampleApplication.class.getName(),
                    "Running"));
    Thread.currentThread().join(); // wait forever
  }
}
