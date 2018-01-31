package io.prometheus.jmx;

import org.junit.Assert;
import org.junit.Test;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmxMBeanPropertyCacheTest {

    @Test
    public void testSingleObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=value"));
        assertSameElementsAndOrder(parameterList, "name", "value");
    }

    @Test
    public void testSimpleObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=value,name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "value", "name2", "value2");
    }

    @Test
    public void testQuotedObjectName() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=value,name2=\"value2\""));
        assertSameElementsAndOrder(parameterList, "name", "value", "name2", "\"value2\"");
    }

    @Test
    public void testQuotedObjectNameWithComma() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=\"value,more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value,more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithEquals() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=\"value=more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value=more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithQuote() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=\"value\\\"more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value\\\"more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithBackslash() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=\"value\\\\more\",name2=value2"));
        assertSameElementsAndOrder(parameterList, "name", "\"value\\\\more\"", "name2", "value2");
    }

    @Test
    public void testQuotedObjectNameWithMultipleQuoted() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(new ObjectName("com.organisation:name=\"value\\\\\\?\\*\\n\\\",:=more\",name2= value2 "));
        assertSameElementsAndOrder(parameterList, "name", "\"value\\\\\\?\\*\\n\\\",:=more\"", "name2", " value2 ");
    }

    @Test
    public void testIssue52() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        LinkedHashMap<String, String> parameterList = testCache.getKeyPropertyList(
                new ObjectName("org.apache.camel:context=ourinternalname,type=endpoints,name=\"seda://endpointName\\?concurrentConsumers=8&size=50000\""));
        assertSameElementsAndOrder(parameterList,
                "context", "ourinternalname",
                "type", "endpoints",
                "name", "\"seda://endpointName\\?concurrentConsumers=8&size=50000\"");
    }

    @Test
    public void testIdempotentGet() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        ObjectName testObjectName = new ObjectName("com.organisation:name=value");
        LinkedHashMap<String, String> parameterListFirst = testCache.getKeyPropertyList(testObjectName);
        LinkedHashMap<String, String> parameterListSecond = testCache.getKeyPropertyList(testObjectName);
        assertEquals(parameterListFirst, parameterListSecond);
    }

    @Test
    public void testGetAfterDeleteOneObject() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        ObjectName testObjectName = new ObjectName("com.organisation:name=value");
        LinkedHashMap<String, String> parameterListFirst = testCache.getKeyPropertyList(testObjectName);
        assertNotNull(parameterListFirst);
        testCache.onlyKeepMBeans(Collections.<ObjectName>emptySet());
        assertEquals(Collections.<ObjectName, LinkedHashMap<String,String>>emptyMap(), testCache.getKeyPropertiesPerBean());
        LinkedHashMap<String, String> parameterListSecond = testCache.getKeyPropertyList(testObjectName);
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
        Set<ObjectName> keepSet = new HashSet<ObjectName>();
        keepSet.add(mBean2);
        keepSet.add(mBean3);
        testCache.onlyKeepMBeans(keepSet);
        assertEquals(2, testCache.getKeyPropertiesPerBean().size());
        assertTrue(testCache.getKeyPropertiesPerBean().keySet().contains(mBean2));
        assertTrue(testCache.getKeyPropertiesPerBean().keySet().contains(mBean3));
    }

    @Test
    public void testRemoveEmptyIdempotent() throws Throwable {
        JmxMBeanPropertyCache testCache = new JmxMBeanPropertyCache();
        testCache.onlyKeepMBeans(Collections.<ObjectName>emptySet());
        testCache.onlyKeepMBeans(Collections.<ObjectName>emptySet());
        assertEquals(testCache.getKeyPropertiesPerBean().size(), 0);
    }

    private void assertSameElementsAndOrder(LinkedHashMap<?, ?> actual, Object... expected) {
        assert expected.length % 2 == 0;
        List<Map.Entry<?,?>> actualList = new ArrayList<Map.Entry<?, ?>>(actual.entrySet());
        List<Map.Entry<?,?>> expectedList = new ArrayList<Map.Entry<?,?>>();
        for (int i = 0; i < expected.length / 2; i++) {
            expectedList.add(new AbstractMap.SimpleImmutableEntry<Object, Object>(expected[i * 2], expected[i * 2 + 1]));
        }
        assertEquals(expectedList, actualList);
    }
}
