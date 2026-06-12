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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.prometheus.jmx.JmxCollector.MBeanFilter;
import io.prometheus.jmx.JmxCollector.MetricCustomizer;
import io.prometheus.jmx.JmxCollector.SslProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JmxScraperTest {

    private ObjectName testObjectName;
    private RecordingMBeanReceiver receiver;
    private ObjectNameAttributeFilter filter;
    private JmxMBeanPropertyCache cache;
    private JmxScraper scraper;

    @BeforeEach
    void setUp() throws Exception {
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
                Collections.emptyList(),
                filter,
                Collections.<MetricCustomizer>emptyList(),
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
                Collections.emptyList(),
                filter,
                customizers,
                receiver,
                cache);
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
                List<String> attrKeys,
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
        final List<String> attrKeys;
        final String attrName;
        final String attrType;
        final String attrDescription;
        final Object value;

        RecordedBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                List<String> attrKeys,
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
    class DoScrapeWithRealMBeansTests {

        private MBeanServer platformServer;
        private ObjectNameAttributeFilter filter;
        private JmxMBeanPropertyCache cache;
        private RecordingMBeanReceiver receiver;

        @BeforeEach
        void setUp() {
            platformServer = ManagementFactory.getPlatformMBeanServer();
            filter = ObjectNameAttributeFilter.create(new HashMap<>());
            cache = new JmxMBeanPropertyCache();
            receiver = new RecordingMBeanReceiver();
        }

        private JmxScraper createScraper(ObjectName includePattern) {
            return new JmxScraper(
                    "",
                    "",
                    "",
                    new SslProperties(false),
                    Collections.singletonList(includePattern),
                    Collections.emptyList(),
                    filter,
                    Collections.emptyList(),
                    receiver,
                    cache);
        }

        private JmxScraper createScraperWithExclude(ObjectName includePattern, List<ObjectName> excludePatterns) {
            return new JmxScraper(
                    "",
                    "",
                    "",
                    new SslProperties(false),
                    Collections.singletonList(includePattern),
                    excludePatterns,
                    filter,
                    Collections.emptyList(),
                    receiver,
                    cache);
        }

        @Test
        void scrapeStringValue() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx:type=stringValue");
            try {
                StringValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans())
                        .anyMatch(b -> "Text".equals(b.attrName) && "value".equals(b.value));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeBooleanValue() throws Exception {
            ObjectName beanName = new ObjectName("boolean:Type=Test");
            try {
                Bool.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                List<RecordedBean> beans = receiver.getRecordedBeans();
                assertThat(beans).anyMatch(b -> "True".equals(b.attrName) && Boolean.TRUE.equals(b.value));
                assertThat(beans).anyMatch(b -> "False".equals(b.attrName) && Boolean.FALSE.equals(b.value));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeNumberValue() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx:type=customValue");
            try {
                CustomValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans())
                        .anyMatch(b -> "Value".equals(b.attrName)
                                && Integer.valueOf(345).equals(b.value));
                assertThat(receiver.getRecordedBeans())
                        .anyMatch(b -> "Text".equals(b.attrName) && "value".equals(b.value));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeNullValueDoesNotRecord() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx.test:type=nullValue");
            try {
                NullValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).noneMatch(b -> "NullValue".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeArrayValueDoesNotRecord() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx.test:type=arrayValue");
            try {
                ArrayValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).noneMatch(b -> "Scores".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeDateValueConvertsToEpoch() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx.test:type=dateValue");
            try {
                DateValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                RecordedBean dateBean = receiver.getRecordedBeans().stream()
                        .filter(b -> "Timestamp".equals(b.attrName))
                        .findFirst()
                        .orElse(null);
                assertThat(dateBean).isNotNull();
                assertThat(dateBean.attrType).isEqualTo("java.lang.Double");
                assertThat(dateBean.value).isEqualTo(1700000000.0);
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeEnumValueToString() throws Exception {
            ObjectName beanName = new ObjectName("org.bean.enum:type=StateMetrics");
            try {
                BeanWithEnum.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                RecordedBean enumBean = receiver.getRecordedBeans().stream()
                        .filter(b -> "State".equals(b.attrName))
                        .findFirst()
                        .orElse(null);
                assertThat(enumBean).isNotNull();
                assertThat(enumBean.value).isEqualTo("RUNNING");
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeOptionalPresentUnwraps() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx.test:type=optionalValue");
            try {
                OptionalValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                RecordedBean optBean = receiver.getRecordedBeans().stream()
                        .filter(b -> "OptionalPresent".equals(b.attrName))
                        .findFirst()
                        .orElse(null);
                assertThat(optBean).isNotNull();
                assertThat(optBean.value).isEqualTo(42);
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void scrapeUnrecognizedTypeDoesNotRecord() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx.test:type=unsupportedType");
            try {
                UnsupportedType.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).noneMatch(b -> "Location".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
        }

        @Test
        void doScrapeQueriesAndScrapesAllMatchingBeans() throws Exception {
            ObjectName stringValueName = new ObjectName("io.prometheus.jmx:type=stringValue");
            ObjectName customValueName = new ObjectName("io.prometheus.jmx:type=customValue");
            ObjectName dateValueName = new ObjectName("io.prometheus.jmx.test:type=dateValue");
            try {
                StringValue.registerBean(platformServer);
                CustomValue.registerBean(platformServer);
                DateValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Text".equals(b.attrName));
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Value".equals(b.attrName));
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Timestamp".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(stringValueName)) {
                    platformServer.unregisterMBean(stringValueName);
                }
                if (platformServer.isRegistered(customValueName)) {
                    platformServer.unregisterMBean(customValueName);
                }
                if (platformServer.isRegistered(dateValueName)) {
                    platformServer.unregisterMBean(dateValueName);
                }
            }
        }

        @Test
        void doScrapeFiltersByIncludePattern() throws Exception {
            ObjectName stringValueName = new ObjectName("io.prometheus.jmx:type=stringValue");
            ObjectName nullValueName = new ObjectName("io.prometheus.jmx.test:type=nullValue");
            try {
                StringValue.registerBean(platformServer);
                NullValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(new ObjectName("io.prometheus.jmx:*"));
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Text".equals(b.attrName));
                assertThat(receiver.getRecordedBeans()).noneMatch(b -> "NullValue".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(stringValueName)) {
                    platformServer.unregisterMBean(stringValueName);
                }
                if (platformServer.isRegistered(nullValueName)) {
                    platformServer.unregisterMBean(nullValueName);
                }
            }
        }

        @Test
        void doScrapeRespectsExcludePattern() throws Exception {
            ObjectName stringValueName = new ObjectName("io.prometheus.jmx:type=stringValue");
            ObjectName nullValueName = new ObjectName("io.prometheus.jmx.test:type=nullValue");
            try {
                StringValue.registerBean(platformServer);
                NullValue.registerBean(platformServer);
                List<ObjectName> excludes = Collections.singletonList(new ObjectName("io.prometheus.jmx.test:*"));
                JmxScraper scraper = createScraperWithExclude(null, excludes);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Text".equals(b.attrName));
                assertThat(receiver.getRecordedBeans()).noneMatch(b -> "NullValue".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(stringValueName)) {
                    platformServer.unregisterMBean(stringValueName);
                }
                if (platformServer.isRegistered(nullValueName)) {
                    platformServer.unregisterMBean(nullValueName);
                }
            }
        }

        @Test
        void doScrapeClearsOldCachedBeans() throws Exception {
            ObjectName beanName = new ObjectName("io.prometheus.jmx:type=stringValue");
            try {
                StringValue.registerBean(platformServer);
                JmxScraper scraper = createScraper(null);
                scraper.doScrape();
                assertThat(receiver.getRecordedBeans()).anyMatch(b -> "Text".equals(b.attrName));
                platformServer.unregisterMBean(beanName);
                RecordingMBeanReceiver freshReceiver = new RecordingMBeanReceiver();
                JmxScraper freshScraper = new JmxScraper(
                        "",
                        "",
                        "",
                        new SslProperties(false),
                        Collections.singletonList((ObjectName) null),
                        Collections.emptyList(),
                        filter,
                        Collections.emptyList(),
                        freshReceiver,
                        cache);
                freshScraper.doScrape();
                assertThat(freshReceiver.getRecordedBeans()).noneMatch(b -> "Text".equals(b.attrName));
            } finally {
                if (platformServer.isRegistered(beanName)) {
                    platformServer.unregisterMBean(beanName);
                }
            }
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
                List<String> attrKeys = new ArrayList<>();
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

        @Test
        void filterReturnsFalseWhenDomainMatchesButPropertiesDontMatch() throws Exception {
            MBeanFilter mbeanFilter = new MBeanFilter();
            mbeanFilter.domain = "test.domain";
            HashMap<String, String> props = new HashMap<>();
            props.put("type", "Test");
            props.put("extra", "NotInBean");
            mbeanFilter.properties = props;
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
    class MainMethodTests {

        @Test
        void mainWithNoArgsUsesPlatformMBeanServer() {
            assertThatCode(() -> JmxScraper.main(new String[0])).doesNotThrowAnyException();
        }

        @Test
        void mainWithOneArgCoversShortArgBranch() {
            String url = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi";
            assertThatExceptionOfType(Exception.class).isThrownBy(() -> JmxScraper.main(new String[] {url}));
        }

        @Test
        void mainWithThreeArgsCoversLongArgBranch() {
            String url = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi";
            assertThatExceptionOfType(Exception.class).isThrownBy(() -> JmxScraper.main(new String[] {url, "u", "p"}));
        }

        @Test
        void mainWithThreeArgsAndNonSslFourthArg() {
            String url = "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi";
            assertThatExceptionOfType(Exception.class)
                    .isThrownBy(() -> JmxScraper.main(new String[] {url, "u", "p", "notssl"}));
        }
    }
}
