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

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.util.logging.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TestMBeanTypesTest {

    private PrometheusRegistry prometheusRegistry;
    private PrometheusRegistryUtils prometheusRegistryUtils;

    @BeforeAll
    public static void classSetUp() throws Exception {
        LogManager.getLogManager()
                .readConfiguration(TestMBeanTypesTest.class.getResourceAsStream("/logging.properties"));

        TestMBeanRegistry.registerTestMBeans();
    }

    @BeforeEach
    public void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        prometheusRegistryUtils = new PrometheusRegistryUtils(prometheusRegistry);
    }

    @Nested
    class DateValueTests {

        @Test
        public void testDateValueIsConvertedToEpochSeconds() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=dateValue'")
                    .register(prometheusRegistry);

            Double value =
                    getSampleValue("io_prometheus_jmx_test_dateValue_Timestamp", new String[] {}, new String[] {});

            assertThat(value).isNotNull();
            assertThat(value).isCloseTo(1700000000.0, org.assertj.core.data.Offset.offset(1.0));
            assertThat(prometheusRegistryUtils.getSampleType(
                            "io_prometheus_jmx_test_dateValue_Timestamp", new String[] {}, new String[] {}))
                    .isEqualTo("GAUGE");
        }

        @Test
        public void testDateValueWithTotalSuffixStillDerivesGauge() throws Exception {
            new JmxCollector("---\n"
                            + "includeObjectNames:\n"
                            + "  - 'io.prometheus.jmx.test:type=dateValue'\n"
                            + "rules:\n"
                            + "  - pattern: 'io.prometheus.jmx.test<type=dateValue><>Timestamp:'\n"
                            + "    name: date_timestamp_total")
                    .register(prometheusRegistry);

            assertThat(prometheusRegistryUtils.getSampleType("date_timestamp", new String[] {}, new String[] {}))
                    .isEqualTo("GAUGE");
        }
    }

    @Nested
    class BooleanTypeDerivationTests {

        @Test
        public void testBooleanValueWithTotalSuffixStillDerivesGauge() throws Exception {
            new JmxCollector("---\n"
                            + "includeObjectNames:\n"
                            + "  - 'boolean:Type=Test'\n"
                            + "rules:\n"
                            + "  - pattern: 'boolean<Type=Test><>True:'\n"
                            + "    name: boolean_flag_total")
                    .register(prometheusRegistry);

            assertThat(prometheusRegistryUtils.getSampleType("boolean_flag", new String[] {}, new String[] {}))
                    .isEqualTo("GAUGE");
        }
    }

    @Nested
    class DescriptorMetricTypeDerivationTests {

        @Test
        public void testDescriptorMetricTypeDerivesCounterAndGauge() throws Exception {
            javax.management.MBeanServer mbs = java.lang.management.ManagementFactory.getPlatformMBeanServer();
            javax.management.ObjectName beanName =
                    new javax.management.ObjectName("io.prometheus.jmx.test:type=descriptorMetrics");
            javax.management.DynamicMBean descriptorBean = new javax.management.DynamicMBean() {
                @Override
                public Object getAttribute(String attribute) {
                    return "requests".equals(attribute) ? 10L : 5L;
                }

                @Override
                public void setAttribute(javax.management.Attribute attribute) {}

                @Override
                public javax.management.AttributeList getAttributes(String[] attributes) {
                    javax.management.AttributeList list = new javax.management.AttributeList();
                    for (String attr : attributes) {
                        list.add(new javax.management.Attribute(attr, getAttribute(attr)));
                    }
                    return list;
                }

                @Override
                public javax.management.AttributeList setAttributes(javax.management.AttributeList attributes) {
                    return new javax.management.AttributeList();
                }

                @Override
                public Object invoke(String actionName, Object[] params, String[] signature) {
                    return null;
                }

                @Override
                public javax.management.MBeanInfo getMBeanInfo() {
                    javax.management.Descriptor counterDesc =
                            new javax.management.ImmutableDescriptor("metricType=counter");
                    javax.management.Descriptor gaugeDesc =
                            new javax.management.ImmutableDescriptor("metricType=gauge");
                    javax.management.MBeanAttributeInfo[] attrs = new javax.management.MBeanAttributeInfo[] {
                        new javax.management.MBeanAttributeInfo(
                                "requests", "long", "Total requests", true, false, false, counterDesc),
                        new javax.management.MBeanAttributeInfo(
                                "connections_total", "long", "Active connections", true, false, false, gaugeDesc)
                    };
                    return new javax.management.MBeanInfo(
                            getClass().getName(), "Descriptor Test MBean", attrs, null, null, null);
                }
            };

            mbs.registerMBean(descriptorBean, beanName);
            try {
                new JmxCollector("---\n" + "includeObjectNames:\n"
                                + "  - 'io.prometheus.jmx.test:type=descriptorMetrics'")
                        .register(prometheusRegistry);

                // 'requests' has descriptor metricType=counter -> derived as COUNTER
                assertThat(prometheusRegistryUtils.getSampleType(
                                "io_prometheus_jmx_test_descriptorMetrics_requests", new String[] {}, new String[] {}))
                        .isEqualTo("COUNTER");

                // 'connections_total' has descriptor metricType=gauge -> derived as GAUGE despite _total suffix
                assertThat(prometheusRegistryUtils.getSampleType(
                                "io_prometheus_jmx_test_descriptorMetrics_connections",
                                new String[] {},
                                new String[] {}))
                        .isEqualTo("GAUGE");
            } finally {
                if (mbs.isRegistered(beanName)) {
                    mbs.unregisterMBean(beanName);
                }
            }
        }
    }

    @Nested
    class NullValueTests {

        @Test
        public void testNullValueIsSkipped() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=nullValue'")
                    .register(prometheusRegistry);

            Double value =
                    getSampleValue("io_prometheus_jmx_test_nullValue_nullValue", new String[] {}, new String[] {});

            assertThat(value).isNull();
        }
    }

    @Nested
    class ArrayValueTests {

        @Test
        public void testArrayValueIsSkipped() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=arrayValue'")
                    .register(prometheusRegistry);

            Double value = getSampleValue("io_prometheus_jmx_test_arrayValue_Scores", new String[] {}, new String[] {});

            assertThat(value).isNull();
        }
    }

    @Nested
    class OptionalValueTests {

        @Test
        public void testOptionalPresentIsUnwrapped() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=optionalValue'")
                    .register(prometheusRegistry);

            Double value = getSampleValue(
                    "io_prometheus_jmx_test_optionalValue_OptionalPresent", new String[] {}, new String[] {});

            assertThat(value).isNotNull();
            assertThat(value).isCloseTo(42.0, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        public void testOptionalEmptyIsSkipped() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=optionalValue'")
                    .register(prometheusRegistry);

            Double value = getSampleValue(
                    "io_prometheus_jmx_test_optionalValue_OptionalEmpty", new String[] {}, new String[] {});

            assertThat(value).isNull();
        }
    }

    @Nested
    class UnsupportedTypeTests {

        @Test
        public void testUnsupportedTypeIsSkipped() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=unsupportedType'")
                    .register(prometheusRegistry);

            Double value =
                    getSampleValue("io_prometheus_jmx_test_unsupportedType_Location", new String[] {}, new String[] {});

            assertThat(value).isNull();
        }

        @Test
        public void testUnsupportedTypeIsAutoExcludedOnSecondScrape() throws Exception {
            new JmxCollector("---\n" + "includeObjectNames:\n" + "  - 'io.prometheus.jmx.test:type=unsupportedValue'")
                    .register(prometheusRegistry);

            prometheusRegistry.scrape(s -> true);

            prometheusRegistry.scrape(s -> true);
        }
    }

    private Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleValue(name, labelNames, labelValues);
    }
}
