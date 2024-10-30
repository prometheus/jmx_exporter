package io.prometheus.jmx;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.*;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ObjectNameAttributeFilterTest {

    static final String CONFIG_EXCLUDE_MAP_FIELD = "configExcludeObjectNameAttributesMap";
    static final String DYNAMIC_EXCLUDE_MAP_FIELD = "dynamicExcludeObjectNameAttributesMap";

    @Test
    public void emptyConfig() throws Exception {
        ObjectNameAttributeFilter filter = initEmptyConfigFilter();

        assertEquals(0, excludeMapSize(filter, CONFIG_EXCLUDE_MAP_FIELD));
        assertEquals(0, excludeMapSize(filter, DYNAMIC_EXCLUDE_MAP_FIELD));
    }

    @Test
    public void nonEmptyConfigShouldInitializeConfigExcludeMap() throws Exception {
        ObjectNameAttributeFilter filter = initNonEmptyConfigFilter();

        Map<ObjectName, Set<String>> configMap =
                getInternalMapValue(filter, CONFIG_EXCLUDE_MAP_FIELD);
        Map<ObjectName, Set<String>> dynamicMap =
                getInternalMapValue(filter, DYNAMIC_EXCLUDE_MAP_FIELD);

        assertEquals(2, configMap.size());
        assertEquals(1, configMap.get(new ObjectName("java.lang:type=OperatingSystem")).size());
        assertEquals(2, configMap.get(new ObjectName("java.lang:type=Runtime")).size());
        assertEquals(0, dynamicMap.size());
    }

    @Test
    public void excludeShouldCheckConfigExclusionMap() throws Exception {
        ObjectNameAttributeFilter filter = initNonEmptyConfigFilter();

        boolean result = filter.exclude(new ObjectName("java.lang:type=Runtime"), "ClassPath");

        assertTrue("java.lang:type=Runtime<>ClassPath should be excluded by config", result);
    }

    @Test
    public void excludeShouldCheckDynamicExclusionMap() throws Exception {
        ObjectNameAttributeFilter filter = initEmptyConfigFilter();
        filter.add(new ObjectName("boolean:Type=Test"), "Value");

        boolean result = filter.exclude(new ObjectName("boolean:Type=Test"), "Value");

        assertTrue("boolean:Type=Test<>Value should be excluded dynamically", result);
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
        assertEquals("onlyKeepMBeans should shrink the map", size - 1, dynamicMap.size());
        for (ObjectName objectName : aliveMBeans) {
            assertTrue(
                    objectName
                            + "<>Value should still be excluded dynamically after onlyKeepMBeans",
                    filter.exclude(objectName, "Value"));
        }
        assertFalse(
                unregisteredObjectName + "<>Value should not be excluded dynamically before add",
                filter.exclude(unregisteredObjectName, "Value"));
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
        assertEquals(
                "configExcludeObjectNameAttributesMap should be left untouched after"
                        + " onlyKeepMBeans(emptyList())",
                2,
                configExcludeObjectNameAttributesMap.size());
        assertEquals(
                "dynamicExcludeObjectNameAttributesMap should be empty after"
                        + " onlyKeepMBeans(emptyList())",
                0,
                dynamicExcludeObjectNameAttributesMap.size());
        assertTrue(
                "java.lang:type=Runtime<>ClassPath should be excluded by config and not removed by"
                        + " onlyKeepMBeans",
                objectNameAttributeFilter.exclude(
                        new ObjectName("java.lang:type=Runtime"), "ClassPath"));
        for (ObjectName objectName : aliveMBeans) {
            assertFalse(
                    objectName + "<>Value should not be excluded dynamically before call to add",
                    objectNameAttributeFilter.exclude(objectName, "Value"));
        }
    }

    private static ObjectNameAttributeFilter initEmptyConfigFilter() {
        return ObjectNameAttributeFilter.create(
                new Yaml().load("---\nexcludeObjectNameAttributes: {}\n"));
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
                                        + "    - \"SystemProperties\"\n"));
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
