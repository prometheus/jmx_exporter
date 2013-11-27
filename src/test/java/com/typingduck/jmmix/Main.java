/*
 * Main.java - main class for the sample MBeans.
 * Exposes some some example mbeans that would want to be monitored
 * e.g. mbeans from Cassandra and Hadoop.
 */
package com.typingduck.jmmix;

import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class Main {

    public static void main(String[] args) throws Exception {

        // Get the Platform MBean Server
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Register the MBeans
        Cassandra.registerBean(mbs);
        CassandraMetrics.registerBean(mbs);

        Hadoop.registerBean(mbs);

        // Wait forever
        System.out.println("Sample mbean application listening...");
        Thread.sleep(Long.MAX_VALUE);
    }
}

