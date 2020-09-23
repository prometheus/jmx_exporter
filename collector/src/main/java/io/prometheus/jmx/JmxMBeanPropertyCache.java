package io.prometheus.jmx;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This object stores a mapping of mBean objectNames to mBean key property lists. The main purpose of it is to reduce
 * the frequency with which we invoke PROPERTY_PATTERN when discovering mBeans.
 */
class JmxMBeanPropertyCache {
    /**
     * Encapsulates attribute info with other data.
     */
    public static class MBeanAttributeInfoWrapper {
        MBeanAttributeInfo info;
        boolean usedAtLastScrape;
        public MBeanAttributeInfoWrapper(MBeanAttributeInfo info) {
            this.info = info;
            this.usedAtLastScrape = true;
        }
        public boolean isUsedAtLastScrape() {
            return usedAtLastScrape;
        }
        public void setUsedAtLastScrape(boolean activated) {
            this.usedAtLastScrape = activated;
        }
    }

    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "([^,=:\\*\\?]+)" + // Name - non-empty, anything but comma, equals, colon, star, or question mark
                    "=" +  // Equals
                    "(" + // Either
                    "\"" + // Quoted
                    "(?:" + // A possibly empty sequence of
                    "[^\\\\\"]*" + // Greedily match anything but backslash or quote
                    "(?:\\\\.)?" + // Greedily see if we can match an escaped sequence
                    ")*" +
                    "\"" +
                    "|" + // Or
                    "[^,=:\"]*" + // Unquoted - can be empty, anything but comma, equals, colon, or quote
                    ")");

    // Implement a version of ObjectName.getKeyPropertyList that returns the
    // properties in the ordered they were added (the ObjectName stores them
    // in the order they were added).
    private final Map<ObjectName, LinkedHashMap<String, String>> keyPropertiesPerBean;

    private final Map<ObjectName, Map<String, MBeanAttributeInfoWrapper>> mbeanInfoCache = 
            new ConcurrentHashMap<ObjectName, Map<String,MBeanAttributeInfoWrapper>>();

    public JmxMBeanPropertyCache() {
        this.keyPropertiesPerBean = new ConcurrentHashMap<ObjectName, LinkedHashMap<String, String>>();
    }

    Map<ObjectName, LinkedHashMap<String, String>> getKeyPropertiesPerBean() {
        return keyPropertiesPerBean;
    }

    public LinkedHashMap<String, String> getKeyPropertyList(ObjectName mbeanName) {
        LinkedHashMap<String, String> keyProperties = keyPropertiesPerBean.get(mbeanName);
        if (keyProperties == null) {
            keyProperties = new LinkedHashMap<String, String>();
            String properties = mbeanName.getKeyPropertyListString();
            Matcher match = PROPERTY_PATTERN.matcher(properties);
            while (match.lookingAt()) {
                keyProperties.put(match.group(1), match.group(2));
                properties = properties.substring(match.end());
                if (properties.startsWith(",")) {
                    properties = properties.substring(1);
                }
                match.reset(properties);
            }
            keyPropertiesPerBean.put(mbeanName, keyProperties);
        }
        return keyProperties;
    }

    /**
     * Puts this MBean info in cache.
     *
     * @param mbeanName
     * @param name2AttrInfo
     */
    public void cacheAttrInfo(ObjectName mbeanName, Map<String, MBeanAttributeInfoWrapper> name2AttrInfo) {
        mbeanInfoCache.put(mbeanName, name2AttrInfo);
    }

    /**
     * Gets this MBean info from cache, or null if not present.
     *
     * @param mbeanName
     * @return Map or attributes names to attributes info
     */
    public Map<String, MBeanAttributeInfoWrapper> getAttrInfo(ObjectName mbeanName) {
        return mbeanInfoCache.get(mbeanName);
    }

    /**
     * Cleans the cache of unused MBeans info.
     *
     * @param latestBeans Beans to keep
     */
    public void onlyKeepMBeans(Set<ObjectName> latestBeans) {
        for (ObjectName prevName : keyPropertiesPerBean.keySet()) {
            if (!latestBeans.contains(prevName)) {
                keyPropertiesPerBean.remove(prevName);
                mbeanInfoCache.remove(prevName);
            }
        }
    }


}
