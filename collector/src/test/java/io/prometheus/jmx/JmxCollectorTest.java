package io.prometheus.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.lang.management.ManagementFactory;
import java.util.logging.LogManager;
import javax.management.MBeanServer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class JmxCollectorTest {

    private PrometheusRegistry prometheusRegistry;
    private PrometheusRegistryUtils prometheusRegistryUtils;

    @BeforeClass
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
    }

    @Before
    public void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        prometheusRegistryUtils = new PrometheusRegistryUtils(prometheusRegistry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRulesMustHaveNameWithHelp() throws Exception {
        JmxCollector jc = new JmxCollector("---\nrules:\n- help: foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRulesMustHaveNameWithLabels() throws Exception {
        JmxCollector jc = new JmxCollector("---\nrules:\n- labels: {}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRulesMustHavePatternWithName() throws Exception {
        JmxCollector jc = new JmxCollector("---\nrules:\n- name: foo");
    }

    @Test
    public void testNameIsReplacedOnMatch() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testSnakeCaseAttrName() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replace_block_op_min_time:`\n  name: foo\n  attrNameSnakeCase: true"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testLabelsAreSet() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    l: v"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("foo", new String[] {"l"}, new String[] {"v"}), .001);
    }

    @Test
    public void testEmptyLabelsAreIgnored() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    '': v\n    l: ''"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testLowercaseOutputName() throws Exception {
        new JmxCollector(
                        "\n---\nlowercaseOutputName: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testLowercaseOutputLabelNames() throws Exception {
        new JmxCollector(
                        "\n---\nlowercaseOutputLabelNames: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo\n  labels:\n    ABC: DEF"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(200, getSampleValue("Foo", new String[] {"abc"}, new String[] {"DEF"}), .001);
    }

    @Test
    public void testNameAndLabelsFromPattern() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^hadoop<(service)=(DataNode), name=DataNodeActivity-ams-hdd001-50010><>(replaceBlockOpMinTime):`\n  name: hadoop_$3\n  labels:\n    `$1`: `$2`"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(
                200,
                getSampleValue(
                        "hadoop_replaceBlockOpMinTime",
                        new String[] {"service"},
                        new String[] {"DataNode"}),
                .001);
    }

    @Test
    public void testNameAndLabelSanitized() throws Exception {
        new JmxCollector(
                        "\n---\nrules:\n- pattern: `^(hadoop<service=DataNode, )name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: `$1`\n  labels:\n    `$1`: `$1`"
                                .replace('`', '"'))
                .register(prometheusRegistry);
        assertEquals(
                200,
                getSampleValue(
                        "hadoop_service_DataNode_",
                        new String[] {"hadoop_service_DataNode_"},
                        new String[] {"hadoop<service=DataNode, "}),
                .001);
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
        assertNull(getSampleValue("foo", new String[] {}, new String[] {}));
    }

    @Test
    public void defaultExportTest() throws Exception {
        new JmxCollector("---").register(prometheusRegistry);

        // Test JVM bean.
        assertNotNull(
                getSampleValue(
                        "java_lang_OperatingSystem_ProcessCpuTime",
                        new String[] {},
                        new String[] {}));

        // Test Cassandra Bean.
        assertEquals(
                100,
                getSampleValue(
                        "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                        new String[] {},
                        new String[] {}),
                .001);
        // Test Cassandra Metrics.
        assertEquals(
                .2,
                getSampleValue(
                        "org_apache_cassandra_metrics_Compaction_Value",
                        new String[] {"name"},
                        new String[] {"CompletedTasks"}),
                .001);

        // Test Hadoop Metrics.
        assertEquals(
                200,
                getSampleValue(
                        "hadoop_DataNode_replaceBlockOpMinTime",
                        new String[] {"name"},
                        new String[] {"DataNodeActivity-ams-hdd001-50010"}),
                .001);
    }

    @Test
    public void nestedTabularDataTest() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);
        assertEquals(
                338,
                getSampleValue(
                        "Hadoop_DataNodeInfo_DatanodeNetworkCounts",
                        new String[] {"service", "key", "key_"},
                        new String[] {"DataNode", "1.2.3.4", "networkErrors"}),
                .001);
    }

    @Test
    public void tabularDataCompositeKeyTest() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);
        assertEquals(
                1,
                getSampleValue(
                        "org_exist_management_exist_ProcessReport_RunningQueries_id",
                        new String[] {"key_id", "key_path"},
                        new String[] {"1", "/db/query1.xq"}),
                .001);
        assertEquals(
                2,
                getSampleValue(
                        "org_exist_management_exist_ProcessReport_RunningQueries_id",
                        new String[] {"key_id", "key_path"},
                        new String[] {"2", "/db/query2.xq"}),
                .001);
    }

    @Test
    public void testIncludeObjectNames() throws Exception {
        new JmxCollector(
                        "\n---\nincludeObjectNames:\n- java.lang:*\n- java.lang:*\n- org.apache.cassandra.concurrent:*"
                                .replace('`', '"'))
                .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertNotNull(
                getSampleValue(
                        "java_lang_OperatingSystem_ProcessCpuTime",
                        new String[] {},
                        new String[] {}));
        assertNotNull(
                getSampleValue(
                        "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                        new String[] {},
                        new String[] {}));

        assertNull(
                getSampleValue(
                        "org_apache_cassandra_metrics_Compaction_Value",
                        new String[] {"name"},
                        new String[] {"CompletedTasks"}));
        assertNull(
                getSampleValue(
                        "hadoop_DataNode_replaceBlockOpMinTime",
                        new String[] {"name"},
                        new String[] {"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testWhitelist() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nwhitelistObjectNames:\n- java.lang:*\n- java.lang:*\n- org.apache.cassandra.concurrent:*"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertNotNull(
                getSampleValue(
                        "java_lang_OperatingSystem_ProcessCpuTime",
                        new String[] {},
                        new String[] {}));
        assertNotNull(
                getSampleValue(
                        "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                        new String[] {},
                        new String[] {}));

        assertNull(
                getSampleValue(
                        "org_apache_cassandra_metrics_Compaction_Value",
                        new String[] {"name"},
                        new String[] {"CompletedTasks"}));
        assertNull(
                getSampleValue(
                        "hadoop_DataNode_replaceBlockOpMinTime",
                        new String[] {"name"},
                        new String[] {"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testExcludeObjectNames() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nincludeObjectNames:\n- java.lang:*\n- org.apache.cassandra.concurrent:*\nexcludeObjectNames:\n- org.apache.cassandra.concurrent:*"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertNotNull(
                getSampleValue(
                        "java_lang_OperatingSystem_ProcessCpuTime",
                        new String[] {},
                        new String[] {}));

        assertNull(
                getSampleValue(
                        "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                        new String[] {},
                        new String[] {}));
        assertNull(
                getSampleValue(
                        "org_apache_cassandra_metrics_Compaction_Value",
                        new String[] {"name"},
                        new String[] {"CompletedTasks"}));
        assertNull(
                getSampleValue(
                        "hadoop_DataNode_replaceBlockOpMinTime",
                        new String[] {"name"},
                        new String[] {"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testBlacklist() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nwhitelistObjectNames:\n- java.lang:*\n- org.apache.cassandra.concurrent:*\nblacklistObjectNames:\n- org.apache.cassandra.concurrent:*"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);

        // Test what should and shouldn't be present.
        assertNotNull(
                getSampleValue(
                        "java_lang_OperatingSystem_ProcessCpuTime",
                        new String[] {},
                        new String[] {}));

        assertNull(
                getSampleValue(
                        "org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount",
                        new String[] {},
                        new String[] {}));
        assertNull(
                getSampleValue(
                        "org_apache_cassandra_metrics_Compaction_Value",
                        new String[] {"name"},
                        new String[] {"CompletedTasks"}));
        assertNull(
                getSampleValue(
                        "hadoop_DataNode_replaceBlockOpMinTime",
                        new String[] {"name"},
                        new String[] {"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testDefaultExportLowercaseOutputName() throws Exception {
        JmxCollector jc =
                new JmxCollector("---\nlowercaseOutputName: true").register(prometheusRegistry);
        assertNotNull(
                getSampleValue(
                        "java_lang_operatingsystem_processcputime",
                        new String[] {},
                        new String[] {}));
    }

    @Test
    public void testServletRequestPattern() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: 'Catalina<j2eeType=Servlet, WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]),\n    name=([-a-zA-Z0-9+/$%~_-|!.]*), J2EEApplication=none, \nJ2EEServer=none><>RequestCount:'\n  name: tomcat_request_servlet_count\n  labels:\n    module: `$1`\n    servlet: `$2`\n  help: Tomcat servlet request count\n  type: COUNTER\n  attrNameSnakeCase: false"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertEquals(
                1.0,
                getSampleValue(
                        "tomcat_request_servlet_count",
                        new String[] {"module", "servlet"},
                        new String[] {"localhost/host-manager", "HTMLHostManager"}),
                .001);
    }

    @Test
    public void testBooleanValues() throws Exception {
        JmxCollector jc = new JmxCollector("---").register(prometheusRegistry);

        assertEquals(
                1.0, getSampleValue("boolean_Test_True", new String[] {}, new String[] {}), .001);
        assertEquals(
                0.0, getSampleValue("boolean_Test_False", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testValueEmpty() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value:"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertNull(getSampleValue("foo", new String[] {}, new String[] {}));
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
        assertEquals(10.0, getSampleValue("foo", new String[] {}, new String[] {}), .001);
    }

    @Test
    public void testValueIgnoreNonNumber() throws Exception {
        JmxCollector jc =
                new JmxCollector(
                                "\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: a"
                                        .replace('`', '"'))
                        .register(prometheusRegistry);
        assertNull(getSampleValue("foo", new String[] {}, new String[] {}));
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
        assertEquals(1.0, getSampleValue("bean_running", new String[] {}, new String[] {}), .001);
    }

    @Test(expected = IllegalStateException.class)
    public void testDelayedStartNotReady() throws Exception {
        JmxCollector jc =
                new JmxCollector("---\nstartDelaySeconds: 1").register(prometheusRegistry);
        assertNull(getSampleValue("boolean_Test_True", new String[] {}, new String[] {}));
        fail();
    }

    @Test
    public void testDelayedStartReady() throws Exception {
        // TODO register calls the collector, which is in start delay seconds, need to understand
        // how to handle
        JmxCollector jc = new JmxCollector("---\nstartDelaySeconds: 1");
        Thread.sleep(2000);
        jc.register(prometheusRegistry);
        assertEquals(
                1.0, getSampleValue("boolean_Test_True", new String[] {}, new String[] {}), .001);
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
        assertEquals(Camel.EXPECTED_SECONDS, actual, 0);
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

        assertEquals(Double.valueOf(2), value);

        value =
                getSampleValue(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_bootstraps",
                        new String[] {},
                        new String[] {});

        assertEquals(Double.valueOf(4), value);

        value =
                getSampleValue(
                        "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_bootstraps_deferred",
                        new String[] {},
                        new String[] {});

        assertEquals(Double.valueOf(6), value);
    }

    private Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleValue(name, labelNames, labelValues);
    }
}
