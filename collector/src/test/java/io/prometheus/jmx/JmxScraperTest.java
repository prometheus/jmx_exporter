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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.prometheus.jmx.JmxCollector.MBeanFilter;
import io.prometheus.jmx.JmxCollector.MetricCustomizer;
import io.prometheus.jmx.JmxCollector.SslProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JmxScraperTest {

    private MBeanServerConnection mockConn;
    private ObjectName testObjectName;
    private RecordingMBeanReceiver receiver;
    private ObjectNameAttributeFilter filter;
    private JmxMBeanPropertyCache cache;
    private JmxScraper scraper;

    @BeforeEach
    void setUp() throws Exception {
        mockConn = mock(MBeanServerConnection.class);
        testObjectName = new ObjectName("test.domain:type=Test");
        receiver = new RecordingMBeanReceiver();
        filter = ObjectNameAttributeFilter.create(new HashMap<>());
        cache = new JmxMBeanPropertyCache();
        scraper = createScraper(receiver);
    }

    private JmxScraper createScraper(JmxScraper.MBeanReceiver receiver) {
        return new JmxScraper(
                "",
                "",
                "",
                new SslProperties(false),
                Collections.singletonList(null),
                new LinkedList<>(),
                filter,
                new LinkedList<>(),
                receiver,
                cache);
    }

    private JmxScraper createScraperWithCustomizers(
            JmxScraper.MBeanReceiver receiver, List<MetricCustomizer> customizers) {
        return new JmxScraper(
                "",
                "",
                "",
                new SslProperties(false),
                Collections.singletonList(null),
                new LinkedList<>(),
                filter,
                customizers,
                receiver,
                cache);
    }

    private void invokeScrapeBean(JmxScraper scraper, MBeanServerConnection conn, ObjectName name) throws Exception {
        Method method = JmxScraper.class.getDeclaredMethod("scrapeBean", MBeanServerConnection.class, ObjectName.class);
        method.setAccessible(true);
        method.invoke(scraper, conn, name);
    }

    @Nested
    class ScrapeBeanErrorPathTests {

        @Test
        void scrapeBeanWithJMExceptionFromGetMBeanInfoDoesNotThrow() throws Exception {
            when(mockConn.getMBeanInfo(testObjectName)).thenThrow(new InstanceNotFoundException("mbean not found"));

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanWithNonReadableAttributeSkipsIt() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo nonReadableAttr =
                    new MBeanAttributeInfo("NonReadableAttr", "java.lang.String", "desc", false, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {nonReadableAttr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanWithNullAttributesReturnsGracefully() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo readableAttr =
                    new MBeanAttributeInfo("ReadableAttr", "java.lang.String", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {readableAttr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(null);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanWithGetAttributesExceptionFallsBackToOneByOne() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo readableAttr =
                    new MBeanAttributeInfo("Attr1", "java.lang.String", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {readableAttr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenThrow(new RuntimeMBeanException(new RuntimeException("bulk get failed")));
            when(mockConn.getAttribute(testObjectName, "Attr1")).thenReturn("value1");

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).attrName).isEqualTo("Attr1");
        }

        @Test
        void scrapeBeanWithNullElementInAttributeListSkipsIt() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo readableAttr =
                    new MBeanAttributeInfo("Attr1", "java.lang.String", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {readableAttr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(null);
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanWithNonAttributeElementInAttributeListSkipsIt() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo readableAttr =
                    new MBeanAttributeInfo("Attr1", "java.lang.String", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {readableAttr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add("not an Attribute object");
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void processAttributesOneByOneSkipsFailedAttributes() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr1 =
                    new MBeanAttributeInfo("GoodAttr", "java.lang.String", "desc", true, false, false);
            MBeanAttributeInfo attr2 =
                    new MBeanAttributeInfo("BadAttr", "java.lang.String", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr1, attr2});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenThrow(new RuntimeMBeanException(new RuntimeException("bulk failed")));
            when(mockConn.getAttribute(testObjectName, "GoodAttr")).thenReturn("goodValue");
            when(mockConn.getAttribute(testObjectName, "BadAttr"))
                    .thenThrow(new AttributeNotFoundException("not found"));

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).attrName).isEqualTo("GoodAttr");
        }
    }

    @Nested
    class ProcessBeanValueTypeTests {

        @Test
        void scrapeBeanWithNumberValueRecordsCorrectly() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr = new MBeanAttributeInfo("Count", "java.lang.Integer", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("Count", 42));
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).value).isEqualTo(42);
        }

        @Test
        void scrapeBeanWithNullValueDoesNotRecord() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr =
                    new MBeanAttributeInfo("NullAttr", "java.lang.Object", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("NullAttr", null));
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanWithBooleanValueRecordsCorrectly() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr =
                    new MBeanAttributeInfo("Enabled", "java.lang.Boolean", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("Enabled", true));
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).value).isEqualTo(true);
        }

        @Test
        void scrapeBeanWithArrayValueDoesNotRecord() throws Exception {
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr =
                    new MBeanAttributeInfo("ArrayAttr", "[Ljava.lang.String;", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr});
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("ArrayAttr", new String[] {"a", "b"}));
            when(mockConn.getAttributes(eq(testObjectName), any(String[].class)))
                    .thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, testObjectName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }
    }

    @Nested
    class RuntimeMBeanSkipTests {

        @Test
        void scrapeBeanSkipsSystemPropertiesAttributeForRuntimeMBean() throws Exception {
            ObjectName runtimeName = new ObjectName("java.lang:type=Runtime");
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo sysPropsAttr =
                    new MBeanAttributeInfo("SystemProperties", "java.util.Map", "desc", true, false, false);
            MBeanAttributeInfo normalAttr =
                    new MBeanAttributeInfo("StartTime", "java.lang.Long", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {sysPropsAttr, normalAttr});
            when(mockConn.getMBeanInfo(runtimeName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("SystemProperties", new HashMap<>()));
            attrList.add(new Attribute("StartTime", 100L));
            when(mockConn.getAttributes(eq(runtimeName), any(String[].class))).thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, runtimeName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).attrName).isEqualTo("StartTime");
        }

        @Test
        void scrapeBeanSkipsClassPathAttributeForRuntimeMBean() throws Exception {
            ObjectName runtimeName = new ObjectName("java.lang:type=Runtime");
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo classPathAttr =
                    new MBeanAttributeInfo("ClassPath", "java.lang.String", "desc", true, false, false);
            MBeanAttributeInfo normalAttr =
                    new MBeanAttributeInfo("Uptime", "java.lang.Long", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {classPathAttr, normalAttr});
            when(mockConn.getMBeanInfo(runtimeName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("ClassPath", "/path/to/classes"));
            attrList.add(new Attribute("Uptime", 200L));
            when(mockConn.getAttributes(eq(runtimeName), any(String[].class))).thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, runtimeName);
            assertThat(receiver.getRecordedBeans()).hasSize(1);
            assertThat(receiver.getRecordedBeans().get(0).attrName).isEqualTo("Uptime");
        }

        @Test
        void scrapeBeanSkipsFlightRecorderMBean() throws Exception {
            ObjectName jfrName = new ObjectName("jdk.management.jfr:type=FlightRecorder");
            MBeanInfo mBeanInfo = mock(MBeanInfo.class);
            MBeanAttributeInfo attr = new MBeanAttributeInfo("MaxSize", "java.lang.Long", "desc", true, false, false);
            when(mBeanInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[] {attr});
            when(mockConn.getMBeanInfo(jfrName)).thenReturn(mBeanInfo);

            AttributeList attrList = new AttributeList();
            attrList.add(new Attribute("MaxSize", 100L));
            when(mockConn.getAttributes(eq(jfrName), any(String[].class))).thenReturn(attrList);

            invokeScrapeBean(scraper, mockConn, jfrName);
            assertThat(receiver.getRecordedBeans()).isEmpty();
        }
    }

    @Nested
    class StdoutWriterTests {

        @Test
        void stdoutWriterRecordsBeanToStdout() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(baos, true));
            try {
                JmxScraper.MBeanReceiver stdoutWriter = createStdoutWriter();

                LinkedHashMap<String, String> beanProperties = new LinkedHashMap<>();
                beanProperties.put("type", "Test");
                LinkedList<String> attrKeys = new LinkedList<>();
                attrKeys.add("key1");

                stdoutWriter.recordBean(
                        "test.domain",
                        beanProperties,
                        Collections.emptyMap(),
                        attrKeys,
                        "AttrName",
                        "java.lang.String",
                        "desc",
                        "hello");

                String output = baos.toString();
                assertThat(output).contains("test.domain");
                assertThat(output).contains("AttrName");
                assertThat(output).contains("hello");
            } finally {
                System.setOut(originalOut);
            }
        }
    }

    @Nested
    class CallSafelyTests {

        @Test
        void callSafelySwallowsExceptions() throws Exception {
            Method callSafelyMethod = JmxScraper.class.getDeclaredMethod("callSafely", Callable[].class);
            callSafelyMethod.setAccessible(true);

            Callable<?> throwingCallable = () -> {
                throw new RuntimeException("test exception");
            };
            Callable<?> successCallable = () -> "success";

            assertThatCode(() -> callSafelyMethod.invoke(
                            scraper, (Object) new Callable<?>[] {throwingCallable, successCallable}))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    class GetMetricCustomizerTests {

        @Test
        void getMetricCustomizerReturnsNullForEmptyList() throws Exception {
            Method method = JmxScraper.class.getDeclaredMethod("getMetricCustomizer", ObjectName.class);
            method.setAccessible(true);

            Object result = method.invoke(scraper, testObjectName);
            assertThat(result).isNull();
        }

        @Test
        void getMetricCustomizerReturnsNullForNonMatchingCustomizer() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "non.matching.domain";
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;
            JmxScraper scraperWithCustomizers =
                    createScraperWithCustomizers(receiver, Collections.singletonList(customizer));

            Method method = JmxScraper.class.getDeclaredMethod("getMetricCustomizer", ObjectName.class);
            method.setAccessible(true);

            Object result = method.invoke(scraperWithCustomizers, testObjectName);
            assertThat(result).isNull();
        }

        @Test
        void getMetricCustomizerReturnsCustomizerForMatchingDomain() throws Exception {
            HashMap<String, String> properties = new HashMap<>();
            properties.put("type", "Test");
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            mbeanFilter.properties = properties;
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;
            JmxScraper scraperWithCustomizers =
                    createScraperWithCustomizers(receiver, Collections.singletonList(customizer));

            Method method = JmxScraper.class.getDeclaredMethod("getMetricCustomizer", ObjectName.class);
            method.setAccessible(true);

            Object result = method.invoke(scraperWithCustomizers, testObjectName);
            assertThat(result).isSameAs(customizer);
        }
    }

    @Nested
    class FilterMbeanByDomainAndPropertiesTests {

        @Test
        void filterReturnsTrueForMatchingDomainAndProperties() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            mbeanFilter.properties = new HashMap<>();
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;

            Method method = JmxScraper.class.getDeclaredMethod(
                    "filterMbeanByDomainAndProperties", ObjectName.class, MetricCustomizer.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(scraper, testObjectName, customizer);
            assertThat(result).isTrue();
        }

        @Test
        void filterReturnsFalseForNonMatchingDomain() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "wrong.domain";
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;

            Method method = JmxScraper.class.getDeclaredMethod(
                    "filterMbeanByDomainAndProperties", ObjectName.class, MetricCustomizer.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(scraper, testObjectName, customizer);
            assertThat(result).isFalse();
        }
    }

    @Nested
    class GetExtraMetricsTests {

        @Test
        void getExtraMetricsReturnsEmptyListWhenNull() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;

            Method method = JmxScraper.class.getDeclaredMethod("getExtraMetrics", MetricCustomizer.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<JmxCollector.ExtraMetric> result = (List<JmxCollector.ExtraMetric>) method.invoke(scraper, customizer);
            assertThat(result).isEmpty();
        }

        @Test
        void getExtraMetricsReturnsMetricsWhenPresent() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            JmxCollector.ExtraMetric extraMetric = new JmxCollector.ExtraMetric();
            extraMetric.name = "extra_name";
            extraMetric.description = "extra_desc";
            extraMetric.value = 1.0;
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;
            customizer.extraMetrics = Collections.singletonList(extraMetric);

            Method method = JmxScraper.class.getDeclaredMethod("getExtraMetrics", MetricCustomizer.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<JmxCollector.ExtraMetric> result = (List<JmxCollector.ExtraMetric>) method.invoke(scraper, customizer);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name).isEqualTo("extra_name");
        }
    }

    @Nested
    class GetAttributesAsLabelsWithValuesTests {

        @Test
        void getAttributesAsLabelsWithValuesMapsAttributes() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;
            customizer.attributesAsLabels = Collections.singletonList("LabelAttr");

            AttributeList attributes = new AttributeList();
            attributes.add(new Attribute("LabelAttr", "labelValue"));
            attributes.add(new Attribute("OtherAttr", "otherValue"));

            Method method = JmxScraper.class.getDeclaredMethod(
                    "getAttributesAsLabelsWithValues", MetricCustomizer.class, AttributeList.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) method.invoke(scraper, customizer, attributes);
            assertThat(result).containsEntry("LabelAttr", "labelValue");
            assertThat(result).doesNotContainKey("OtherAttr");
        }

        @Test
        void getAttributesAsLabelsWithValuesSkipsMissingAttributes() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            MetricCustomizer customizer = new MetricCustomizer();
            customizer.mbeanFilter = mbeanFilter;
            customizer.attributesAsLabels = new ArrayList<>();
            customizer.attributesAsLabels.add("MissingAttr");
            customizer.attributesAsLabels.add("PresentAttr");

            AttributeList attributes = new AttributeList();
            attributes.add(new Attribute("PresentAttr", "present"));

            Method method = JmxScraper.class.getDeclaredMethod(
                    "getAttributesAsLabelsWithValues", MetricCustomizer.class, AttributeList.class);
            method.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, String> result = (Map<String, String>) method.invoke(scraper, customizer, attributes);
            assertThat(result).containsEntry("PresentAttr", "present");
            assertThat(result).doesNotContainKey("MissingAttr");
        }
    }

    private static JmxScraper.MBeanReceiver createStdoutWriter() {
        try {
            Class<?> stdoutWriterClass = Class.forName("io.prometheus.jmx.JmxScraper$StdoutWriter");
            java.lang.reflect.Constructor<?> constructor = stdoutWriterClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (JmxScraper.MBeanReceiver) constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class RecordingMBeanReceiver implements JmxScraper.MBeanReceiver {

        private final List<RecordedBean> recordedBeans = new ArrayList<>();

        @Override
        public void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value) {
            recordedBeans.add(new RecordedBean(
                    domain,
                    beanProperties,
                    attributesAsLabelsWithValues,
                    attrKeys,
                    attrName,
                    attrType,
                    attrDescription,
                    value));
        }

        List<RecordedBean> getRecordedBeans() {
            return recordedBeans;
        }
    }

    static class RecordedBean {

        final String domain;
        final LinkedHashMap<String, String> beanProperties;
        final Map<String, String> attributesAsLabelsWithValues;
        final LinkedList<String> attrKeys;
        final String attrName;
        final String attrType;
        final String attrDescription;
        final Object value;

        RecordedBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object value) {
            this.domain = domain;
            this.beanProperties = beanProperties;
            this.attributesAsLabelsWithValues = attributesAsLabelsWithValues;
            this.attrKeys = attrKeys;
            this.attrName = attrName;
            this.attrType = attrType;
            this.attrDescription = attrDescription;
            this.value = value;
        }
    }

    @Nested
    class SslPropertiesTests {

        @Test
        void sslPropertiesEnabledDefaultIsFalse() {
            JmxCollector.SslProperties sslProperties = new JmxCollector.SslProperties(false);
            assertThat(sslProperties.enabled).isFalse();
        }

        @Test
        void sslPropertiesEnabledCanBeTrue() {
            JmxCollector.SslProperties sslProperties = new JmxCollector.SslProperties(true);
            assertThat(sslProperties.enabled).isTrue();
        }

        @Test
        void sslPropertiesWithEmptyCollections() {
            JmxCollector.SslProperties sslProperties = new JmxCollector.SslProperties(false);
            assertThat(sslProperties.protocols).isEmpty();
            assertThat(sslProperties.ciphers).isEmpty();
        }
    }

    @Nested
    class SslFactoryCreationTests {

        @Test
        void createSslFactoryWithSslDisabled() throws Exception {
            JmxCollector.SslProperties sslProperties = new JmxCollector.SslProperties(false);
            JmxScraper scraper = new JmxScraper(
                    "",
                    "",
                    "",
                    sslProperties,
                    Collections.singletonList(null),
                    new LinkedList<>(),
                    filter,
                    new LinkedList<>(),
                    receiver,
                    cache);

            Method createSslFactory = JmxScraper.class.getDeclaredMethod("createSslFactory");
            createSslFactory.setAccessible(true);

            assertThatCode(() -> createSslFactory.invoke(scraper)).doesNotThrowAnyException();
        }

        @Test
        void createSslFactoryWithSslEnabled() throws Exception {
            JmxCollector.SslProperties sslProperties = new JmxCollector.SslProperties(true);
            JmxScraper scraper = new JmxScraper(
                    "",
                    "",
                    "",
                    sslProperties,
                    Collections.singletonList(null),
                    new LinkedList<>(),
                    filter,
                    new LinkedList<>(),
                    receiver,
                    cache);

            Method createSslFactory = JmxScraper.class.getDeclaredMethod("createSslFactory");
            createSslFactory.setAccessible(true);

            assertThatCode(() -> createSslFactory.invoke(scraper)).doesNotThrowAnyException();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void scrapeBeanHandlesIOExceptionGracefully() throws Exception {
            MBeanServerConnection mockConn = mock(MBeanServerConnection.class);
            ObjectName testObjectName = new ObjectName("test.domain:type=ErrorTest");

            when(mockConn.getMBeanInfo(testObjectName)).thenThrow(new IOException("Test IO Exception"));

            JmxScraper scraper = createScraper(receiver);

            Method scrapeBean =
                    JmxScraper.class.getDeclaredMethod("scrapeBean", MBeanServerConnection.class, ObjectName.class);
            scrapeBean.setAccessible(true);

            assertThatCode(() -> scrapeBean.invoke(scraper, mockConn, testObjectName))
                    .doesNotThrowAnyException();

            assertThat(receiver.getRecordedBeans()).isEmpty();
        }

        @Test
        void scrapeBeanHandlesRuntimeMBeanExceptionGracefully() throws Exception {
            MBeanServerConnection mockConn = mock(MBeanServerConnection.class);
            ObjectName testObjectName = new ObjectName("test.domain:type=RuntimeError");

            MBeanInfo mockInfo = mock(MBeanInfo.class);
            when(mockInfo.getAttributes()).thenReturn(new MBeanAttributeInfo[0]);
            when(mockConn.getMBeanInfo(testObjectName)).thenReturn(mockInfo);
            when(mockConn.getAttributes(testObjectName, new String[] {}))
                    .thenThrow(new RuntimeMBeanException(new RuntimeException("Test runtime error")));

            JmxScraper scraper = createScraper(receiver);

            Method scrapeBean =
                    JmxScraper.class.getDeclaredMethod("scrapeBean", MBeanServerConnection.class, ObjectName.class);
            scrapeBean.setAccessible(true);

            assertThatCode(() -> scrapeBean.invoke(scraper, mockConn, testObjectName))
                    .doesNotThrowAnyException();
        }
    }
}
