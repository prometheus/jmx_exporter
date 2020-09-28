package io.prometheus.jmx;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularType;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.naming.Context;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import io.prometheus.jmx.JmxMBeanPropertyCache.MBeanAttributeInfoWrapper;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


class JmxScraper {
    private static final Logger logger = Logger.getLogger(JmxScraper.class.getName());


    public static interface MBeanReceiver {
        boolean recordBean(
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value);
    }

    private final String jmxUrl;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final List<ObjectName> whitelistObjectNames, blacklistObjectNames;
    private boolean detectUnusedJmxAttributes;
    private final JmxMBeanPropertyCache jmxMBeanPropertyCache;

    private MBeanServerConnection beanConn;
    private JMXConnector jmxc = null;

    public JmxScraper(String jmxUrl, String username, String password, boolean ssl,
                      List<ObjectName> whitelistObjectNames, List<ObjectName> blacklistObjectNames,
                      JmxMBeanPropertyCache jmxMBeanPropertyCache) {
        this(jmxUrl, username, password, ssl, whitelistObjectNames, blacklistObjectNames, false, jmxMBeanPropertyCache);
    }

    public JmxScraper(String jmxUrl, String username, String password, boolean ssl,
            List<ObjectName> whitelistObjectNames, List<ObjectName> blacklistObjectNames,
            boolean detectUnusedJmxAttributes, JmxMBeanPropertyCache jmxMBeanPropertyCache) {
        this.jmxUrl = jmxUrl;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.whitelistObjectNames = whitelistObjectNames;
        this.blacklistObjectNames = blacklistObjectNames;
        this.detectUnusedJmxAttributes = detectUnusedJmxAttributes;
        this.jmxMBeanPropertyCache = jmxMBeanPropertyCache;
    }

    /**
     * Handles (re)connection to MBean server
     */
    private synchronized void connect() throws IOException, MalformedURLException {
        if (jmxc != null) return;
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
          
          jmxc.addConnectionNotificationListener(new NotificationListener() {
            @Override
            public void handleNotification(Notification notification, Object handback) {
                JMXConnectionNotification not = (JMXConnectionNotification) notification;
                if (JMXConnectionNotification.CLOSED.equals(not.getType())
                        || JMXConnectionNotification.FAILED.equals(not.getType())) {
                    if (logger.isLoggable(Level.FINE)) logger.fine("JMX connection closed. Forcing reconnection.");
                    try {
                        jmxc.close();
                    } catch (Exception e) {
                        // Silence
                    }
                    jmxc = null;
                    beanConn = null;
                }
            }
          }, null, null);
        }
    }

    /**
      * If no connection available, (re)connects.
      */
    private void connectEventually() throws MalformedURLException, IOException {
        if (jmxc == null) {
            connect();
        }
    }

    /**
      * Get a list of mbeans on host_port and scrape their values.
      *
      * Values are passed to the receiver in a single thread.
      */
    public void doScrape(MBeanReceiver receiver) throws Exception {
        connectEventually();

        // Query MBean names, see #89 for reasons queryMBeans() is used instead of queryNames()
        Set<ObjectName> mBeanNames = new HashSet<ObjectName>();
        for (ObjectName name : whitelistObjectNames) {
            for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                mBeanNames.add(instance.getObjectName());
            }
        }

        for (ObjectName name : blacklistObjectNames) {
            for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                mBeanNames.remove(instance.getObjectName());
            }
        }

        // Now that we have *only* the whitelisted mBeans, remove any old ones from the cache:
        jmxMBeanPropertyCache.onlyKeepMBeans(mBeanNames);

        for (ObjectName objectName : mBeanNames) {
            long start = System.nanoTime();
            scrapeBean(beanConn, receiver, objectName);
            if (logger.isLoggable(Level.FINE)) logger.fine("TIME: " + (System.nanoTime() - start) + " ns for " + objectName.toString());
        }
    }

    private void scrapeBean(MBeanServerConnection beanConn, MBeanReceiver receiver, ObjectName mbeanName) {
        Map<String, MBeanAttributeInfoWrapper> name2AttrInfo = jmxMBeanPropertyCache.getAttrInfo(mbeanName);

        if (name2AttrInfo == null) {
            MBeanInfo info;
            try {
              info = beanConn.getMBeanInfo(mbeanName);
            } catch (IOException e) {
              if (logger.isLoggable(Level.FINE)) logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
              return;
            } catch (JMException e) {
              if (logger.isLoggable(Level.FINE)) logScrape(mbeanName.toString(), "getMBeanInfo Fail: " + e);
              return;
            }

            MBeanAttributeInfo[] attrInfos = info.getAttributes();
    
            name2AttrInfo = new LinkedHashMap<String, MBeanAttributeInfoWrapper>();
            for (int idx = 0; idx < attrInfos.length; ++idx) {
                MBeanAttributeInfo attr = attrInfos[idx];
                if (!attr.isReadable()) {
                    if (logger.isLoggable(Level.FINE)) logScrape(mbeanName, attr, "not readable");
                    continue;
                }
                name2AttrInfo.put(attr.getName(), new MBeanAttributeInfoWrapper(attr));
            }

            jmxMBeanPropertyCache.cacheAttrInfo(mbeanName, name2AttrInfo);
        }

        ArrayList<String> attributesNames = new ArrayList<String>();
        for (Map.Entry<String, MBeanAttributeInfoWrapper> entry: name2AttrInfo.entrySet()) {
            if (! this.detectUnusedJmxAttributes || entry.getValue().usedAtLastScrape) attributesNames.add(entry.getKey());
        }

        if (! attributesNames.isEmpty()) {
            final AttributeList attributes;
            try {
                attributes = beanConn.getAttributes(mbeanName, attributesNames.toArray(new String[attributesNames.size()]));
                if (attributes == null) {
                    if (logger.isLoggable(Level.FINE)) logScrape(mbeanName.toString(), "getAttributes Fail: attributes are null");
                    return;
                }
            } catch (Exception e) {
                if (logger.isLoggable(Level.FINE)) logScrape(mbeanName, attributesNames, "Fail: " + e);
                return;
            }
    
            for (Attribute attribute : attributes.asList()) {
                final MBeanAttributeInfoWrapper attrWithActivation = name2AttrInfo.get(attribute.getName());
                MBeanAttributeInfo attr = attrWithActivation.info;
                if (logger.isLoggable(Level.FINE)) logScrape(mbeanName, attr, "process");
                attrWithActivation.usedAtLastScrape = processBeanValue(
                        receiver,
                        mbeanName.getDomain(),
                        jmxMBeanPropertyCache.getKeyPropertyList(mbeanName),
                        new LinkedList<String>(),
                        attr.getName(),
                        attr.getType(),
                        attr.getDescription(),
                        attribute.getValue());
            }
        }
    }



    /**
     * Recursive function for exporting the values of an mBean.
     * JMX is a very open technology, without any prescribed way of declaring mBeans
     * so this function tries to do a best-effort pass of getting the values/names
     * out in a way it can be processed elsewhere easily.
     */
    private boolean processBeanValue(
            MBeanReceiver receiver,
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        boolean result = false;
        if (value == null) {
            if (logger.isLoggable(Level.FINE)) logScrape(domain + beanProperties + attrName, "null");
            result = true;
        } else if (value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof java.util.Date) {
            if (value instanceof java.util.Date) {
                attrType = "java.lang.Double";
                value = ((java.util.Date) value).getTime() / 1000.0;
            }
            if (logger.isLoggable(Level.FINE)) logScrape(domain + beanProperties + attrName, value.toString());
            result = receiver.recordBean(
                    domain,
                    beanProperties,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value);
        } else if (value instanceof CompositeData) {
            if (logger.isLoggable(Level.FINE)) logScrape(domain + beanProperties + attrName, "compositedata");
            CompositeData composite = (CompositeData) value;
            CompositeType type = composite.getCompositeType();
            attrKeys = new LinkedList<String>(attrKeys);
            attrKeys.add(attrName);
            for(String key : type.keySet()) {
                String typ = type.getType(key).getTypeName();
                Object valu = composite.get(key);
                boolean intermediateResult = processBeanValue(
                        receiver,
                        domain,
                        beanProperties,
                        attrKeys,
                        key,
                        typ,
                        type.getDescription(),
                        valu);
                result = intermediateResult || result;
            }
        } else if (value instanceof TabularData) {
            // I don't pretend to have a good understanding of TabularData.
            // The real world usage doesn't appear to match how they were
            // meant to be used according to the docs. I've only seen them
            // used as 'key' 'value' pairs even when 'value' is itself a
            // CompositeData of multiple values.
            if (logger.isLoggable(Level.FINE)) logScrape(domain + beanProperties + attrName, "tabulardata");
            TabularData tds = (TabularData) value;
            TabularType tt = tds.getTabularType();

            List<String> rowKeys = tt.getIndexNames();

            CompositeType type = tt.getRowType();
            Set<String> valueKeys = new TreeSet<String>(type.keySet());
            valueKeys.removeAll(rowKeys);

            LinkedList<String> extendedAttrKeys = new LinkedList<String>(attrKeys);
            extendedAttrKeys.add(attrName);
            for (Object valu : tds.values()) {
                if (valu instanceof CompositeData) {
                    CompositeData composite = (CompositeData) valu;
                    LinkedHashMap<String, String> l2s = new LinkedHashMap<String, String>(beanProperties);
                    for (String idx : rowKeys) {
                        Object obj = composite.get(idx);
                        if (obj != null) {
                            // Nested tabulardata will repeat the 'key' label, so
                            // append a suffix to distinguish each.
                            while (l2s.containsKey(idx)) {
                              idx = idx + "_";
                            }
                            l2s.put(idx, obj.toString());
                        }
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
                        boolean intermediateResult = processBeanValue(
                            receiver,
                            domain,
                            l2s,
                            attrNames,
                            name,
                            typ,
                            type.getDescription(),
                            composite.get(valueIdx));
                        result = intermediateResult || result;
                    }
                } else {
                    if (logger.isLoggable(Level.FINE)) logScrape(domain, "not a correct tabulardata format");
                }
            }
        } else if (value.getClass().isArray()) {
            if (logger.isLoggable(Level.FINE)) logScrape(domain, "arrays are unsupported");
            result = false;
        } else {
            if (logger.isLoggable(Level.FINE)) logScrape(domain + beanProperties, attrType + " is not exported");
            result = false;
        }
        return result;
    }

    /**
     * For debugging.
     */
    private static void logScrape(ObjectName mbeanName, Collection<String> names, String msg) {
        logScrape(mbeanName + "_" + names, msg);
    }
    private static void logScrape(ObjectName mbeanName, MBeanAttributeInfo attr, String msg) {
        logScrape(mbeanName + "'_'" + attr.getName(), msg);
    }
    private static void logScrape(String name, String msg) {
        logger.log(Level.FINE, "scrape: '" + name + "': " + msg);
    }

    private static class StdoutWriter implements MBeanReceiver {
        public boolean recordBean(
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
            return true;
        }
    }

    /**
     * Convenience function to run standalone.
     */
    public static void main(String[] args) throws Exception {
      List<ObjectName> objectNames = new LinkedList<ObjectName>();
      objectNames.add(null);
      if (args.length >= 3){
            new JmxScraper(args[0], args[1], args[2], false, objectNames, new LinkedList<ObjectName>(),
                  new JmxMBeanPropertyCache()).doScrape(new StdoutWriter());
        }
      else if (args.length > 0){
          new JmxScraper(args[0], "", "", false, objectNames, new LinkedList<ObjectName>(),
                  new JmxMBeanPropertyCache()).doScrape(new StdoutWriter());
      }
      else {
          new JmxScraper("", "", "", false, objectNames, new LinkedList<ObjectName>(),
                  new JmxMBeanPropertyCache()).doScrape(new StdoutWriter());
      }
    }
}

