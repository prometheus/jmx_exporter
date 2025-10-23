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

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/** Class to implement filtering of an MBean's attributes based on the attribute's name */
@SuppressWarnings("unchecked")
public class ObjectNameAttributeFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectNameAttributeFilter.class);

    /** Configuration constant to define a mapping of ObjectNames to attribute names to exclude */
    public static final String EXCLUDE_OBJECT_NAME_ATTRIBUTES = "excludeObjectNameAttributes";

    /** Configuration constant to define a mapping of ObjectNames to attribute names to include */
    public static final String INCLUDE_OBJECT_NAME_ATTRIBUTES = "includeObjectNameAttributes";

    /** Configuration constant to enable auto ObjectName attributes filtering */
    public static final String AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES =
            "autoExcludeObjectNameAttributes";

    private final Map<ObjectName, Set<String>> configExcludeObjectNameAttributesMap;
    private final Map<ObjectName, Set<String>> dynamicExcludeObjectNameAttributesMap;
    private final Map<ObjectName, Set<String>> includeObjectNameAttributesMap;

    private boolean autoExcludeObjectNameAttributes;

    /** Constructor */
    private ObjectNameAttributeFilter() {
        configExcludeObjectNameAttributesMap = new ConcurrentHashMap<>();
        dynamicExcludeObjectNameAttributesMap = new ConcurrentHashMap<>();
        includeObjectNameAttributesMap = new ConcurrentHashMap<>();
    }

    /**
     * Method to initialize the ObjectNameAttributeFilter
     *
     * @param yamlConfig yamlConfig
     * @return an ObjectNameAttributeFilter
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    private ObjectNameAttributeFilter initialize(Map<String, Object> yamlConfig)
            throws MalformedObjectNameException {
        initializeObjectNameAttributes(
                yamlConfig, EXCLUDE_OBJECT_NAME_ATTRIBUTES, configExcludeObjectNameAttributesMap);
        initializeObjectNameAttributes(
                yamlConfig, INCLUDE_OBJECT_NAME_ATTRIBUTES, includeObjectNameAttributesMap);
        if (yamlConfig.containsKey(AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES)) {
            autoExcludeObjectNameAttributes =
                    (Boolean) yamlConfig.get(AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES);
        } else {
            autoExcludeObjectNameAttributes = true;
        }

        LOGGER.trace("dynamicExclusion [%b]", autoExcludeObjectNameAttributes);

        return this;
    }

    /**
     * Method to initialize the configExcludeObjectNameAttributesMap and
     * includeObjectNameAttributesMap
     *
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    private void initializeObjectNameAttributes(
            Map<String, Object> yamlConfig,
            String key,
            Map<ObjectName, Set<String>> objectNameAttributesMap)
            throws MalformedObjectNameException {
        if (yamlConfig.containsKey(key)) {
            Map<Object, Object> objectNameAttributeMap = (Map<Object, Object>) yamlConfig.get(key);

            for (Map.Entry<Object, Object> entry : objectNameAttributeMap.entrySet()) {
                ObjectName objectName = new ObjectName((String) entry.getKey());

                List<String> attributeNames = (List<String>) entry.getValue();

                Set<String> attributeNameSet =
                        objectNameAttributesMap.computeIfAbsent(
                                objectName, o -> Collections.synchronizedSet(new HashSet<>()));

                attributeNameSet.addAll(attributeNames);
            }
        }
    }

    /**
     * Method to add an attribute name to the filter if dynamic exclusion is enabled
     *
     * @param objectName the ObjectName
     * @param attributeName the attribute name
     */
    public void add(ObjectName objectName, String attributeName) {
        if (autoExcludeObjectNameAttributes) {
            Set<String> attributeNameSet =
                    dynamicExcludeObjectNameAttributesMap.computeIfAbsent(
                            objectName, o -> Collections.synchronizedSet(new HashSet<>()));

            LOGGER.trace(
                    "auto adding exclusion of object name [%s] attribute name [%s]",
                    objectName.getCanonicalName(), attributeName);

            attributeNameSet.add(attributeName);
        }
    }

    /**
     * Method to only keep "alive" mBeans, remove old mBeans to prevent memory growth
     *
     * @param aliveMBeans aliveMBeans
     */
    public void onlyKeepMBeans(Set<ObjectName> aliveMBeans) {
        if (autoExcludeObjectNameAttributes) {
            for (ObjectName prevName : dynamicExcludeObjectNameAttributesMap.keySet()) {
                if (!aliveMBeans.contains(prevName)) {
                    dynamicExcludeObjectNameAttributesMap.remove(prevName);
                }
            }
        }
    }

    /**
     * Method to check if an attribute should be excluded
     *
     * @param objectName the ObjectName
     * @param attributeName the attribute name
     * @return true if it should be excluded, false otherwise
     */
    public boolean exclude(ObjectName objectName, String attributeName) {
        return exclude(configExcludeObjectNameAttributesMap, objectName, attributeName)
                || exclude(dynamicExcludeObjectNameAttributesMap, objectName, attributeName);
    }

    private boolean exclude(
            Map<ObjectName, Set<String>> exclusionMap,
            ObjectName objectName,
            String attributeName) {
        boolean result = false;
        if (!exclusionMap.isEmpty()) {
            Set<String> attributeNameSet = exclusionMap.get(objectName);
            if (attributeNameSet != null) {
                result = attributeNameSet.contains(attributeName);
            }
        }
        return result;
    }

    /**
     * Method to return whether an attribute should be included
     *
     * @param objectName objectName
     * @param attributeName attributeName
     * @return true if the attribute should be included, else false
     */
    public boolean include(ObjectName objectName, String attributeName) {
        boolean result = false;

        Set<String> attributeNameSet = includeObjectNameAttributesMap.get(objectName);
        if (attributeNameSet != null) {
            result = attributeNameSet.contains(attributeName);
        }

        return result;
    }

    /**
     * Method to return whether any attributes are included
     *
     * @return true if empty, else false
     */
    public boolean includeObjectNameAttributesIsEmpty() {
        return includeObjectNameAttributesMap.isEmpty();
    }

    /**
     * Method to create an ObjectNameAttributeFilter
     *
     * @param yamlConfig yamlConfig
     * @return an ObjectNameAttributeFilter
     */
    public static ObjectNameAttributeFilter create(Map<String, Object> yamlConfig) {
        try {
            return new ObjectNameAttributeFilter().initialize(yamlConfig);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(
                    "Invalid configuration format for excludeObjectNameAttributes", e);
        }
    }
}
