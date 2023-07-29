/*
 * Copyright (C) 2018-2023 The Prometheus jmx_exporter Authors
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.ObjectName;

/**
 * This object stores a mapping of mBean objectNames to mBean key property lists. The main purpose
 * of it is to reduce the frequency with which we invoke PROPERTY_PATTERN when discovering mBeans.
 */
class JmxMBeanPropertyCache {
    private static final Pattern PROPERTY_PATTERN =
            Pattern.compile(
                    "([^,=:\\*\\?]+)"
                            + // Name - non-empty, anything but comma, equals, colon, star, or
                            // question mark
                            "="
                            + // Equals
                            "("
                            + // Either
                            "\""
                            + // Quoted
                            "(?:"
                            + // A possibly empty sequence of
                            "[^\\\\\"]*"
                            + // Greedily match anything but backslash or quote
                            "(?:\\\\.)?"
                            + // Greedily see if we can match an escaped sequence
                            ")*"
                            + "\""
                            + "|"
                            + // Or
                            "[^,=:\"]*"
                            + // Unquoted - can be empty, anything but comma, equals, colon, or
                            // quote
                            ")");

    // Implement a version of ObjectName.getKeyPropertyList that returns the
    // properties in the ordered they were added (the ObjectName stores them
    // in the order they were added).
    private final Map<ObjectName, LinkedHashMap<String, String>> keyPropertiesPerBean;

    public JmxMBeanPropertyCache() {
        this.keyPropertiesPerBean = new ConcurrentHashMap<>();
    }

    Map<ObjectName, LinkedHashMap<String, String>> getKeyPropertiesPerBean() {
        return keyPropertiesPerBean;
    }

    public LinkedHashMap<String, String> getKeyPropertyList(ObjectName mbeanName) {
        LinkedHashMap<String, String> keyProperties = keyPropertiesPerBean.get(mbeanName);
        if (keyProperties == null) {
            keyProperties = new LinkedHashMap<>();
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
