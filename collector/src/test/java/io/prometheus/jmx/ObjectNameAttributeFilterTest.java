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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ObjectNameAttributeFilterTest {

    static final String CONFIG_EXCLUDE_MAP_FIELD = "configExcludeObjectNameAttributesMap";
    static final String DYNAMIC_EXCLUDE_MAP_FIELD = "dynamicExcludeObjectNameAttributesMap";
    static final String CONFIG_INCLUDE_MAP_FIELD = "includeObjectNameAttributesMap";

    @Test
    public void emptyConfig() throws Exception {
        ObjectNameAttributeFilter filter = initEmptyConfigFilter();

        assertThat(excludeMapSize(filter, CONFIG_EXCLUDE_MAP_FIELD)).isEqualTo(0);
        assertThat(excludeMapSize(filter, DYNAMIC_EXCLUDE_MAP_FIELD)).isEqualTo(0);
        assertThat(includeMapSize(filter)).isEqualTo(0);
    }

    @Test
    public void nonEmptyConfigShouldInitializeConfigExcludeMap() throws Exception {
        ObjectNameAttributeFilter filter = initNonEmptyConfigFilter();

        Map<ObjectName, Set<String>> configExcludeMap =
                getInternalMapValue(filter, CONFIG_EXCLUDE_MAP_FIELD);
        Map<ObjectName, Set<String>> dynamicMap =
                getInternalMapValue(filter, DYNAMIC_EXCLUDE_MAP_FIELD);
        Map<ObjectName, Set<String>> configIncludeMap =
                getInternalMapValue(filter, CONFIG_INCLUDE_MAP_FIELD);

        assertThat(configExcludeMap.size()).isEqualTo(2);
        assertThat(configExcludeMap.get(new ObjectName("java.lang:type=OperatingSystem")).size())
                .isEqualTo(1);
        assertThat(configExcludeMap.get(new ObjectName("java.lang:type=Runtime")).size())
                .isEqualTo(2);
        assertThat(dynamicMap.size()).isEqualTo(0);
        assertThat(configIncludeMap.size()).isEqualTo(2);
        assertThat(configIncludeMap.get(new ObjectName("java.lang:type=Threading")).size())
                .isEqualTo(1);
        assertThat(configIncludeMap.get(new ObjectName("java.lang:type=ClassLoading")).size())
                .isEqualTo(1);
    }

    @Test
    public void excludeShouldCheckConfigExclusionMap() throws Exception {
        ObjectNameAttributeFilter filter = initNonEmptyConfigFilter();

        boolean result = filter.exclude(new ObjectName("java.lang:type=Runtime"), "ClassPath");

        assertThat(result)
                .withFailMessage("java.lang:type=Runtime<>ClassPath should be excluded by config")
                .isTrue();
    }

    @Test
    public void excludeShouldCheckDynamicExclusionMap() throws Exception {
        ObjectNameAttributeFilter filter = initEmptyConfigFilter();
        filter.add(new ObjectName("boolean:Type=Test"), "Value");

        boolean result = filter.exclude(new ObjectName("boolean:Type=Test"), "Value");

        assertThat(result)
                .withFailMessage("boolean:Type=Test<>Value should be excluded dynamically")
                .isTrue();
    }

    @Test
    public void onlyKeepMBeansShouldRemoveDeadDynamicRecords() throws Exception {
        ObjectNameAttributeFilter filter = initEmptyConfigFilter();
        Set<ObjectName> aliveMBeans = getAliveMBeans();
        int size = aliveMBeans.size();
        for (ObjectName objectName : aliveMBeans) {
            filter.add(objectName, "Value");
        }

        Map<ObjectName, Set<String>> dynamicMap =
                getInternalMapValue(filter, DYNAMIC_EXCLUDE_MAP_FIELD);

        Iterator<ObjectName> iterator = aliveMBeans.iterator();
        ObjectName unregisteredObjectName = iterator.next();
        iterator.remove();
        filter.onlyKeepMBeans(aliveMBeans);
        assertThat(dynamicMap.size())
                .withFailMessage("onlyKeepMBeans should shrink the map")
                .isEqualTo(size - 1);
        for (ObjectName objectName : aliveMBeans) {
            assertThat(filter.exclude(objectName, "Value"))
                    .withFailMessage(
                            objectName
                                    + "<>Value should still be excluded dynamically after"
                                    + " onlyKeepMBeans")
                    .isTrue();
        }
        assertThat(filter.exclude(unregisteredObjectName, "Value"))
                .withFailMessage(
                        unregisteredObjectName
                                + "<>Value should not be excluded dynamically before add")
                .isFalse();
    }

    @Test
    public void onlyKeepMBeansShouldNotRemoveConfigRecords() throws Exception {
        ObjectNameAttributeFilter objectNameAttributeFilter = initNonEmptyConfigFilter();
        Set<ObjectName> aliveMBeans = getAliveMBeans();

        for (ObjectName objectName : aliveMBeans) {
            objectNameAttributeFilter.add(objectName, "Value");
        }
        objectNameAttributeFilter.onlyKeepMBeans(Collections.emptySet());
        Map<ObjectName, Set<String>> configExcludeObjectNameAttributesMap =
                getInternalMapValue(objectNameAttributeFilter, CONFIG_EXCLUDE_MAP_FIELD);
        Map<ObjectName, Set<String>> dynamicExcludeObjectNameAttributesMap =
                getInternalMapValue(objectNameAttributeFilter, DYNAMIC_EXCLUDE_MAP_FIELD);
        assertThat(configExcludeObjectNameAttributesMap.size())
                .withFailMessage(
                        "configExcludeObjectNameAttributesMap should be left untouched after"
                                + " onlyKeepMBeans(emptyList())")
                .isEqualTo(2);
        assertThat(dynamicExcludeObjectNameAttributesMap.size())
                .withFailMessage(
                        "dynamicExcludeObjectNameAttributesMap should be empty after"
                                + " onlyKeepMBeans(emptyList())")
                .isEqualTo(0);
        assertThat(
                        objectNameAttributeFilter.exclude(
                                new ObjectName("java.lang:type=Runtime"), "ClassPath"))
                .withFailMessage(
                        "java.lang:type=Runtime<>ClassPath should be excluded by config and not"
                                + " removed by onlyKeepMBeans")
                .isTrue();
        for (ObjectName objectName : aliveMBeans) {
            assertThat(objectNameAttributeFilter.exclude(objectName, "Value"))
                    .withFailMessage(
                            "java.lang:type=Runtime<>ClassPath should be excluded by config and not"
                                    + " removed by onlyKeepMBeans")
                    .isFalse();
        }
    }

    @Test
    public void includeShouldCheckConfigInclusionMap() throws Exception {
        ObjectNameAttributeFilter filter = initNonEmptyConfigFilter();

        boolean result = filter.include(new ObjectName("java.lang:type=Threading"), "ThreadCount");

        assertThat(result)
                .withFailMessage(
                        "java.lang:type=Threading<>ThreadCount should be included by config")
                .isTrue();
    }

    private static ObjectNameAttributeFilter initEmptyConfigFilter() {
        return ObjectNameAttributeFilter.create(
                new Yaml()
                        .load(
                                "---\n"
                                        + "excludeObjectNameAttributes: {}\n"
                                        + "includeObjectNameAttributes: {}\n"));
    }

    private static ObjectNameAttributeFilter initNonEmptyConfigFilter() {
        return ObjectNameAttributeFilter.create(
                new Yaml()
                        .load(
                                "---\n"
                                        + "excludeObjectNameAttributes:\n"
                                        + "  \"java.lang:type=OperatingSystem\":\n"
                                        + "    - \"ObjectName\"\n"
                                        + "  \"java.lang:type=Runtime\":\n"
                                        + "    - \"ClassPath\"\n"
                                        + "    - \"SystemProperties\"\n"
                                        + "includeObjectNameAttributes:\n"
                                        + "  \"java.lang:type=Threading\":\n"
                                        + "    - \"ThreadCount\"\n"
                                        + "  \"java.lang:type=ClassLoading\":\n"
                                        + "    - \"LoadedClassCount\"\n"));
    }

    private static Set<ObjectName> getAliveMBeans() throws MalformedObjectNameException {
        return new HashSet<>(
                Arrays.asList(
                        new ObjectName("boolean:Type=Test1"),
                        new ObjectName("boolean:Type=Test2"),
                        new ObjectName("boolean:Type=Test3"),
                        new ObjectName("boolean:Type=Test4")));
    }

    private int excludeMapSize(ObjectNameAttributeFilter filter, String mapName) throws Exception {
        return getInternalMapValue(filter, mapName).size();
    }

    private int includeMapSize(ObjectNameAttributeFilter filter) throws Exception {
        return getInternalMapValue(filter, CONFIG_INCLUDE_MAP_FIELD).size();
    }

    private Map getInternalMapValue(ObjectNameAttributeFilter filter, String mapName)
            throws Exception {
        return getInternalFieldValue(filter, mapName, Map.class);
    }

    @SuppressWarnings("java:S1172")
    private static <T> T getInternalFieldValue(
            Object object, String fieldName, Class<T> ignoredFieldType) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(object);
    }
}
