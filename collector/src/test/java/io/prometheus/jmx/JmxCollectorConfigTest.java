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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import java.util.logging.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class JmxCollectorConfigTest {

    private PrometheusRegistry prometheusRegistry;
    private PrometheusRegistryUtils prometheusRegistryUtils;

    @BeforeAll
    public static void classSetUp() throws Exception {
        LogManager.getLogManager()
                .readConfiguration(JmxCollectorConfigTest.class.getResourceAsStream("/logging.properties"));

        TestMBeanRegistry.registerTestMBeans();
    }

    @BeforeEach
    public void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        prometheusRegistryUtils = new PrometheusRegistryUtils(prometheusRegistry);
    }

    @Nested
    class StartDelaySecondsTests {

        @Test
        public void testStartDelaySecondsAsStringThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector("---\nstartDelaySeconds: not_a_number"));
        }

        @Test
        public void testNegativeStartDelaySecondsThrowsException() {
            assertThatIllegalArgumentException().isThrownBy(() -> new JmxCollector("---\nstartDelaySeconds: -5"));
        }

        @Test
        public void testZeroStartDelaySecondsIsValid() throws Exception {
            new JmxCollector("---\nstartDelaySeconds: 0").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class HostPortAndJmxUrlTests {

        @Test
        public void testHostPortAndJmxUrlBothSetThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(
                            () -> new JmxCollector(
                                    "---\nhostPort: localhost:1234\njmxUrl: service:jmx:rmi:///jndi/rmi://localhost:1234/jmxrmi"));
        }

        @Test
        public void testHostPortSetsJmxUrl() throws Exception {
            new JmxCollector("---\nhostPort: localhost:9999").register(prometheusRegistry);
        }
    }

    @Nested
    class SslConfigTests {

        @Test
        public void testSslAsBoolean() throws Exception {
            new JmxCollector("---\nssl: true").register(prometheusRegistry);
        }

        @Test
        public void testSslAsMapWithKeyStore() throws Exception {
            new JmxCollector("---\n"
                            + "ssl:\n"
                            + "  enabled: true\n"
                            + "  keyStore:\n"
                            + "    filename: /path/to/keystore\n"
                            + "    type: JKS\n"
                            + "    password: changeit\n"
                            + "  trustStore:\n"
                            + "    filename: /path/to/truststore\n"
                            + "    type: JKS\n"
                            + "    password: changeit\n"
                            + "  protocols: \"TLSv1.2, TLSv1.3\"\n"
                            + "  ciphers: \"AES256, AES128\"")
                    .register(prometheusRegistry);
        }

        @Test
        public void testSslMapEnabledFalse() throws Exception {
            new JmxCollector("---\nssl:\n  enabled: false").register(prometheusRegistry);
        }
    }

    @Nested
    class ExcludeJvmMetricsTests {

        @Test
        public void testExcludeJvmMetricsTrue() throws Exception {
            new JmxCollector("---\nexcludeJvmMetrics: true").register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNull();
        }

        @Test
        public void testExcludeJvmMetricsFalse() throws Exception {
            new JmxCollector("---\nexcludeJvmMetrics: false").register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class MetricCustomizersValidationTests {

        @Test
        public void testMetricCustomizersWithNullListThrowsException() {
            assertThatIllegalArgumentException().isThrownBy(() -> new JmxCollector("---\nmetricCustomizers: null"));
        }

        @Test
        public void testMetricCustomizersMissingMbeanFilterThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(
                            () -> new JmxCollector("---\nmetricCustomizers:\n  - attributesAsLabels:\n      - Text"));
        }

        @Test
        public void testMetricCustomizersMbeanFilterMissingDomainThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector("---\n"
                            + "metricCustomizers:\n"
                            + "  - mbeanFilter:\n"
                            + "      properties:\n"
                            + "        type: customValue\n"
                            + "    attributesAsLabels:\n"
                            + "      - Text"));
        }

        @Test
        public void testMetricCustomizersMissingBothAttributesAndExtraMetrics() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector("---\n"
                            + "metricCustomizers:\n"
                            + "  - mbeanFilter:\n"
                            + "      domain: io.prometheus.jmx\n"
                            + "      properties:\n"
                            + "        type: customValue"));
        }

        @Test
        public void testExtraMetricMissingNameThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector("---\n"
                            + "metricCustomizers:\n"
                            + "  - mbeanFilter:\n"
                            + "      domain: io.prometheus.jmx\n"
                            + "      properties:\n"
                            + "        type: customValue\n"
                            + "    extraMetrics:\n"
                            + "      - value: true"));
        }

        @Test
        public void testExtraMetricMissingValueThrowsException() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector("---\n"
                            + "metricCustomizers:\n"
                            + "  - mbeanFilter:\n"
                            + "      domain: io.prometheus.jmx\n"
                            + "      properties:\n"
                            + "        type: customValue\n"
                            + "    extraMetrics:\n"
                            + "      - name: isActive"));
        }
    }

    @Nested
    class RuleValidationTests {

        @Test
        public void testRuleValueFactorInvalidNumber() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() ->
                            new JmxCollector("---\nrules:\n- pattern: `.*`\n  name: foo\n  valueFactor: not_a_number"
                                    .replace('`', '"')));
        }

        @Test
        public void testRuleTypeUntypedConvertedToUnknown() throws Exception {
            new JmxCollector("---\n"
                            + "rules:\n"
                            + "- pattern: '^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:'\n"
                            + "  name: untyped_metric\n"
                            + "  type: UNTYPED\n"
                            + "  value: 1")
                    .register(prometheusRegistry);

            String type = prometheusRegistryUtils.getSampleType("untyped_metric", new String[] {}, new String[] {});
            assertThat(type).isEqualTo("UNKNOWN");
        }

        @Test
        public void testRuleWithCacheEnabled() throws Exception {
            new JmxCollector("---\nrules:\n- pattern: `.*`\n  name: foo\n  cache: true\n  value: 1".replace('`', '"'))
                    .register(prometheusRegistry);
        }

        @Test
        public void testRuleWithHelpAndLabels() throws Exception {
            new JmxCollector("---\n"
                            + "rules:\n"
                            + "- pattern: '^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:'\n"
                            + "  name: test_help_metric\n"
                            + "  help: This is help text\n"
                            + "  labels:\n"
                            + "    env: prod")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("test_help_metric", new String[] {"env"}, new String[] {"prod"}))
                    .isCloseTo(200, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    class LowercaseOutputTests {

        @Test
        public void testLowercaseOutputLabelNamesWithDefaultRules() throws Exception {
            new JmxCollector("---\nlowercaseOutputLabelNames: true").register(prometheusRegistry);
        }

        @Test
        public void testLowercaseOutputNameWithDefaultRules() throws Exception {
            new JmxCollector("---\nlowercaseOutputName: true").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_operatingsystem_processcputime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class EmptyAndNullConfigTests {

        @Test
        public void testEmptyYamlConfigUsesDefaults() throws Exception {
            new JmxCollector("---").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testMinimalYamlConfig() throws Exception {
            new JmxCollector("{}").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class InferCounterTypeTests {

        @Test
        public void testInferCounterTypeFromNameEnabled() throws Exception {
            new JmxCollector("---\n"
                            + "inferCounterTypeFromName: true\n"
                            + "rules:\n"
                            + "- pattern: 'kafka.consumer<type=.+, client-id=(.+), node-id=(.+)><>(.+):'\n"
                            + "  name: kafka_consumer_$3\n"
                            + "  labels:\n"
                            + "    client_id: $1\n"
                            + "    node_id: $2")
                    .register(prometheusRegistry);

            String totalType = prometheusRegistryUtils.getSampleType(
                    "kafka_consumer_request",
                    new String[] {"client_id", "node_id"},
                    new String[] {"my-app-consumer", "node-1"});

            assertThat(totalType).isEqualTo("COUNTER");
        }

        @Test
        public void testInferCounterTypeFromNameEnabledWithDefaultRules() throws Exception {
            new JmxCollector("---\ninferCounterTypeFromName: true").register(prometheusRegistry);
        }

        @Test
        public void testInferCounterTypeFromNameDisabled() throws Exception {
            new JmxCollector("---\n"
                            + "inferCounterTypeFromName: false\n"
                            + "rules:\n"
                            + "- pattern: 'kafka.consumer<type=.+, client-id=(.+), node-id=(.+)><>(.+):'\n"
                            + "  name: kafka_consumer_$3\n"
                            + "  labels:\n"
                            + "    client_id: $1\n"
                            + "    node_id: $2")
                    .register(prometheusRegistry);

            String totalType = prometheusRegistryUtils.getSampleType(
                    "kafka_consumer_request",
                    new String[] {"client_id", "node_id"},
                    new String[] {"my-app-consumer", "node-1"});

            assertThat(totalType).isEqualTo("UNKNOWN");
        }
    }

    @Nested
    class UsernamePasswordTests {

        @Test
        public void testUsernameConfig() throws Exception {
            new JmxCollector("---\nusername: testuser").register(prometheusRegistry);
        }

        @Test
        public void testPasswordConfig() throws Exception {
            new JmxCollector("---\npassword: testpass").register(prometheusRegistry);
        }
    }

    @Nested
    class WhitelistBlacklistBackwardCompatTests {

        @Test
        public void testBlacklistObjectNamesBackwardCompat() throws Exception {
            new JmxCollector("---\n"
                            + "whitelistObjectNames:\n"
                            + "- java.lang:*\n"
                            + "blacklistObjectNames:\n"
                            + "- java.lang:type=Runtime")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class CachedRulesTests {

        @Test
        public void testCachedRulesWithMultipleCollects() throws Exception {
            new JmxCollector("---\n"
                            + "rules:\n"
                            + "- pattern: '^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:'\n"
                            + "  name: cached_hadoop_metric\n"
                            + "  cache: true")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("cached_hadoop_metric", new String[] {}, new String[] {}))
                    .isCloseTo(200, org.assertj.core.data.Offset.offset(0.001));

            prometheusRegistry.scrape(s -> true);
            assertThat(getSampleValue("cached_hadoop_metric", new String[] {}, new String[] {}))
                    .isCloseTo(200, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        public void testDefaultRuleWithCache() throws Exception {
            new JmxCollector("---\nrules:\n- cache: true").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class AttrNameSnakeCaseTests {

        @Test
        public void testAttrNameSnakeCaseWithDefaultRule() throws Exception {
            new JmxCollector("---\nrules:\n- attrNameSnakeCase: true").register(prometheusRegistry);
            assertThat(getSampleValue("java_lang_OperatingSystem_process_cpu_time", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class ObjectNameAttributeFilterConfigTests {

        @Test
        public void testExcludeObjectNameAttributesConfig() throws Exception {
            new JmxCollector("---\n"
                            + "excludeObjectNameAttributes:\n"
                            + "  \"java.lang:type=OperatingSystem\":\n"
                            + "    - \"ProcessCpuTime\"")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNull();
        }

        @Test
        public void testIncludeObjectNameAttributesConfig() throws Exception {
            new JmxCollector("---\n"
                            + "includeObjectNameAttributes:\n"
                            + "  \"java.lang:type=OperatingSystem\":\n"
                            + "    - \"ProcessCpuTime\"")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testAutoExcludeObjectNameAttributesConfig() throws Exception {
            new JmxCollector("---\nautoExcludeObjectNameAttributes: true").register(prometheusRegistry);
        }
    }

    @Nested
    class RuleValueFactorTests {

        @Test
        public void testRuleWithValidValueFactor() throws Exception {
            new JmxCollector("---\n"
                            + "rules:\n"
                            + "- pattern: '^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:'\n"
                            + "  name: vf_metric\n"
                            + "  valueFactor: 0.001")
                    .register(prometheusRegistry);

            assertThat(getSampleValue("vf_metric", new String[] {}, new String[] {}))
                    .isCloseTo(0.2, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    class LowercaseOutputLabelNamesWithMetricCustomizersTests {

        @Test
        public void testLowercaseOutputLabelNamesWithAttributesAsLabels() throws Exception {
            new JmxCollector("---\n"
                            + "lowercaseOutputLabelNames: true\n"
                            + "includeObjectNames:\n"
                            + "  - 'io.prometheus.jmx:type=customValue'\n"
                            + "metricCustomizers:\n"
                            + "  - mbeanFilter:\n"
                            + "      domain: io.prometheus.jmx\n"
                            + "      properties:\n"
                            + "        type: customValue\n"
                            + "    attributesAsLabels:\n"
                            + "      - Text")
                    .register(prometheusRegistry);

            assertThat(getSampleValue(
                            "io_prometheus_jmx_customValue_Value", new String[] {"text"}, new String[] {"value"}))
                    .isCloseTo(345, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    class LowercaseOutputLabelNamesDefaultExportTests {

        @Test
        public void testLowercaseOutputLabelNamesWithDefaultExport() throws Exception {
            new JmxCollector("---\n"
                            + "lowercaseOutputLabelNames: true\n"
                            + "includeObjectNames:\n"
                            + "  - 'org.apache.cassandra.concurrent:*'")
                    .register(prometheusRegistry);

            assertThat(getSampleValue(
                            "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                            new String[] {},
                            new String[] {}))
                    .isCloseTo(100, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    class LabelRegexExceptionTests {

        @Test
        public void testLabelRegexWithInvalidBackreferenceProducesScrapeError() throws Exception {
            new JmxCollector("---\n"
                            + "rules:\n"
                            + "- pattern: '^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:'\n"
                            + "  name: bad_label_metric\n"
                            + "  labels:\n"
                            + "    label: $999")
                    .register(prometheusRegistry);

            prometheusRegistry.scrape(s -> true);

            Double errorValue =
                    prometheusRegistryUtils.getSampleValue("jmx_scrape_error", new String[] {}, new String[] {});
            assertThat(errorValue).isNotNull();
            assertThat(errorValue).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
        }
    }

    @Nested
    class CollectErrorPathTests {

        @Test
        public void testCollectWithNonExistentJmxUrl() throws Exception {
            new JmxCollector("---\njmxUrl: service:jmx:rmi:///jndi/rmi://localhost:0/jmxrmi")
                    .register(prometheusRegistry);

            MetricSnapshots snapshots = prometheusRegistry.scrape(s -> true);
            assertThat(snapshots).isNotNull();
        }

        @Test
        public void testCollectWithNonExistentJmxUrlAndCredentials() throws Exception {
            new JmxCollector("---\n"
                            + "jmxUrl: service:jmx:rmi:///jndi/rmi://localhost:0/jmxrmi\n"
                            + "username: testuser\n"
                            + "password: testpass")
                    .register(prometheusRegistry);

            MetricSnapshots snapshots = prometheusRegistry.scrape(s -> true);
            assertThat(snapshots).isNotNull();
        }
    }

    @Nested
    class DefaultExportEdgeCaseTests {

        @Test
        public void testDefaultExportWithLowercaseOutputLabelNames() throws Exception {
            new JmxCollector("---\nlowercaseOutputLabelNames: true").register(prometheusRegistry);

            Double value = getSampleValue("hadoop_DataNode_replaceBlockOpMinTime", new String[] {"name"}, new String[] {
                "DataNodeActivity-ams-hdd001-50010"
            });
            assertThat(value).isCloseTo(200, org.assertj.core.data.Offset.offset(0.001));
        }

        @Test
        public void testDefaultExportWithInferCounterTypeFromName() throws Exception {
            new JmxCollector("---\n"
                            + "inferCounterTypeFromName: true\n"
                            + "includeObjectNames:\n"
                            + "  - 'kafka.consumer:*'")
                    .register(prometheusRegistry);

            String type = prometheusRegistryUtils.getSampleType(
                    "kafka_consumer_consumer_node_metrics_request",
                    new String[] {"client_id", "node_id"},
                    new String[] {"my-app-consumer", "node-1"});
            assertThat(type).isEqualTo("COUNTER");
        }
    }

    private Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleValue(name, labelNames, labelValues);
    }
}
