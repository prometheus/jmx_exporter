package io.prometheus.jmx;

import java.io.IOException;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ObjectInstance;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.management.remote.rmi.RMIConnectorServer;


public class JmxScraper {
    private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());;
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "([^,=:\\*\\?]+)" + // Name - non-empty, anything but comma, equals, colon, star, or question mark
            "=" +  // Equals
            "(" + // Either
                "\"" + // Quoted
                    "(?:" + // A possibly empty sequence of
                    "[^\\\\\"]" + // Anything but backslash or quote
                    "|\\\\\\\\" + // or an escaped backslash
                    "|\\\\n" + // or an escaped newline
                    "|\\\\\"" + // or an escaped quote
                    "|\\\\\\?" + // or an escaped question mark
                    "|\\\\\\*" + // or an escaped star
                    ")*" +
                "\"" +
            "|" + // Or
                "[^,=:\"]*" + // Unquoted - can be empty, anything but comma, equals, colon, or quote
            ")");

    public static interface MBeanReceiver {
        void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value);
    }

    private MBeanReceiver receiver;
    private String jmxUrl;
    private String username;
    private String password;
    private List<ObjectName> whitelistObjectNames, blacklistObjectNames;

    public JmxScraper(String jmxUrl, String username, String password, List<ObjectName> whitelistObjectNames, List<ObjectName> blacklistObjectNames, MBeanReceiver receiver) {
        this.jmxUrl = jmxUrl;
        this.receiver = receiver;
        this.username = username;
        this.password = password;
        this.whitelistObjectNames = whitelistObjectNames;
        this.blacklistObjectNames = blacklistObjectNames;
    }

    /**
      * Get a list of mbeans on host_port and scrape their values.
      *
      * Values are passed to the receiver in a single thread.
      */
    public void doScrape() throws Exception {
        MBeanServerConnection beanConn;
        JMXConnector jmxc = null;
        if (jmxUrl.isEmpty()) {
          beanConn = ManagementFactory.getPlatformMBeanServer();
        } else {
          HashMap<String, Object> env = new HashMap<String, Object>();
          if(username != null && username.length() != 0 && password != null && password.length() != 0) {
            String[] credent = new String[] {username, password};
            env.put(javax.management.remote.JMXConnector.CREDENTIALS, credent);
          }       

          SslRMIClientSocketFactory clientSocketFactory = new SslRMIClientSocketFactory();
          env.put(Context.SECURITY_PROTOCOL, "ssl");
          env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFactory);
          env.put("com.sun.jndi.rmi.factory.socket", clientSocketFactory);
          jmxc = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), env);
          beanConn = jmxc.getMBeanServerConnection();
        }
        try {
            // Query MBean names, see #89 for reasons queryMBeans() is used instead of queryNames()
            Set<ObjectInstance> mBeanNames = new HashSet();
            for (ObjectName name : whitelistObjectNames) {
                mBeanNames.addAll(beanConn.queryMBeans(name, null));
            }
            for (ObjectName name : blacklistObjectNames) {
                mBeanNames.removeAll(beanConn.queryMBeans(name, null));
            }
            for (ObjectInstance name : mBeanNames) {
                scrapeBean(beanConn, name.getObjectName());
            }
        } finally {
          if (jmxc != null) {
            jmxc.close();
          }
        }
    }

    private void scrapeBean(MBeanServerConnection beanConn, ObjectName mbeanName) {
        MBeanInfo info;
        try {
          info = beanConn.getMBeanInfo(mbeanName);
        } catch (IOException e) {
          logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
          return;
        } catch (JMException e) {
          logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
          return;
        }
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
            } catch(Exception e) {
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

    static LinkedHashMap<String, String> getKeyPropertyList(ObjectName mbeanName) {
        // Implement a version of ObjectName.getKeyPropertyList that returns the
        // properties in the ordered they were added (the ObjectName stores them
        // in the order they were added).
        LinkedHashMap<String, String> output = new LinkedHashMap<String, String>();
        String properties = mbeanName.getKeyPropertyListString();
        Matcher match = PROPERTY_PATTERN.matcher(properties);
        while (match.lookingAt()) {
            output.put(match.group(1), match.group(2));
            properties = properties.substring(match.end());
            if (properties.startsWith(",")) {
                properties = properties.substring(1);
            }
            match.reset(properties);
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
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean) {
            logScrape(domain + beanProperties + attrName, value.toString());
            this.receiver.recordBean(
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
                            // Skip appending 'value' to the name
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

    /**
     * For debugging.
     */
    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
        logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }
    private static void logScrape(String name, String msg) {
        logger.log(Level.FINE, "scrape: '" + name + "': " + msg);
    }

    private static class StdoutWriter implements MBeanReceiver {
        public void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
            System.out.println(domain +
                               beanProperties + 
                               attrKeys +
                               attrName +
                               ": " + value);
        }
    }

    /**
     * Convenience function to run standalone.
     */
    public static void main(String[] args) throws Exception {
      List<ObjectName> objectNames = new LinkedList<ObjectName>();
      objectNames.add(null);
      if (args.length > 0){
          new JmxScraper(args[0], "", "", objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
      }
      else if (args.length >= 3){
          new JmxScraper(args[0], args[1], args[2], objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
      }
      else {
          new JmxScraper("", "", "", objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
      }
    }
}
