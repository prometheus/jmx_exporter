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

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/** Class to implement filtering of an MBean's attributes based on the attribute's name */
@SuppressWarnings("unchecked")
public class ObjectNameAttributeFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectNameAttributeFilter.class);

    /** Configuration constant to define a mapping of ObjectNames to attribute names */
    public static final String EXCLUDE_OBJECT_NAME_ATTRIBUTES = "excludeObjectNameAttributes";

    /** Configuration constant to enable auto ObjectName attributes filtering */
    public static final String AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES =
            "autoExcludeObjectNameAttributes";

    private final Map<ObjectName, Set<String>> excludeObjectNameAttributesMap;

    private boolean autoExcludeObjectNameAttributes;

    /** Constructor */
    private ObjectNameAttributeFilter() {
        excludeObjectNameAttributesMap = new ConcurrentHashMap<>();
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
        if (yamlConfig.containsKey(EXCLUDE_OBJECT_NAME_ATTRIBUTES)) {
            Map<Object, Object> objectNameAttributeMap =
                    (Map<Object, Object>) yamlConfig.get(EXCLUDE_OBJECT_NAME_ATTRIBUTES);

            for (Map.Entry<Object, Object> entry : objectNameAttributeMap.entrySet()) {
                ObjectName objectName = new ObjectName((String) entry.getKey());

                List<String> attributeNames = (List<String>) entry.getValue();

                Set<String> attributeNameSet =
                        excludeObjectNameAttributesMap.computeIfAbsent(
                                objectName, o -> Collections.synchronizedSet(new HashSet<>()));

                attributeNameSet.addAll(attributeNames);
                for (String attribueName : attributeNames) {
                    attributeNameSet.add(attribueName);
                }

                excludeObjectNameAttributesMap.put(objectName, attributeNameSet);
            }
        }

        if (yamlConfig.containsKey(AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES)) {
            autoExcludeObjectNameAttributes =
                    (Boolean) yamlConfig.get(AUTO_EXCLUDE_OBJECT_NAME_ATTRIBUTES);
        } else {
            autoExcludeObjectNameAttributes = true;
        }

        LOGGER.log(Level.FINE, "dynamicExclusion [%b]", autoExcludeObjectNameAttributes);

        return this;
    }

    /**
     * Method to add an attribute name to the filter if dynamic exclusion is enabled
     *
     * @param objectName the ObjectName
     * @param attributeName the attribute name
     */
    public void add(ObjectName objectName, String attributeName) {
        if (autoExcludeObjectNameAttributes) {
            Set<String> attribteNameSet =
                    excludeObjectNameAttributesMap.computeIfAbsent(
                            objectName, o -> Collections.synchronizedSet(new HashSet<>()));

            LOGGER.log(
                    Level.FINE,
                    "auto adding exclusion of object name [%s] attribute name [%s]",
                    objectName.getCanonicalName(),
                    attributeName);

            attribteNameSet.add(attributeName);
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
        boolean result = false;

        for (Map.Entry<ObjectName, Set<String>> objectNameSetEntry :
                excludeObjectNameAttributesMap.entrySet()) {
            if (objectNameSetEntry.getKey().apply(objectName)) {
                // if exclusion found - return
                // otherwise keep searching as checked object may match multiple patterns
                // and checked attribute may be defined only under one of them
                if (objectNameSetEntry.getValue().contains(attributeName)) {
                    return true;
                }
            }
        }

        return result;
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
