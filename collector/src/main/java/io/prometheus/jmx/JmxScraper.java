/*
 * Copyright (C) 2015-2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx;

import static java.util.logging.Level.FINE;

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
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

class JmxScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxScraper.class);

    public interface MBeanReceiver {
        void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value);
    }

    private final MBeanReceiver receiver;
    private final String jmxUrl;
    private final String username;
    private final String password;
    private final boolean ssl;
    private final List<ObjectName> includeObjectNames, excludeObjectNames;
    private final ObjectNameAttributeFilter objectNameAttributeFilter;
    private final JmxMBeanPropertyCache jmxMBeanPropertyCache;

    public JmxScraper(
            String jmxUrl,
            String username,
            String password,
            boolean ssl,
            List<ObjectName> includeObjectNames,
            List<ObjectName> excludeObjectNames,
            ObjectNameAttributeFilter objectNameAttributeFilter,
            MBeanReceiver receiver,
            JmxMBeanPropertyCache jmxMBeanPropertyCache) {
        this.jmxUrl = jmxUrl;
        this.receiver = receiver;
        this.username = username;
        this.password = password;
        this.ssl = ssl;
        this.includeObjectNames = includeObjectNames;
        this.excludeObjectNames = excludeObjectNames;
        this.objectNameAttributeFilter = objectNameAttributeFilter;
        this.jmxMBeanPropertyCache = jmxMBeanPropertyCache;
    }

    /**
     * Get a list of mbeans on host_port and scrape their values.
     *
     * <p>Values are passed to the receiver in a single thread.
     */
    public void doScrape() throws Exception {
        MBeanServerConnection beanConn;
        JMXConnector jmxc = null;
        if (jmxUrl.isEmpty()) {
            beanConn = ManagementFactory.getPlatformMBeanServer();
        } else {
            Map<String, Object> environment = new HashMap<>();
            if (username != null
                    && username.length() != 0
                    && password != null
                    && password.length() != 0) {
                String[] credent = new String[] {username, password};
                environment.put(javax.management.remote.JMXConnector.CREDENTIALS, credent);
            }
            if (ssl) {
                environment.put(Context.SECURITY_PROTOCOL, "ssl");
                SslRMIClientSocketFactory clientSocketFactory = new SslRMIClientSocketFactory();
                environment.put(
                        RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                        clientSocketFactory);
                environment.put("com.sun.jndi.rmi.factory.socket", clientSocketFactory);
            }

            jmxc = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), environment);
            beanConn = jmxc.getMBeanServerConnection();
        }
        try {
            // Query MBean names, see #89 for reasons queryMBeans() is used instead of queryNames()
            Set<ObjectName> mBeanNames = new HashSet<>();
            for (ObjectName name : includeObjectNames) {
                for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                    mBeanNames.add(instance.getObjectName());
                }
            }

            for (ObjectName name : excludeObjectNames) {
                for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                    mBeanNames.remove(instance.getObjectName());
                }
            }

            // Now that we have *only* the whitelisted mBeans, remove any old ones from the cache:
            jmxMBeanPropertyCache.onlyKeepMBeans(mBeanNames);

            for (ObjectName objectName : mBeanNames) {
                long start = System.nanoTime();
                scrapeBean(beanConn, objectName);
                LOGGER.log(FINE, "TIME: %d ns for %s", System.nanoTime() - start, objectName);
            }
        } finally {
            if (jmxc != null) {
                jmxc.close();
            }
        }
    }

    private void scrapeBean(MBeanServerConnection beanConn, ObjectName mBeanName) {
        MBeanInfo mBeanInfo;

        try {
            mBeanInfo = beanConn.getMBeanInfo(mBeanName);
        } catch (IOException e) {
            LOGGER.log(FINE, "%s getMBeanInfo Fail: %s", mBeanName, e);
            return;
        } catch (JMException e) {
            LOGGER.log(FINE, "%s getMBeanInfo Fail: %s", mBeanName, e.getMessage());
            return;
        }

        MBeanAttributeInfo[] mBeanAttributeInfos = mBeanInfo.getAttributes();

        Map<String, MBeanAttributeInfo> name2MBeanAttributeInfo = new LinkedHashMap<>();
        for (MBeanAttributeInfo mBeanAttributeInfo : mBeanAttributeInfos) {
            if (!mBeanAttributeInfo.isReadable()) {
                LOGGER.log(FINE, "%s_%s not readable", mBeanName, mBeanAttributeInfo.getName());
                continue;
            }

            if (objectNameAttributeFilter.exclude(mBeanName, mBeanAttributeInfo.getName())) {
                continue;
            }

            name2MBeanAttributeInfo.put(mBeanAttributeInfo.getName(), mBeanAttributeInfo);
        }

        if (name2MBeanAttributeInfo.isEmpty()) {
            return;
        }

        AttributeList attributes;

        try {
            // bulk load all attributes
            attributes =
                    beanConn.getAttributes(
                            mBeanName, name2MBeanAttributeInfo.keySet().toArray(new String[0]));
            if (attributes == null) {
                LOGGER.log(FINE, "%s getMBeanInfo Fail: attributes are null", mBeanName);
                return;
            }
        } catch (Exception e) {
            LOGGER.log(
                    FINE,
                    "%s getAttributes Fail: processing one by one: %s",
                    mBeanName,
                    e.getMessage());

            // couldn't get them all in one go, try them 1 by 1
            processAttributesOneByOne(beanConn, mBeanName, name2MBeanAttributeInfo);
            return;
        }

        final String mBeanNameString = mBeanName.toString();

        for (Object object : attributes) {
            // The contents of an AttributeList should all be Attribute instances, but we'll verify
            // that.
            if (object instanceof Attribute) {
                Attribute attribute = (Attribute) object;
                String attributeName = attribute.getName();
                if (mBeanNameString.equals("java.lang:type=Runtime")
                        && (attributeName.equalsIgnoreCase("SystemProperties")
                                || attributeName.equalsIgnoreCase("ClassPath")
                                || attributeName.equalsIgnoreCase("BootClassPath")
                                || attributeName.equalsIgnoreCase("LibraryPath"))) {
                    // Skip this attributes for the "java.lang:type=Runtime" MBean because
                    // getting the values is expensive and the values are ultimately ignored
                    continue;
                } else if (mBeanNameString.equals("jdk.management.jfr:type=FlightRecorder")) {
                    // Skip the FlightRecorderMXBean
                    continue;
                }

                MBeanAttributeInfo mBeanAttributeInfo =
                        name2MBeanAttributeInfo.get(attribute.getName());
                LOGGER.log(FINE, "%s_%s process", mBeanName, mBeanAttributeInfo.getName());
                processBeanValue(
                        mBeanName,
                        mBeanName.getDomain(),
                        jmxMBeanPropertyCache.getKeyPropertyList(mBeanName),
                        new LinkedList<>(),
                        mBeanAttributeInfo.getName(),
                        mBeanAttributeInfo.getType(),
                        mBeanAttributeInfo.getDescription(),
                        attribute.getValue());
            } else if (object == null) {
                LOGGER.log(
                        FINE,
                        "%s object is NULL, not an instance javax.management.Attribute, skipping",
                        mBeanName);
            } else {
                LOGGER.log(
                        FINE,
                        "%s object [%s] isn't an instance javax.management.Attribute, skipping",
                        mBeanName,
                        object.getClass().getName());
            }
        }
    }

    private void processAttributesOneByOne(
            MBeanServerConnection beanConn,
            ObjectName mbeanName,
            Map<String, MBeanAttributeInfo> name2AttrInfo) {
        Object value;
        for (MBeanAttributeInfo attr : name2AttrInfo.values()) {
            try {
                value = beanConn.getAttribute(mbeanName, attr.getName());
            } catch (Exception e) {
                LOGGER.log(FINE, "%s_%s Fail: %s", mbeanName, attr.getName(), e.getMessage());
                continue;
            }

            LOGGER.log(FINE, "%s_%s process", mbeanName, attr.getName());
            processBeanValue(
                    mbeanName,
                    mbeanName.getDomain(),
                    jmxMBeanPropertyCache.getKeyPropertyList(mbeanName),
                    new LinkedList<>(),
                    attr.getName(),
                    attr.getType(),
                    attr.getDescription(),
                    value);
        }
    }

    /**
     * Recursive function for exporting the values of an mBean. JMX is a very open technology,
     * without any prescribed way of declaring mBeans so this function tries to do a best-effort
     * pass of getting the values/names out in a way it can be processed elsewhere easily.
     */
    private void processBeanValue(
            ObjectName objectName,
            String domain,
            LinkedHashMap<String, String> beanProperties,
            LinkedList<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        if (value == null) {
            LOGGER.log(FINE, "%s%s%s scrape: null", domain, beanProperties, attrName);
        } else if (value instanceof Number
                || value instanceof String
                || value instanceof Boolean
                || value instanceof java.util.Date) {
            if (value instanceof java.util.Date) {
                attrType = "java.lang.Double";
                value = ((java.util.Date) value).getTime() / 1000.0;
            }
            LOGGER.log(FINE, "%s%s%s scrape: %s", domain, beanProperties, attrName, value);
            this.receiver.recordBean(
                    domain, beanProperties, attrKeys, attrName, attrType, attrDescription, value);
        } else if (value instanceof CompositeData) {
            LOGGER.log(FINE, "%s%s%s scrape: compositedata", domain, beanProperties, attrName);
            CompositeData composite = (CompositeData) value;
            CompositeType type = composite.getCompositeType();
            attrKeys = new LinkedList<>(attrKeys);
            attrKeys.add(attrName);
            for (String key : type.keySet()) {
                String typ = type.getType(key).getTypeName();
                Object valu = composite.get(key);
                processBeanValue(
                        objectName,
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
            LOGGER.log(FINE, "%s%s%s scrape: tabulardata", domain, beanProperties, attrName);
            TabularData tds = (TabularData) value;
            TabularType tt = tds.getTabularType();

            List<String> rowKeys = tt.getIndexNames();

            CompositeType type = tt.getRowType();
            Set<String> valueKeys = new TreeSet<>(type.keySet());
            rowKeys.forEach(valueKeys::remove);

            LinkedList<String> extendedAttrKeys = new LinkedList<>(attrKeys);
            extendedAttrKeys.add(attrName);
            for (Object valu : tds.values()) {
                if (valu instanceof CompositeData) {
                    CompositeData composite = (CompositeData) valu;
                    LinkedHashMap<String, String> l2s = new LinkedHashMap<>(beanProperties);
                    for (String idx : rowKeys) {
                        Object obj = composite.get(idx);
                        if (obj != null) {

                            // Nested tabulardata will repeat the 'key' label, so
                            // append a suffix to distinguish each.
                            while (l2s.containsKey(idx)) {
                                idx = idx + "_";
                            }

                            if (obj instanceof CompositeData) {
                                // TabularData key is a composite key
                                CompositeData compositeKey = (CompositeData) obj;
                                CompositeType ct = compositeKey.getCompositeType();
                                for (final String compositeKeyIdx : ct.keySet()) {
                                    l2s.put(
                                            idx + "_" + compositeKeyIdx,
                                            compositeKey.get(compositeKeyIdx).toString());
                                }
                            } else {
                                // TabularData key is an Open type key
                                l2s.put(idx, obj.toString());
                            }
                        }
                    }
                    for (String valueIdx : valueKeys) {
                        LinkedList<String> attrNames = extendedAttrKeys;
                        String typ = type.getType(valueIdx).getTypeName();
                        String name = valueIdx;
                        if (valueIdx.equalsIgnoreCase("value")) {
                            // Skip appending 'value' to the name
                            attrNames = attrKeys;
                            name = attrName;
                        }
                        processBeanValue(
                                objectName,
                                domain,
                                l2s,
                                attrNames,
                                name,
                                typ,
                                type.getDescription(),
                                composite.get(valueIdx));
                    }
                } else {
                    LOGGER.log(FINE, "%s scrape: not a correct tabulardata format", domain);
                }
            }
        } else if (value.getClass().isArray()) {
            LOGGER.log(FINE, "%s scrape: arrays are unsupported", domain);
        } else if (value instanceof Optional) {
            LOGGER.log(FINE, "%s%s%s scrape: java.util.Optional", domain, beanProperties, attrName);
            Optional<?> optional = (Optional<?>) value;
            if (optional.isPresent()) {
                processBeanValue(
                        objectName,
                        domain,
                        beanProperties,
                        attrKeys,
                        attrName,
                        attrType,
                        attrDescription,
                        optional.get());
            }
        } else if (value.getClass().isEnum()) {
            LOGGER.log(FINE, "%s%s%s scrape: %s", domain, beanProperties, attrName, value);
            processBeanValue(
                    objectName,
                    domain,
                    beanProperties,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value.toString());
        } else {
            objectNameAttributeFilter.add(objectName, attrName);
            LOGGER.log(FINE, "%s%s scrape: %s not exported", domain, beanProperties, attrType);
        }
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
            System.out.println(domain + beanProperties + attrKeys + attrName + ": " + value);
        }
    }

    /** Convenience function to run standalone. */
    public static void main(String[] args) throws Exception {
        ObjectNameAttributeFilter objectNameAttributeFilter =
                ObjectNameAttributeFilter.create(new HashMap<>());
        List<ObjectName> objectNames = new LinkedList<>();
        objectNames.add(null);
        if (args.length >= 3) {
            new JmxScraper(
                            args[0],
                            args[1],
                            args[2],
                            (args.length > 3 && "ssl".equalsIgnoreCase(args[3])),
                            objectNames,
                            new LinkedList<>(),
                            objectNameAttributeFilter,
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        } else if (args.length > 0) {
            new JmxScraper(
                            args[0],
                            "",
                            "",
                            false,
                            objectNames,
                            new LinkedList<>(),
                            objectNameAttributeFilter,
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        } else {
            new JmxScraper(
                            "",
                            "",
                            "",
                            false,
                            objectNames,
                            new LinkedList<>(),
                            objectNameAttributeFilter,
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        }
    }
}
