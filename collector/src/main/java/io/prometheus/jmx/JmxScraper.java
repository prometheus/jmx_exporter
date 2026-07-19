/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx;

import io.prometheus.jmx.JmxCollector.SslProperties;
import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
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
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.ProviderUtils;

/**
 * Class to implement JmxScraper
 */
class JmxScraper {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxScraper.class);

    private static final List<ObjectName> JVM_EXCLUDE_OBJECT_NAMES;

    static {
        List<ObjectName> names = new ArrayList<>();
        try {
            names.add(new ObjectName("com.sun.management:*"));
            names.add(new ObjectName("com.sun.management.jmxremote:*"));
            names.add(new ObjectName("java.lang:*"));
            names.add(new ObjectName("java.nio:*"));
            names.add(new ObjectName("java.util.logging:*"));
            names.add(new ObjectName("javax.management:*"));
            names.add(new ObjectName("javax.management.remote:*"));
            names.add(new ObjectName("jdk.internal:*"));
            names.add(new ObjectName("jdk.management:*"));
            names.add(new ObjectName("jdk.management.jfr:*"));
            names.add(new ObjectName("sun.management:*"));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
        JVM_EXCLUDE_OBJECT_NAMES = Collections.unmodifiableList(names);
    }

    /**
     * Interface to implement MBeanReceiver
     */
    public interface MBeanReceiver {

        /**
         * Method to create a bean
         *
         * @param domain domain
         * @param beanProperties beanProperties
         * @param attrKeys attrKeys
         * @param attrName attrName
         * @param attrType attrType
         * @param attrDescription attrDescription
         * @param value value
         */
        void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                List<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value);
    }

    private final MBeanReceiver receiver;
    private final String jmxUrl;
    private final String username;
    private final String password;
    private final SslProperties sslProperties;
    private final List<ObjectName> includeObjectNames, excludeObjectNames;
    private final boolean excludeJvmMetrics;
    private final List<JmxCollector.MetricCustomizer> metricCustomizers;
    private final ObjectNameAttributeFilter objectNameAttributeFilter;
    private final JmxMBeanPropertyCache jmxMBeanPropertyCache;

    /**
     * Constructor
     *
     * @param jmxUrl jmxUrl
     * @param username username
     * @param password password
     * @param sslProperties sslProperties
     * @param includeObjectNames includeObjectNames
     * @param excludeObjectNames excludeObjectNames
     * @param excludeJvmMetrics excludeJvmMetrics
     * @param objectNameAttributeFilter objectNameAttributeFilter
     * @param metricCustomizers metricCustomizers
     * @param receiver receiver
     * @param jmxMBeanPropertyCache jmxMBeanPropertyCache
     */
    public JmxScraper(
            String jmxUrl,
            String username,
            String password,
            SslProperties sslProperties,
            List<ObjectName> includeObjectNames,
            List<ObjectName> excludeObjectNames,
            boolean excludeJvmMetrics,
            ObjectNameAttributeFilter objectNameAttributeFilter,
            List<JmxCollector.MetricCustomizer> metricCustomizers,
            MBeanReceiver receiver,
            JmxMBeanPropertyCache jmxMBeanPropertyCache) {
        this.jmxUrl = jmxUrl;
        this.receiver = receiver;
        this.username = username;
        this.password = password;
        this.sslProperties = sslProperties;
        this.includeObjectNames = includeObjectNames;
        this.excludeObjectNames = excludeObjectNames;
        this.excludeJvmMetrics = excludeJvmMetrics;
        this.metricCustomizers = metricCustomizers;
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
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                String[] credentials = new String[] {username, password};
                environment.put(JMXConnector.CREDENTIALS, credentials);
            }
            if (sslProperties.enabled) {
                environment.put(Context.SECURITY_PROTOCOL, "ssl");

                SSLFactory sslFactory = createSslFactory();
                ProviderUtils.configure(sslFactory);

                SslRMIClientSocketFactory clientSocketFactory = new SslRMIClientSocketFactory();
                environment.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, clientSocketFactory);
                if (!"true".equalsIgnoreCase(System.getenv("RMI_REGISTRY_SSL_DISABLED"))) {
                    environment.put("com.sun.jndi.rmi.factory.socket", clientSocketFactory);
                }
            }

            jmxc = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), environment);
            beanConn = jmxc.getMBeanServerConnection();
        }
        try {
            // Query MBean names, see #89 for reasons queryMBeans() is used instead of queryNames()
            Set<ObjectName> mBeanNames = new HashSet<>();
            // Track beans from explicit (non-null) include patterns so they can be
            // re-added after JVM exclusion without additional JMX queries.
            Set<ObjectName> explicitIncludeBeans = new HashSet<>();
            for (ObjectName name : includeObjectNames) {
                for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                    ObjectName objectName = instance.getObjectName();
                    mBeanNames.add(objectName);
                    if (name != null) {
                        explicitIncludeBeans.add(objectName);
                    }
                }
            }

            for (ObjectName name : excludeObjectNames) {
                for (ObjectInstance instance : beanConn.queryMBeans(name, null)) {
                    ObjectName objectName = instance.getObjectName();
                    explicitIncludeBeans.remove(objectName);
                    mBeanNames.remove(objectName);
                }
            }

            if (excludeJvmMetrics) {
                // Remove JVM beans in-memory (no extra JMX queries).
                Iterator<ObjectName> iterator = mBeanNames.iterator();
                while (iterator.hasNext()) {
                    ObjectName bean = iterator.next();
                    for (ObjectName jvmPattern : JVM_EXCLUDE_OBJECT_NAMES) {
                        if (jvmPattern.apply(bean)) {
                            iterator.remove();
                            break;
                        }
                    }
                }

                // Re-add beans from explicit include patterns that were removed
                // by JVM exclusion but not by user-specified excludes (those were
                // already removed from explicitIncludeBeans above).
                mBeanNames.addAll(explicitIncludeBeans);
            }

            // Now that we have *only* the whitelisted mBeans, remove any old ones from the cache
            // and dynamic attribute filter:
            jmxMBeanPropertyCache.onlyKeepMBeans(mBeanNames);
            objectNameAttributeFilter.onlyKeepMBeans(mBeanNames);

            for (ObjectName objectName : mBeanNames) {
                long start = System.nanoTime();
                scrapeBean(beanConn, objectName);
                LOGGER.trace("TIME: %d ns for %s", System.nanoTime() - start, objectName);
            }
        } finally {
            if (jmxc != null) {
                jmxc.close();
            }

            if (sslProperties.enabled) {
                ProviderUtils.remove();
            }
        }
    }

    /**
     * Attempts to resolve the ssl configuration defined in the yaml file Next to that it also
     * attempts to read the following system properties:
     *
     * <p>
     *
     * <pre>
     *  - javax.net.ssl.keyStore
     *  - javax.net.ssl.keyStorePassword
     *  - javax.net.ssl.keyStoreType
     *  - javax.net.ssl.keyStoreProvider
     *  - javax.net.ssl.trustStore
     *  - javax.net.ssl.trustStorePassword
     *  - javax.net.ssl.trustStoreType
     *  - javax.net.ssl.trustStoreProvider
     *  - https.protocols
     *  - https.cipherSuites
     * </pre>
     */
    private SSLFactory createSslFactory() {
        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder().withDefaultTrustMaterial();
        sslProperties
                .getKeyStoreProperties()
                .ifPresent(props -> sslFactoryBuilder.withIdentityMaterial(props.path, props.password, props.type));
        sslProperties
                .getTrustStoreProperties()
                .ifPresent(props -> sslFactoryBuilder.withTrustMaterial(props.path, props.password, props.type));

        if (!sslProperties.protocols.isEmpty()) {
            sslFactoryBuilder.withProtocols(sslProperties.protocols.toArray(new String[0]));
        }
        if (!sslProperties.ciphers.isEmpty()) {
            sslFactoryBuilder.withCiphers(sslProperties.ciphers.toArray(new String[0]));
        }

        callSafely(
                sslFactoryBuilder::withSystemPropertyDerivedIdentityMaterial,
                sslFactoryBuilder::withSystemPropertyDerivedTrustMaterial,
                sslFactoryBuilder::withSystemPropertyDerivedProtocols,
                sslFactoryBuilder::withSystemPropertyDerivedCiphers);

        return sslFactoryBuilder.build();
    }

    private void callSafely(Callable<?>... callables) {
        for (Callable<?> callable : callables) {
            try {
                callable.call();
            } catch (Exception ignored) {
                // Intentionally empty
            }
        }
    }

    private void scrapeBean(MBeanServerConnection beanConn, ObjectName mBeanName) {
        MBeanInfo mBeanInfo;

        try {
            mBeanInfo = beanConn.getMBeanInfo(mBeanName);
        } catch (IOException e) {
            LOGGER.trace("%s getMBeanInfo Fail: %s", mBeanName, e);
            return;
        } catch (JMException e) {
            LOGGER.trace("%s getMBeanInfo Fail: %s", mBeanName, e.getMessage());
            return;
        }

        MBeanAttributeInfo[] mBeanAttributeInfos = mBeanInfo.getAttributes();

        Map<String, MBeanAttributeInfo> name2MBeanAttributeInfo = new LinkedHashMap<>();
        for (MBeanAttributeInfo mBeanAttributeInfo : mBeanAttributeInfos) {
            String attributeName = mBeanAttributeInfo.getName();
            if (!mBeanAttributeInfo.isReadable()) {
                LOGGER.trace("%s_%s not readable", mBeanName, attributeName);
                continue;
            }

            if (objectNameAttributeFilter.exclude(mBeanName, attributeName)) {
                continue;
            }

            if (objectNameAttributeFilter.includeObjectNameAttributesIsEmpty()) {
                name2MBeanAttributeInfo.put(attributeName, mBeanAttributeInfo);
                continue;
            }

            if (objectNameAttributeFilter.include(mBeanName, attributeName)) {
                name2MBeanAttributeInfo.put(attributeName, mBeanAttributeInfo);
            }
        }

        if (name2MBeanAttributeInfo.isEmpty()) {
            return;
        }

        AttributeList attributes;

        try {
            // bulk load all attributes
            attributes = beanConn.getAttributes(
                    mBeanName, name2MBeanAttributeInfo.keySet().toArray(new String[0]));
            if (attributes == null) {
                LOGGER.trace("%s getMBeanInfo Fail: attributes are null", mBeanName);
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("%s getAttributes Fail: processing one by one: %s", mBeanName, e.getMessage());

            // couldn't get them all in one go, try them 1 by 1
            processAttributesOneByOne(beanConn, mBeanName, name2MBeanAttributeInfo);
            return;
        }

        final String mBeanNameString = mBeanName.toString();
        final String mBeanDomain = mBeanName.getDomain();
        JmxCollector.MetricCustomizer metricCustomizer = getMetricCustomizer(mBeanName);
        Map<String, String> attributesAsLabelsWithValues = Collections.emptyMap();
        if (metricCustomizer != null) {
            if (metricCustomizer.attributesAsLabels != null) {
                attributesAsLabelsWithValues = getAttributesAsLabelsWithValues(metricCustomizer, attributes);
            }
            for (JmxCollector.ExtraMetric extraMetric : getExtraMetrics(metricCustomizer)) {
                processBeanValue(
                        mBeanName,
                        mBeanDomain,
                        jmxMBeanPropertyCache.getKeyPropertyList(mBeanName),
                        attributesAsLabelsWithValues,
                        new ArrayList<>(),
                        extraMetric.name,
                        "UNKNOWN",
                        extraMetric.description,
                        extraMetric.value);
            }
        }

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

                MBeanAttributeInfo mBeanAttributeInfo = name2MBeanAttributeInfo.get(attributeName);
                if (mBeanAttributeInfo == null) {
                    LOGGER.trace(
                            "%s returned attribute '%s' that is not present in its MBeanInfo. Skipping.",
                            mBeanName, attributeName);
                    continue;
                }
                LOGGER.trace("%s_%s process", mBeanName, mBeanAttributeInfo.getName());
                processBeanValue(
                        mBeanName,
                        mBeanDomain,
                        jmxMBeanPropertyCache.getKeyPropertyList(mBeanName),
                        attributesAsLabelsWithValues,
                        new ArrayList<>(),
                        attributeName,
                        mBeanAttributeInfo.getType(),
                        mBeanAttributeInfo.getDescription(),
                        attribute.getValue());
            } else if (object == null) {
                LOGGER.trace("%s object is NULL, not an instance Attribute, skipping", mBeanName);
            } else {
                LOGGER.trace(
                        "%s object [%s] isn't an instance Attribute, skipping",
                        mBeanName, object.getClass().getName());
            }
        }
    }

    private List<JmxCollector.ExtraMetric> getExtraMetrics(JmxCollector.MetricCustomizer metricCustomizer) {
        return metricCustomizer.extraMetrics != null ? metricCustomizer.extraMetrics : Collections.emptyList();
    }

    private Map<String, String> getAttributesAsLabelsWithValues(
            JmxCollector.MetricCustomizer metricCustomizer, AttributeList attributes) {
        Map<String, Object> attributeMap =
                attributes.asList().stream().collect(Collectors.toMap(Attribute::getName, Attribute::getValue));
        Map<String, String> attributesAsLabelsWithValues = new HashMap<>();
        for (String attributeAsLabel : metricCustomizer.attributesAsLabels) {
            Object attrValue = attributeMap.get(attributeAsLabel);
            if (attrValue != null) {
                attributesAsLabelsWithValues.put(attributeAsLabel, attrValue.toString());
            }
        }
        return attributesAsLabelsWithValues;
    }

    private JmxCollector.MetricCustomizer getMetricCustomizer(ObjectName mBeanName) {
        if (!metricCustomizers.isEmpty()) {
            for (JmxCollector.MetricCustomizer metricCustomizer : metricCustomizers) {
                if (filterMbeanByDomainAndProperties(mBeanName, metricCustomizer)) {
                    return metricCustomizer;
                }
            }
        }
        return null;
    }

    private boolean filterMbeanByDomainAndProperties(
            ObjectName mBeanName, JmxCollector.MetricCustomizer metricCustomizer) {
        return metricCustomizer.mbeanFilter.domain.equals(mBeanName.getDomain())
                && mBeanName
                        .getKeyPropertyList()
                        .entrySet()
                        .containsAll(metricCustomizer.mbeanFilter.properties.entrySet());
    }

    private void processAttributesOneByOne(
            MBeanServerConnection beanConn, ObjectName mbeanName, Map<String, MBeanAttributeInfo> name2AttrInfo) {
        Object value;
        for (MBeanAttributeInfo attr : name2AttrInfo.values()) {
            String attributeName = attr.getName();
            try {
                value = beanConn.getAttribute(mbeanName, attributeName);
            } catch (Exception e) {
                LOGGER.trace("%s_%s Fail: %s", mbeanName, attributeName, e.getMessage());
                continue;
            }

            LOGGER.trace("%s_%s process", mbeanName, attributeName);
            processBeanValue(
                    mbeanName,
                    mbeanName.getDomain(),
                    jmxMBeanPropertyCache.getKeyPropertyList(mbeanName),
                    new HashMap<>(),
                    new ArrayList<>(),
                    attributeName,
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
            Map<String, String> attributesAsLabelsWithValues,
            List<String> attrKeys,
            String attrName,
            String attrType,
            String attrDescription,
            Object value) {
        if (value == null) {
            LOGGER.trace("%s%s%s scrape: null", domain, beanProperties, attrName);
        } else if (value instanceof Number
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Date) {
            if (value instanceof Date) {
                attrType = "java.lang.Double";
                value = ((Date) value).getTime() / 1000.0;
            }
            LOGGER.trace("%s%s%s scrape: %s", domain, beanProperties, attrName, value);
            this.receiver.recordBean(
                    domain,
                    beanProperties,
                    attributesAsLabelsWithValues,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value);
        } else if (value instanceof CompositeData) {
            LOGGER.trace("%s%s%s scrape: compositedata", domain, beanProperties, attrName);
            CompositeData composite = (CompositeData) value;
            CompositeType type = composite.getCompositeType();
            attrKeys = new ArrayList<>(attrKeys);
            attrKeys.add(attrName);
            for (String key : type.keySet()) {
                String typeName = type.getType(key).getTypeName();
                Object compositeValue = composite.get(key);
                processBeanValue(
                        objectName,
                        domain,
                        beanProperties,
                        attributesAsLabelsWithValues,
                        attrKeys,
                        key,
                        typeName,
                        type.getDescription(),
                        compositeValue);
            }
        } else if (value instanceof TabularData) {
            // I don't pretend to have a good understanding of TabularData.
            // The real world usage doesn't appear to match how they were
            // meant to be used according to the docs. I've only seen them
            // used as 'key' 'value' pairs even when 'value' is itself a
            // CompositeData of multiple values.
            LOGGER.trace("%s%s%s scrape: tabulardata", domain, beanProperties, attrName);
            TabularData tds = (TabularData) value;
            TabularType tt = tds.getTabularType();

            List<String> rowKeys = tt.getIndexNames();

            CompositeType type = tt.getRowType();
            Set<String> valueKeys = new TreeSet<>(type.keySet());
            rowKeys.forEach(valueKeys::remove);

            List<String> extendedAttrKeys = new ArrayList<>(attrKeys);
            extendedAttrKeys.add(attrName);
            for (Object compositeDataValue : tds.values()) {
                if (compositeDataValue instanceof CompositeData) {
                    CompositeData composite = (CompositeData) compositeDataValue;
                    LinkedHashMap<String, String> l2s = new LinkedHashMap<>(beanProperties);
                    for (String idx : rowKeys) {
                        Object obj = composite.get(idx);
                        if (obj != null) {

                            // Nested tabulardata will repeat the 'key' label, so
                            // append a suffix to distinguish each.
                            int suffixCount = 0;
                            StringBuilder idxBuilder = new StringBuilder(idx);
                            while (l2s.containsKey(idxBuilder.toString())) {
                                idxBuilder.append("_");
                                suffixCount++;
                                if (suffixCount > 1000) {
                                    throw new IllegalStateException(
                                            "Too many key collisions in TabularData processing");
                                }
                            }
                            idx = idxBuilder.toString();

                            if (obj instanceof CompositeData) {
                                // TabularData key is a composite key
                                CompositeData compositeKey = (CompositeData) obj;
                                CompositeType ct = compositeKey.getCompositeType();
                                for (final String compositeKeyIdx : ct.keySet()) {
                                    l2s.put(
                                            new StringBuilder(idx.length() + 1 + compositeKeyIdx.length())
                                                    .append(idx)
                                                    .append('_')
                                                    .append(compositeKeyIdx)
                                                    .toString(),
                                            compositeKey.get(compositeKeyIdx).toString());
                                }
                            } else {
                                // TabularData key is an Open type key
                                l2s.put(idx, obj.toString());
                            }
                        }
                    }
                    for (String valueIdx : valueKeys) {
                        List<String> attrNames = extendedAttrKeys;
                        String typeName = type.getType(valueIdx).getTypeName();
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
                                attributesAsLabelsWithValues,
                                attrNames,
                                name,
                                typeName,
                                type.getDescription(),
                                composite.get(valueIdx));
                    }
                } else {
                    LOGGER.trace("%s scrape: not a correct tabulardata format", domain);
                }
            }
        } else if (value.getClass().isArray()) {
            LOGGER.trace("%s scrape: arrays are unsupported", domain);
        } else if (value instanceof Optional) {
            LOGGER.trace("%s%s%s scrape: Optional", domain, beanProperties, attrName);
            Optional<?> optional = (Optional<?>) value;
            if (optional.isPresent()) {
                processBeanValue(
                        objectName,
                        domain,
                        beanProperties,
                        attributesAsLabelsWithValues,
                        attrKeys,
                        attrName,
                        attrType,
                        attrDescription,
                        optional.get());
            }
        } else if (value.getClass().isEnum()) {
            LOGGER.trace("%s%s%s scrape: %s", domain, beanProperties, attrName, value);
            processBeanValue(
                    objectName,
                    domain,
                    beanProperties,
                    attributesAsLabelsWithValues,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value.toString());
        } else {
            objectNameAttributeFilter.add(objectName, attrName);
            LOGGER.trace("%s%s scrape: %s not exported", domain, beanProperties, attrType);
        }
    }

    private static class StdoutWriter implements MBeanReceiver {
        public void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                List<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value) {
            System.out.println(new StringBuilder(256)
                    .append(domain)
                    .append(beanProperties)
                    .append(attrKeys)
                    .append(attrName)
                    .append(": ")
                    .append(value)
                    .toString());
        }
    }

    /**
     * Convenience method to run standalone.
     */
    public static void main(String[] args) throws Exception {
        ObjectNameAttributeFilter objectNameAttributeFilter = ObjectNameAttributeFilter.create(new HashMap<>());
        List<ObjectName> objectNames = new ArrayList<>();
        objectNames.add(null);
        if (args.length >= 3) {
            new JmxScraper(
                            args[0],
                            args[1],
                            args[2],
                            (args.length > 3 && "ssl".equalsIgnoreCase(args[3]))
                                    ? new SslProperties(true)
                                    : new SslProperties(false),
                            objectNames,
                            new ArrayList<>(),
                            false,
                            objectNameAttributeFilter,
                            new ArrayList<>(),
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        } else if (args.length > 0) {
            new JmxScraper(
                            args[0],
                            "",
                            "",
                            new SslProperties(false),
                            objectNames,
                            new ArrayList<>(),
                            false,
                            objectNameAttributeFilter,
                            new ArrayList<>(),
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        } else {
            new JmxScraper(
                            "",
                            "",
                            "",
                            new SslProperties(false),
                            objectNames,
                            new ArrayList<>(),
                            false,
                            objectNameAttributeFilter,
                            new ArrayList<>(),
                            new StdoutWriter(),
                            new JmxMBeanPropertyCache())
                    .doScrape();
        }
    }
}
