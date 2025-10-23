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

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.lang.management.ManagementFactory;
import java.util.logging.LogManager;
import javax.management.MBeanServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JmxCollectorTest {

    private PrometheusRegistry prometheusRegistry;
    private PrometheusRegistryUtils prometheusRegistryUtils;

    @BeforeAll
    public static void classSetUp() throws Exception {
        LogManager.getLogManager()
                .readConfiguration(
                        JmxCollectorTest.class.getResourceAsStream("/logging.properties"));

        // Get the Platform MBean Server.
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Register the MBeans.
        CollidingName.registerBeans(mbs);
        PerformanceMetrics.registerBean(mbs);
        TotalValue.registerBean(mbs);
        Cassandra.registerBean(mbs);
        CassandraMetrics.registerBean(mbs);
        Hadoop.registerBean(mbs);
        HadoopDataNode.registerBean(mbs);
        ExistDb.registerBean(mbs);
        BeanWithEnum.registerBean(mbs);
        TomcatServlet.registerBean(mbs);
        Bool.registerBean(mbs);
        Camel.registerBean(mbs);
        CustomValue.registerBean(mbs);
        StringValue.registerBean(mbs);
        KafkaClient.registerBean(mbs);
    }

    @BeforeEach
    public void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        prometheusRegistryUtils = new PrometheusRegistryUtils(prometheusRegistry);
    }

    @Test
    public void testRulesMustHaveNameWithHelp() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxCollector("---\nrules:\n- help: foo"));
    }

    @Test
    public void testRulesMustHaveNameWithLabels() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxCollector("---\nrules:\n- labels: {}"));
    }

    @Test
    public void testRulesMustHavePatternWithName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new JmxCollector("---\nrules:\n- name: foo"));
    }

    @Test
    public void testNameIsReplacedOnMatch() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testSnakeCaseAttrName() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replace_block_op_min_time:`\n  name: foo\n  attrNameSnakeCase: true"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testLabelsAreSet() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    l: v"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {"l"}, new String[] {"v"}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testEmptyLabelsAreIgnored() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    '': v\n    l: ''"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testLowercaseOutputName() throws Exception {
        new JmxCollector(
                        "\n---\nlowercaseOutputName: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testLowercaseOutputLabelNames() throws Exception {
        new JmxCollector(
                        "\n---\nlowercaseOutputLabelNames: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo\n  labels:\n    ABC: DEF"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("Foo", new String[] {"abc"}, new String[] {"DEF"}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testNameAndLabelsFromPattern() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<(service)=(DataNode), name=DataNodeActivity-ams-hdd001-50010><>(replaceBlockOpMinTime):`\n  name: hadoop_$3\n  labels:\n    `$1`: `$2`"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "hadoop_replaceBlockOpMinTime",
                                new String[] {"service"},
                                new String[] {"DataNode"}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testNameAndLabelSanitized() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^(hadoop<service=DataNode, )name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: `$1`\n  labels:\n    `$1`: `$1`"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "hadoop_service_DataNode_",
                                new String[] {"hadoop_service_DataNode_"},
                                new String[] {"hadoop<service=DataNode, "}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void testMetricCustomizers() throws Exception {
        new JmxCollector(
                        "\n---\nincludeObjectNames: [`io.prometheus.jmx:type=customValue`]\nmetricCustomizers:\n   - mbeanFilter:\n        domain: io.prometheus.jmx\n        properties:\n           type: customValue\n     attributesAsLabels:\n        - Text"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "io_prometheus_jmx_customValue_Value",
                                new String[] {"Text"},
                                new String[] {"value"}))
                .isCloseTo(345, within(0.001));
    }

    @Test
    public void testMetricCustomizersExtraMetrics() throws Exception {
        new JmxCollector(
                        "\n---\nincludeObjectNames: [`io.prometheus.jmx:type=stringValue`]\nmetricCustomizers:\n   - mbeanFilter:\n        domain: io.prometheus.jmx\n        properties:\n           type: stringValue\n     extraMetrics:\n        - name: isActive\n          value: true\n          description: This is a boolean value indicating if the scenario is still active or is completed."
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "io_prometheus_jmx_stringValue_isActive",
                                new String[] {},
                                new String[] {}))
                .isCloseTo(1.0, within(0.001));
    }

    @Test
    public void testMetricCustomizersAttributesAsLabelsExtraMetrics() throws Exception {
        new JmxCollector(
                        "\n---\nincludeObjectNames: [`io.prometheus.jmx:type=stringValue`]\nmetricCustomizers:\n   - mbeanFilter:\n        domain: io.prometheus.jmx\n        properties:\n           type: stringValue\n     attributesAsLabels:\n        - Text\n     extraMetrics:\n        - name: isActive\n          value: true\n          description: This is a boolean value indicating if the scenario is still active or is completed."
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "io_prometheus_jmx_stringValue_isActive",
                                new String[] {"Text"},
                                new String[] {"value"}))
                .isCloseTo(1.0, within(0.001));
    }

    /*
    @Test
    public void testHelpFromPattern() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `^(hadoop)<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  help: bar $1"
                                        .replace('`', '"'))
                        ;
        for (Collector.MetricFamilySamples mfs : jc.collect()) {
            if (mfs.name.equals("foo") && mfs.help.equals("bar hadoop")) {
                return;
            }
        }
        fail("MetricFamilySamples foo with help 'bar hadoop' not found.");
    }
    */

    /*
    @Test
    public void stopsOnFirstMatchingRule() throws Exception {
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n- pattern: `.*`\n  name: bar"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertNotNull(getSampleValue("foo", new String[] {}, new String[] {}));
        assertNull(getSampleValue("bar", new String[] {}, new String[] {}));
    }
    */

    @Test
    public void stopsOnEmptyName() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `.*`\n  name: ''\n- pattern: `.*`\n  name: foo"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {})).isNull();
    }

    @Test
    public void defaultExportTest() throws Exception {
        new JmxCollector("---").register(prometheusRegistry);

        // Test JVM bean.
        assertThat(
                        getSampleValue(
                                "java_lang_OperatingSystem_ProcessCpuTime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        // Test Cassandra Bean.
        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                                new String[] {},
                                new String[] {}))
                .isCloseTo(100, within(0.001));

        // Test Cassandra Metrics.
        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_metrics_Compaction_Value",
                                new String[] {"name"},
                                new String[] {"CompletedTasks"}))
                .isCloseTo(0.2, within(0.001));

        // Test Hadoop Metrics.
        assertThat(
                        getSampleValue(
                                "hadoop_DataNode_replaceBlockOpMinTime",
                                new String[] {"name"},
                                new String[] {"DataNodeActivity-ams-hdd001-50010"}))
                .isCloseTo(200, within(0.001));
    }

    @Test
    public void nestedTabularDataTest() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "Hadoop_DataNodeInfo_DatanodeNetworkCounts",
                                new String[] {"service", "key", "key_"},
                                new String[] {"DataNode", "1.2.3.4", "networkErrors"}))
                .isCloseTo(338, within(0.001));
    }

    @Test
    public void tabularDataCompositeKeyTest() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "org_exist_management_exist_ProcessReport_RunningQueries_id",
                                new String[] {"key_id", "key_path"},
                                new String[] {"1", "/db/query1.xq"}))
                .isCloseTo(1, within(0.001));

        assertThat(
                        getSampleValue(
                                "org_exist_management_exist_ProcessReport_RunningQueries_id",
                                new String[] {"key_id", "key_path"},
                                new String[] {"2", "/db/query2.xq"}))
                .isCloseTo(2, within(0.001));
    }

    @Test
    public void testIncludeObjectNames() throws Exception {
        new JmxCollector(
                        "\n"
                                + "---\n"
                                + "includeObjectNames:\n"
                                + "- java.lang:*\n"
                                + "- java.lang:*\n"
                                + "- org.apache.cassandra.concurrent:*")
                .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertThat(
                        getSampleValue(
                                "java_lang_OperatingSystem_ProcessCpuTime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_metrics_Compaction_Value",
                                new String[] {"name"},
                                new String[] {"CompletedTasks"}))
                .isNull();

        assertThat(
                        getSampleValue(
                                "hadoop_DataNode_replaceBlockOpMinTime",
                                new String[] {"name"},
                                new String[] {"DataNodeActivity-ams-hdd001-50010"}))
                .isNull();
    }

    @Test
    public void testWhitelist() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n"
                                        + "---\n"
                                        + "whitelistObjectNames:\n"
                                        + "- java.lang:*\n"
                                        + "- java.lang:*\n"
                                        + "- org.apache.cassandra.concurrent:*")
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertThat(
                        getSampleValue(
                                "java_lang_OperatingSystem_ProcessCpuTime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_metrics_Compaction_Value",
                                new String[] {"name"},
                                new String[] {"CompletedTasks"}))
                .isNull();

        assertThat(
                        getSampleValue(
                                "hadoop_DataNode_replaceBlockOpMinTime",
                                new String[] {"name"},
                                new String[] {"DataNodeActivity-ams-hdd001-50010"}))
                .isNull();
    }

    @Test
    public void testExcludeObjectNames() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n"
                                        + "---\n"
                                        + "includeObjectNames:\n"
                                        + "- java.lang:*\n"
                                        + "- org.apache.cassandra.concurrent:*\n"
                                        + "excludeObjectNames:\n"
                                        + "- org.apache.cassandra.concurrent:*")
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertThat(
                        getSampleValue(
                                "java_lang_OperatingSystem_ProcessCpuTime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                                new String[] {},
                                new String[] {}))
                .isNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_metrics_Compaction_Value",
                                new String[] {"name"},
                                new String[] {"CompletedTasks"}))
                .isNull();

        assertThat(
                        getSampleValue(
                                "hadoop_DataNode_replaceBlockOpMinTime",
                                new String[] {"name"},
                                new String[] {"DataNodeActivity-ams-hdd001-50010"}))
                .isNull();
    }

    @Test
    public void testBlacklist() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n"
                                        + "---\n"
                                        + "whitelistObjectNames:\n"
                                        + "- java.lang:*\n"
                                        + "- org.apache.cassandra.concurrent:*\n"
                                        + "blacklistObjectNames:\n"
                                        + "- org.apache.cassandra.concurrent:*")
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertThat(
                        getSampleValue(
                                "java_lang_OperatingSystem_ProcessCpuTime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();

        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                                new String[] {},
                                new String[] {}))
                .isNull();
        assertThat(
                        getSampleValue(
                                "org_apache_cassandra_metrics_Compaction_Value",
                                new String[] {"name"},
                                new String[] {"CompletedTasks"}))
                .isNull();
        assertThat(
                        getSampleValue(
                                "hadoop_DataNode_replaceBlockOpMinTime",
                                new String[] {"name"},
                                new String[] {"DataNodeActivity-ams-hdd001-50010"}))
                .isNull();
    }

    @Test
    public void testDefaultExportLowercaseOutputName() throws Exception {
        JmxCollector jc =
                new JmxCollector("---\nlowercaseOutputName: true").register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "java_lang_operatingsystem_processcputime",
                                new String[] {},
                                new String[] {}))
                .isNotNull();
    }

    @Test
    public void testServletRequestPattern() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: 'Catalina<j2eeType=Servlet, WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]),\n    name=([-a-zA-Z0-9+/$%~_-|!.]*), J2EEApplication=none, \nJ2EEServer=none><>RequestCount:'\n  name: tomcat_request_servlet_count\n  labels:\n    module: `$1`\n    servlet: `$2`\n  help: Tomcat servlet request count\n  type: COUNTER\n  attrNameSnakeCase: false"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertThat(
                        getSampleValue(
                                "tomcat_request_servlet_count",
                                new String[] {"module", "servlet"},
                                new String[] {"localhost/host-manager", "HTMLHostManager"}))
                .isCloseTo(1.0, within(0.001));
    }

    @Test
    public void testBooleanValues() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);

        assertThat(getSampleValue("boolean_Test_True", new String[] {}, new String[] {}))
                .isCloseTo(1.0, within(0.001));
        assertThat(getSampleValue("boolean_Test_False", new String[] {}, new String[] {}))
                .isCloseTo(0.0, within(0.001));
    }

    @Test
    public void testValueEmpty() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value:"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {})).isNull();
    }

    /*
    @Test
    public void testDuplicateSamples() throws Exception {
        // The following config will map all beans to Samples with name "foo" with empty labels.
        // We still expect only one "foo" Sample, because all subsequent ones should be dropped.
        JmxCollector jc =
                new JmxCollector("rules:\n- pattern: \".*\"\n  name: foo");
        int numberOfSamples = 0;
        for (MetricFamilySamples mfs : jc.collect()) {
            for (MetricFamilySamples.Sample sample : mfs.samples) {
                if (sample.name.equals("foo") && sample.labelNames.isEmpty()) {
                    numberOfSamples++;
                }
            }
        }
        Assert.assertEquals(
                "Expected exactly one sample with name \"foo\" and empty labels",
                1,
                numberOfSamples);
    }
    */

    /*
    @Test
    public void testValueStatic() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1".replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(1.0, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }
    */

    @Test
    public void testValueCaptureGroup() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `^hadoop<.+-500(10)>`\n  name: foo\n  value: $1"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {}))
                .isCloseTo(10.0, within(.001));
    }

    @Test
    public void testValueIgnoreNonNumber() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: a"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertThat(getSampleValue("foo", new String[] {}, new String[] {})).isNull();
    }

    /*
    @Test
    public void testValueFactorEmpty() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor:"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertEquals(1.0, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }
    */

    /*
    @Test
    public void testValueFactor() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor: 0.001"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertEquals(0.001, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }
    */

    @Test
    public void testEnumValue() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `org.bean.enum<type=StateMetrics.*>State: RUNNING`\n  name: bean_running\n  value: 1"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertThat(getSampleValue("bean_running", new String[] {}, new String[] {}))
                .isCloseTo(1.0, within(.001));
    }

    @Test
    public void testDelayedStartNotReady() throws Exception {
        JmxCollector jc =
                new JmxCollector("---\nstartDelaySeconds: 1").register(prometheusRegistry);
        assertThatIllegalStateException()
                .isThrownBy(
                        () ->
                                getSampleValue(
                                        "boolean_Test_True", new String[] {}, new String[] {}));
    }

    @Test
    public void testDelayedStartReady() throws Exception {
        // TODO register calls the collector, which is in start delay seconds, need to understand
        // how to handle
        JmxCollector jc = new JmxCollector("---\nstartDelaySeconds: 1");
        Thread.sleep(2000);
        jc.register(prometheusRegistry);
        assertThat(getSampleValue("boolean_Test_True", new String[] {}, new String[] {}))
                .isCloseTo(1.0, within(.001));
    }

    @Test
    public void testCamelLastExchangeFailureTimestamp() throws Exception {
        String rulePattern =
                "\n"
                        + "---\n"
                        + "rules:\n"
                        + "- pattern: 'org.apache.camel<context=([^,]+), type=routes,"
                        + " name=\"([^\"]+)\"><>LastExchangeFailureTimestamp'\n"
                        + "  name: org.apache.camel.LastExchangeFailureTimestamp\n"
                        + "  help: Exchanges Last Failure Timestamps\n"
                        + "  type: UNTYPED\n"
                        + "  labels:\n"
                        + "    context: \"$1\"\n"
                        + "    route: \"$2\"\n"
                        + "    type: routes";
        JmxCollector jc = new JmxCollector(rulePattern).register(prometheusRegistry);
        Double actual =
                getSampleValue(
                        "org_apache_camel_LastExchangeFailureTimestamp",
                        new String[] {"context", "route", "type"},
                        new String[] {"my-camel-context", "my-route-name", "routes"});
        assertThat(actual).isEqualTo(Camel.EXPECTED_SECONDS);
    }

    /*
    @Test
    public void testCachedBeansDisabled() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor: 4"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertEquals(
                0.0,
                getSampleValue(
                        "jmx_scrape_cached_beans", new String[] {}, new String[] {}),
                .001);
        assertEquals(4.0, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }
    */

    /*
    @Test
    public void testCachedBeansEnabled() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor: 4\n  cache: true"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertTrue(
                getSampleValue("jmx_scrape_cached_beans", new String[] {}, new String[] {})
                        > 0);
        assertEquals(4.0, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }
    */

    /*
    @Test
    public void testCachedBeansEnabledRetainsHelpAcrossCollections() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor: 4\n  cache: true\n  help: help message"
                                        .replace('`', '"'))
                        ;
        List<MetricFamilySamples> samples = jc.collect();
        assertEquals("help message", samples.get(0).help);
        samples = jc.collect();
        assertEquals("help message", samples.get(0).help);
    }
    */

    @Test
    public void testCompositeData() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `io.prometheus.jmx.test<name=PerformanceMetricsMBean><PerformanceMetrics>.*`\n  attrNameSnakeCase: true"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);

        Double value =
                getSampleValue(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_active_sessions",
                        new String[] {},
                        new String[] {});

        assertThat(value).isEqualTo(Double.valueOf(2));

        value =
                getSampleValue(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_bootstraps",
                        new String[] {},
                        new String[] {});

        assertThat(value).isEqualTo(Double.valueOf(4));

        value =
                getSampleValue(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_bootstraps_deferred",
                        new String[] {},
                        new String[] {});

        assertThat(value).isEqualTo(Double.valueOf(6));
    }

    @Test
    public void testInferCounterTypeFromNameEnabled() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "---\n"
                                        + "inferCounterTypeFromName: true\n"
                                        + "rules:\n"
                                        + "- pattern: 'kafka.consumer<type=.+, client-id=(.+),"
                                        + " node-id=(.+)><>(.+):'\n"
                                        + "  name: kafka_consumer_$3\n"
                                        + "  labels:\n"
                                        + "    client_id: $1\n"
                                        + "    node_id: $2")
                        .register(prometheusRegistry);

        String totalType =
                getSampleType(
                        // the `-total` (or `_total`) suffix in `request-total` attribute is lost
                        "kafka_consumer_request",
                        new String[] {"client_id", "node_id"},
                        new String[] {"my-app-consumer", "node-1"});

        // But with the inferCounterTypeFromName=true, the type of `kafka_consumer_request` is
        // COUNTER which will re-add the `_total` suffix
        assertThat(totalType).isEqualTo("COUNTER");

        // inferCounterTypeFromName has no influence on the request-rate attribute
        String rateType =
                getSampleType(
                        "kafka_consumer_request_rate",
                        new String[] {"client_id", "node_id"},
                        new String[] {"my-app-consumer", "node-1"});

        assertThat(rateType).isEqualTo("UNKNOWN");
    }

    @Test
    public void testInferCounterTypeFromNameDisabled() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "---\n"
                                        + "inferCounterTypeFromName: false\n"
                                        + "rules:\n"
                                        + "- pattern: 'kafka.consumer<type=.+, client-id=(.+),"
                                        + " node-id=(.+)><>(.+):'\n"
                                        + "  name: kafka_consumer_$3\n"
                                        + "  labels:\n"
                                        + "    client_id: $1\n"
                                        + "    node_id: $2")
                        .register(prometheusRegistry);

        String totalType =
                getSampleType(
                        // the `-total` (or `_total`) suffix in `request-total` attribute is lost
                        "kafka_consumer_request",
                        new String[] {"client_id", "node_id"},
                        new String[] {"my-app-consumer", "node-1"});

        // With inferCounterTypeFromName=false, the type of `kafka_consumer_request` is UNKNOWN, so
        // the final name will be `kafka_consumer_request`
        assertThat(totalType).isEqualTo("UNKNOWN");

        // inferCounterTypeFromName has no influence on the request-rate attribute
        String rateType =
                getSampleType(
                        "kafka_consumer_request_rate",
                        new String[] {"client_id", "node_id"},
                        new String[] {"my-app-consumer", "node-1"});

        assertThat(rateType).isEqualTo("UNKNOWN");
    }

    private Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleValue(name, labelNames, labelValues);
    }

    private String getSampleType(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleType(name, labelNames, labelValues);
    }
}
