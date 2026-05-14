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
