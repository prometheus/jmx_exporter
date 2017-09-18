package io.prometheus.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.naming.Context;
import javax.rmi.ssl.SslRMIClientSocketFactory;


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

    interface MBeanReceiver {
        void recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value);
    }

    interface MBeanHandlerProvider {

        /**
         * @return null if the mbean is not required by any rule
         */
        MBeanHandler findMBeanHandler(String domain, LinkedHashMap<String, String> beanProperties);

    }

    interface MBeanHandler {
        /**
         * @return null if the attribute is not required by any rule
         */
        MBeanReceiver findReceiver(String attrName);

    }


    private MBeanHandlerProvider receiver;
    private String jmxUrl;
    private String username;
    private String password;
    private boolean ssl;
    private List<ObjectName> whitelistObjectNames, blacklistObjectNames;
    private Map<ObjectName, MBeanInfo> mBeanInfoCache = null;


    public JmxScraper(String jmxUrl, String username, String password, boolean ssl, List<ObjectName> whitelistObjectNames, List<ObjectName> blacklistObjectNames, MBeanHandlerProvider receiver) {
        this.jmxUrl = jmxUrl;
        this.receiver = receiver;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.whitelistObjectNames = whitelistObjectNames;
        this.blacklistObjectNames = blacklistObjectNames;
    }

    public void setBeanInfoCache(Map<ObjectName, MBeanInfo> mBeanInfoCache) {
        this.mBeanInfoCache = mBeanInfoCache;
    }

    /**
      * Get a list of mbeans on host_port and scrape their values.
      *
      * Values are passed to the receiver in a single thread.
      */
    public int doScrape() throws Exception {
        MBeanServerConnection beanConn;
        JMXConnector jmxc = null;
        if (jmxUrl.isEmpty()) {
          beanConn = ManagementFactory.getPlatformMBeanServer();
        } else {
          Map<String, Object> environment = new HashMap<String, Object>();
          if (username != null && username.length() != 0 && password != null && password.length() != 0) {
            String[] credent = new String[] {username, password};
            environment.put(javax.management.remote.JMXConnector.CREDENTIALS, credent);
          }
          if (ssl) {
              environment.put(Context.SECURITY_PROTOCOL, "ssl");
              SslRMIClientSocketFactory clientSocketFactory = new SslRMIClientSocketFactory();
              environment.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFactory);
              environment.put("com.sun.jndi.rmi.factory.socket", clientSocketFactory);
          }

          jmxc = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), environment);
          beanConn = jmxc.getMBeanServerConnection();
        }
        int attributeCount = 0;
        try {
            Set<ObjectName> mBeanNames = new HashSet<ObjectName>();
            // Query MBean names, see #89 for reasons queryMBeans() is used instead of queryNames()
            for (ObjectName name : whitelistObjectNames) {
                if (name == null || name.isPattern()) {
                    mBeanNames.addAll(getObjectInstances(beanConn, name));
                } else {
                    mBeanNames.add(name);
                }
            }
            for (ObjectName name : blacklistObjectNames) {
                mBeanNames.removeAll(getObjectInstances(beanConn, name));
            }
            for (ObjectName name : mBeanNames) {
                long start = System.nanoTime();
                attributeCount += scrapeBeanIfNecessary(beanConn, name);
                logger.fine("TIME: " + (System.nanoTime() - start) + " ns for " + name.toString());
            }
            return attributeCount;
        } finally {
          if (jmxc != null) {
            jmxc.close();
          }
        }
    }

    private List<ObjectName> getObjectInstances(MBeanServerConnection beanConn, ObjectName name) throws IOException {
        List<ObjectName> res = new LinkedList<ObjectName>();
        for (ObjectInstance re : beanConn.queryMBeans(name, null)) {
            res.add(re.getObjectName());
        }
        return res;
    }

    private int scrapeBeanIfNecessary(MBeanServerConnection beanConn, ObjectName mbeanName) {
        MBeanHandler mBeanHandler = this.receiver.findMBeanHandler(mbeanName.getDomain(), getKeyPropertyList(mbeanName));
        if (mBeanHandler != null) {
            return scrapeBean(beanConn, mbeanName, mBeanHandler);
        } else {
            logScrape(mbeanName.toString(), "Ignoring");
            return 0;
        }
    }

    private MBeanAttributeInfo getAttributeInfo(MBeanAttributeInfo[] attrInfos, String name) {
        for (MBeanAttributeInfo attrInfo : attrInfos) {
            if (attrInfo.getName().equals(name)) {
                return attrInfo;
            }
        }
        throw new IllegalStateException("No attribute found with name " + name + " in " + Arrays.toString(attrInfos));
    }

    private MBeanInfo getMBeanInfo(MBeanServerConnection beanConn, ObjectName mbeanName) {
        if (mBeanInfoCache != null) {
            MBeanInfo cachedValue = mBeanInfoCache.get(mbeanName);
            if (cachedValue != null) {
                return cachedValue;
            }
        }
        try {
            final MBeanInfo info = beanConn.getMBeanInfo(mbeanName);
            if (mBeanInfoCache != null) {
                mBeanInfoCache.put(mbeanName, info);
            }
            return info;
        } catch (IOException e) {
            logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
            return null;
        } catch (JMException e) {
            logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
            return null;
        }
    }
    private int scrapeBean(MBeanServerConnection beanConn, ObjectName mbeanName, MBeanHandler mbeanHandler) {
        MBeanInfo info = getMBeanInfo(beanConn, mbeanName);
        if (info == null) {
            return 0;
        }
        MBeanAttributeInfo[] attrInfos = info.getAttributes();
        int retrievedAttributeCount = 0;
        Map<String, MBeanReceiver> attributeReceivers = new LinkedHashMap<String, MBeanReceiver>();
        for (MBeanAttributeInfo attr : attrInfos) {
            if (!attr.isReadable()) {
                logScrape(mbeanName, attr, "not readable");
                continue;
            }
            MBeanReceiver attributeHandler = mbeanHandler.findReceiver(attr.getName());
            if (attributeHandler != null) {
                attributeReceivers.put(attr.getName(), attributeHandler);
                retrievedAttributeCount++;
            } else {
                logScrape(mbeanName, attr, "ignoring");
            }
        }
        String[] attributes = attributeReceivers.keySet().toArray(new String[attributeReceivers.size()]);
        if (attributeReceivers.size() > 0) {
            try {
                //get all attributes in one get
                AttributeList attributeList = beanConn.getAttributes(mbeanName, attributes);
                for (Object valueObject : attributeList) {
                    Attribute attributeValue = (Attribute) valueObject;
                    MBeanReceiver attributeHandler = attributeReceivers.get(attributeValue.getName());
                    MBeanAttributeInfo attr = getAttributeInfo(attrInfos, attributeValue.getName());
                    Object value = attributeValue.getValue();
                    logScrape(mbeanName, attr, "process");
                    processBeanValue(
                        attributeHandler,
                        mbeanName.getDomain(),
                        getKeyPropertyList(mbeanName),
                        new LinkedList<String>(),
                        attr.getName(),
                        attr.getType(),
                        attr.getDescription(),
                        value
                    );
                }
            } catch (Exception e) {
                logScrape(mbeanName, Arrays.toString(attributes), "Fail: " + e);
            }
        }
        return retrievedAttributeCount;
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
            MBeanReceiver attributeReceiver,
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
            attributeReceiver.recordBean(
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
                        attributeReceiver,
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
                            attributeReceiver,
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
        logScrape(mbeanName, attr.getName(), msg);
    }

    private static void logScrape(ObjectName mbeanName, String attr, String msg) {
        logScrape(mbeanName + "'_'" + attr, msg);
    }
    private static void logScrape(String name, String msg) {
        logger.log(Level.FINE, "scrape: '" + name + "': " + msg);
    }

    private static class StdoutWriter implements MBeanReceiver, MBeanHandler, MBeanHandlerProvider {

        public MBeanHandler findMBeanHandler(String domain, LinkedHashMap<String, String> beanProperties) {
            return this;
        }

        public MBeanReceiver findReceiver(String attrName) {
            return this;
        }

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
      if (args.length >= 3){
            new JmxScraper(args[0], args[1], args[2], false, objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
        }
      else if (args.length > 0){
          new JmxScraper(args[0], "", "", false, objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
      }
      else {
          new JmxScraper("", "", "", false, objectNames, new LinkedList<ObjectName>(), new StdoutWriter()).doScrape();
      }
    }
}

