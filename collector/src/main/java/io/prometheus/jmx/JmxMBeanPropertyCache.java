package io.prometheus.jmx;

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

    // Implement a version of ObjectName.getKeyPropertyList that returns the
    // properties in the ordered they were added (the ObjectName stores them
    // in the order they were added).
    private final Map<ObjectName, LinkedHashMap<String, String>> keyPropertiesPerBean;

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

    public void onlyKeepMBeans(Set<ObjectName> latestBeans) {
        for (ObjectName prevName : keyPropertiesPerBean.keySet()) {
            if (!latestBeans.contains(prevName)) {
                keyPropertiesPerBean.remove(prevName);
            }
        }
    }


}
