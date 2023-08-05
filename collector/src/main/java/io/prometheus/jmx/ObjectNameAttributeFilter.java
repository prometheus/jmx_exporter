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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/** Class to implement an ObjectNameAttributeFilter */
@SuppressWarnings("unchecked")
public class ObjectNameAttributeFilter {

    public static final String EXCLUDE_OBJECT_NAME_ATTRIBUTES = "excludeObjectNameAttributes";

    public static final String EXCLUDE_OBJECT_NAME_ATTRIBUTES_DYNAMIC =
            EXCLUDE_OBJECT_NAME_ATTRIBUTES + "Dynamic";

    private final Map<ObjectName, Set<String>> excludeObjectNameAttributesMap;

    private boolean dynamicExclusion;

    /** Constructor */
    private ObjectNameAttributeFilter() {
        excludeObjectNameAttributesMap = Collections.synchronizedMap(new HashMap<>());
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

                for (String attribueName : attributeNames) {
                    attributeNameSet.add(attribueName);
                }

                excludeObjectNameAttributesMap.put(objectName, attributeNameSet);
            }
        }

        if (yamlConfig.containsKey(EXCLUDE_OBJECT_NAME_ATTRIBUTES_DYNAMIC)) {
            dynamicExclusion = (Boolean) yamlConfig.get(EXCLUDE_OBJECT_NAME_ATTRIBUTES_DYNAMIC);
        }

        return this;
    }

    /**
     * Method to add an attribute to the filter if dynamic exclusion is enabled
     *
     * @param objectName the ObjectName
     * @param attributeName the attribute name
     */
    public void add(ObjectName objectName, String attributeName) {
        if (dynamicExclusion) {
            Set<String> attribteNameSet =
                    excludeObjectNameAttributesMap.computeIfAbsent(
                            objectName, o -> Collections.synchronizedSet(new HashSet<>()));

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

        if (excludeObjectNameAttributesMap.size() > 0) {
            Set<String> attributeNameSet = excludeObjectNameAttributesMap.get(objectName);
            if (attributeNameSet != null) {
                result = attributeNameSet.contains(attributeName);
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
