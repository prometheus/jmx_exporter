package com.typingduck.jmmix;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * JMX client that dumps all mBeans from a JMX instance.
 * (only returns values that are numeric or strings).
 */
public class JmxScraper {

    public static interface MBeanFormatter {
        void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value);
    }

    private MBeanFormatter formatter;

    public JmxScraper(MBeanFormatter formatter) {
        this.formatter = formatter;
    }

    /**
      * Get a list of mbeans on host_port and scrape their values.
      */
    public void doScrape(String host_port) throws Exception {
        System.out.println("scrape target: " + host_port);

        // Connect it to the RMI connector server
        String url = "service:jmx:rmi:///jndi/rmi://" + host_port + "/jmxrmi";
        JMXServiceURL serviceUrl = new JMXServiceURL(url);
        JMXConnector jmxc = JMXConnectorFactory.connect(serviceUrl, null);
        MBeanServerConnection beanConn = jmxc.getMBeanServerConnection();

        // Query MBean names
        Set<ObjectName> mBeanNames =
            new TreeSet<ObjectName>(beanConn.queryNames(null, null));

        for (ObjectName name : mBeanNames) {
            scrapeBean(beanConn, name);
        }

        jmxc.close();
    }

    private void scrapeBean(MBeanServerConnection beanConn, ObjectName mbeanName) throws Exception {
        MBeanInfo info = beanConn.getMBeanInfo(mbeanName);
        MBeanAttributeInfo[] attrInfos = info.getAttributes();

        for (int idx = 0; idx < attrInfos.length; ++idx) {
            MBeanAttributeInfo attr = attrInfos[idx];

            if (!attr.isReadable()) {
                logScrape(mbeanName, attr, "not readable");
                continue;
            }

            Object value;
            try {
                value = beanConn.getAttribute(mbeanName, attr.getName());
            } catch(javax.management.RuntimeMBeanException e) {
                logScrape(mbeanName, attr, "Fail: " + e);
                continue;
            } catch(javax.management.RuntimeErrorException e) {
                logScrape(mbeanName, attr, "Fail: " + e);
                continue;
            }

            logScrape(mbeanName, attr, "process");
            processBeanValue(
                    mbeanName.getDomain(),
                    getKeyPropertyList(mbeanName),
                    new LinkedList<String>(),
                    attr.getName(),
                    attr.getType(),
                    attr.getDescription(),
                    value
                    );
        }
    }

    private LinkedHashMap<String, String> getKeyPropertyList(ObjectName mbeanName) {
        // Implement a version of ObjectName.getKeyPropertyList that returns the
        // properties in the ordered they were added (the ObjectName stores them
        // in the order they were added).
        LinkedHashMap<String, String> output = new LinkedHashMap<String, String>();
        String beanName = mbeanName.toString();
        int idx = beanName.indexOf(':') + 1;
        if (idx > 0) {
            String[] tokens = beanName.substring(idx).split(",|=");
            for (int i=0; i<tokens.length-1; )
                output.put(tokens[i++], tokens[i++]);
        }
        return output;
    }

    /**
     * Recursive function for exporting the values of an mBean.
     * JMX is a very open technology, without any prescribed way of declaring mBeans
     * so this function tries to do a best-effort pass of getting the values/names
     * out in a way it can be processed elsewhere easily.
     */
    private void processBeanValue(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        if (value == null) {
            logScrape(domain + beanProperties + attrName, "null");
        } else if (isNumeric(value) || value instanceof String) {
            logScrape(domain + beanProperties + attrName, value.toString());
            this.formatter.recordBean(
                    domain,
                    beanProperties,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value);
        } else if (value instanceof CompositeData) {
            logScrape(domain + beanProperties + attrName, "compositedata");
            CompositeData composite = (CompositeData) value;
            CompositeType type = composite.getCompositeType();
            attrKeys = new LinkedList<String>(attrKeys);
            attrKeys.add(attrName);
            for(String key : type.keySet()) {
                String typ = type.getType(key).getTypeName();
                Object valu = composite.get(key);
                processBeanValue(
                        domain,
                        beanProperties,
                        attrKeys,
                        key,
                        typ,
                        type.getDescription(),
                        valu);
            }
        } else if (value instanceof TabularData) {
            // I don't pretend to have a good understanding of TabularData.
            // The real world usage doesn't appear to match how they were
            // meant to be used according to the docs. I've only seen them
            // used as 'key' 'value' pairs even when 'value' is itself a
            // CompositeData of multiple values.
            logScrape(domain + beanProperties + attrName, "tabulardata");
            TabularData tds = (TabularData) value;
            TabularType tt = tds.getTabularType();

            List<String> rowKeys = tt.getIndexNames();
            LinkedHashMap<String, String> l2s = new LinkedHashMap<String, String>(beanProperties);

            CompositeType type = tt.getRowType();
            Set<String> valueKeys = new TreeSet<String>(type.keySet());
            valueKeys.removeAll(rowKeys);

            LinkedList<String> extendedAttrKeys = new LinkedList<String>(attrKeys);
            extendedAttrKeys.add(attrName);
            for (Object valu : tds.values()) {
                if (valu instanceof CompositeData) {
                    CompositeData composite = (CompositeData) valu;
                    for (String idx : rowKeys) {
                        l2s.put(idx, composite.get(idx).toString());
                    }
                    for(String valueIdx : valueKeys) {
                        LinkedList<String> attrNames = extendedAttrKeys;
                        String typ = type.getType(valueIdx).getTypeName();
                        String name = valueIdx;
                        if (valueIdx.toLowerCase().equals("value")) {
                            // skip appending 'value' to the name
                            attrNames = attrKeys;
                            name = attrName;
                        } 
                        processBeanValue(
                            domain,
                            l2s,
                            attrNames,
                            name,
                            typ,
                            type.getDescription(),
                            composite.get(valueIdx));
                    }
                } else {
                    logScrape(domain, "not a correct tabulardata format");
                }
            }
        } else if (value.getClass().isArray()) {
            logScrape(domain, "arrays are unsupported");
        } else {
            logScrape(domain + beanProperties, attrType + " is not exported");
        }
    }

    public static boolean isNumeric(Object value) {
        return value instanceof Number; 
    }

    /**
     * For debugging.
     */

    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
            logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }
    private static void logScrape(String name, String msg) {
        System.out.println("scrape: '" + name + "': " + msg);
    }

    private static class StdoutWriter implements MBeanFormatter {
        public void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
            System.out.println("Got: " + domain + beanProperties + attrKeys + attrType + ": " + value);
        }
    }

    /**
     * Convenience function to run standalone.
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: JmxScraper <target host:port>");
            System.exit(1);
        }
        String host_port = args[1];
        new JmxScraper(new StdoutWriter()).doScrape(host_port);
        System.out.println("scrape: Results:");
        System.out.println("scrape: ##########################################");
    }

}
