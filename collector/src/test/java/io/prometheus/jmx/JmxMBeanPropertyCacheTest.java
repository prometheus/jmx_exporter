/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.ObjectName;
import org.junit.Test;

public class JmxMBeanPropertyCacheTest {

    @Test
    public void testSingleObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(new ObjectName("com.organisation:name=value"));
        assertSameElementsAndOrder(parameterList, "name", "value");
    }

    @Test
    public void testSimpleObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=value,name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "value", "name2", "value2");
    }

    @Test
    public void testQuotedObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=value,name2=\"value2\""));
        assertSameElementsAndOrder(parameterList, "name", "value", "name2", "\"value2\"");
    }

    @Test
    public void testQuotedObjectNameWithComma() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=\"value,more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value,more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithEquals() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=\"value=more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value=more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithQuote() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=\"value\\\"more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value\\\"more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithBackslash() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName("com.organisation:name=\"value\\\\more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value\\\\more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithMultipleQuoted() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName(
                                "com.organisation:name=\"value\\\\\\?\\*\\n"
                                        + "\\\",:=more\",name2= value2 "));
        assertSameElementsAndOrder(
                parameterList, "name", "\"value\\\\\\?\\*\\n\\\",:=more\"", "name2", " value2 ");
    }

    @Test
    public void testIssue52() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList =
                testCache.getKeyPropertyList(
                        new ObjectName(
                                "org.apache.camel:context=ourinternalname,type=endpoints,name=\"seda://endpointName\\?concurrentConsumers=8&size=50000\""));
        assertSameElementsAndOrder(
                parameterList,
                "context",
                "ourinternalname",
                "type",
                "endpoints",
                "name",
                "\"seda://endpointName\\?concurrentConsumers=8&size=50000\"");
    }

    @Test
    public void testIdempotentGet() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        ObjectName testObjectName = new ObjectName("com.organisation:name=value");
        LinkedHashMap<String, String> parameterListFirst =
                testCache.getKeyPropertyList(testObjectName);
        LinkedHashMap<String, String> parameterListSecond =
                testCache.getKeyPropertyList(testObjectName);
        assertEquals(parameterListFirst, parameterListSecond);
    }

    @Test
    public void testGetAfterDeleteOneObject() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        ObjectName testObjectName = new ObjectName("com.organisation:name=value");
        LinkedHashMap<String, String> parameterListFirst =
                testCache.getKeyPropertyList(testObjectName);
        assertNotNull(parameterListFirst);
        testCache.onlyKeepMBeans(Collections.<ObjectName>emptySet());
        assertEquals(
                Collections.<ObjectName, LinkedHashMap<String, String>>emptyMap(),
                testCache.getKeyPropertiesPerBean());
        LinkedHashMap<String, String> parameterListSecond =
                testCache.getKeyPropertyList(testObjectName);
        assertNotNull(parameterListSecond);
    }

    @Test
    public void testRemoveOneOfMultipleObjects() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        ObjectName mBean1 = new ObjectName("com.organisation:name=value1");
        ObjectName mBean2 = new ObjectName("com.organisation:name=value2");
        ObjectName mBean3 = new ObjectName("com.organisation:name=value3");
        testCache.getKeyPropertyList(mBean1);
        testCache.getKeyPropertyList(mBean2);
        testCache.getKeyPropertyList(mBean3);
        Set<ObjectName> keepSet = new HashSet<>();
        keepSet.add(mBean2);
        keepSet.add(mBean3);
        testCache.onlyKeepMBeans(keepSet);
        assertEquals(2, testCache.getKeyPropertiesPerBean().size());
        assertTrue(testCache.getKeyPropertiesPerBean().containsKey(mBean2));
        assertTrue(testCache.getKeyPropertiesPerBean().containsKey(mBean3));
    }

    @Test
    public void testRemoveEmptyIdempotent() {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        testCache.onlyKeepMBeans(Collections.emptySet());
        testCache.onlyKeepMBeans(Collections.emptySet());
        assertEquals(0, testCache.getKeyPropertiesPerBean().size());
    }

    private void assertSameElementsAndOrder(LinkedHashMap<?, ?> actual, Object... expected) {
        assert expected.length % 2 == 0;
        List<Map.Entry<?, ?>> actualList = new ArrayList<>(actual.entrySet());
        List<Map.Entry<?, ?>> expectedList = new ArrayList<>();
        for (int i = 0; i < expected.length / 2; i++) {
            expectedList.add(
                    new AbstractMap.SimpleImmutableEntry<>(expected[i * 2], expected[i * 2 + 1]));
        }
        assertEquals(expectedList, actualList);
    }
}
