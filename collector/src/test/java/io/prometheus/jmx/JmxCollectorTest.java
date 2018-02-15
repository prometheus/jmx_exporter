package io.prometheus.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;


public class JmxCollectorTest {

    CollectorRegistry registry;

    @BeforeClass
    public static void OneTimeSetUp() throws Exception {
        // Get the Platform MBean Server.
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        // Register the MBeans.
        Cassandra.registerBean(mbs);
        CassandraMetrics.registerBean(mbs);
        Hadoop.registerBean(mbs);
        TomcatServlet.registerBean(mbs);
        Bool.registerBean(mbs);
    }

    @Before
    public void setUp() throws Exception {
      registry = new CollectorRegistry();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHaveNameWithHelp() throws Exception {
      JmxCollector jc = new JmxCollector("---\nrules:\n- help: foo");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHaveNameWithLabels() throws Exception {
	  JmxCollector jc = new JmxCollector("---\nrules:\n- labels: {}");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHavePatternWithName() throws Exception {
	  JmxCollector jc = new JmxCollector("---\nrules:\n- name: foo");
    }

    @Test
    public void testNameIsReplacedOnMatch() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testSnakeCaseAttrName() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replace_block_op_min_time:`\n  name: foo\n  attrNameSnakeCase: true".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLabelsAreSet() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    l: v".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{"l"}, new String[]{"v"}), .001);
    }

    @Test
    public void testEmptyLabelsAreIgnored() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  labels:\n    '': v\n    l: ''".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLowercaseOutputName() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nlowercaseOutputName: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLowercaseOutputLabelNames() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nlowercaseOutputLabelNames: true\nrules:\n- pattern: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: Foo\n  labels:\n    ABC: DEF".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("Foo", new String[]{"abc"}, new String[]{"DEF"}), .001);
    }

    @Test
    public void testNameAndLabelsFromPattern() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^hadoop<(service)=(DataNode), name=DataNodeActivity-ams-hdd001-50010><>(replaceBlockOpMinTime):`\n  name: hadoop_$3\n  labels:\n    `$1`: `$2`".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("hadoop_replaceBlockOpMinTime", new String[]{"service"}, new String[]{"DataNode"}), .001);
    }

    @Test
    public void testNameAndLabelSanatized() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^(hadoop<service=DataNode, )name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: `$1`\n  labels:\n    `$1`: `$1`".replace('`','"')).register(registry);
      assertEquals(200, registry.getSampleValue("hadoop_service_DataNode_", new String[]{"hadoop_service_DataNode_"}, new String[]{"hadoop<service=DataNode, "}), .001);
    }

    @Test
    public void testHelpFromPattern() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `^(hadoop)<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`\n  name: foo\n  help: bar $1".replace('`','"')).register(registry);
      for(Collector.MetricFamilySamples mfs : jc.collect()) {
        if (mfs.name.equals("foo") && mfs.help.equals("bar hadoop")) {
          return;
        }
      }
      fail("MetricFamilySamples foo with help 'bar hadoop' not found.");
    }

    @Test
    public void stopsOnFirstMatchingRule() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `.*`\n  name: foo\n- pattern: `.*`\n  name: bar".replace('`','"')).register(registry);
      assertNotNull(registry.getSampleValue("foo", new String[]{}, new String[]{}));
      assertNull(registry.getSampleValue("bar", new String[]{}, new String[]{}));
    }

    @Test
    public void stopsOnEmptyName() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: `.*`\n  name: ''\n- pattern: `.*`\n  name: foo".replace('`','"')).register(registry);
      assertNull(registry.getSampleValue("foo", new String[]{}, new String[]{}));
    }

    @Test
    public void defaultExportTest() throws Exception {
      JmxCollector jc = new JmxCollector("---").register(registry);

      // Test JVM bean.
      assertNotNull(registry.getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[]{}, new String[]{}));

      // Test Cassandra Bean.
      assertEquals(100, registry.getSampleValue("org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount", new String[]{}, new String[]{}), .001);
      // Test Cassandra MEtrics.
      assertEquals(.2, registry.getSampleValue("org_apache_cassandra_metrics_Compaction_Value", new String[]{"name"}, new String[]{"CompletedTasks"}), .001);

      // Test Hadoop Metrics.
      assertEquals(200, registry.getSampleValue("hadoop_DataNode_replaceBlockOpMinTime", new String[]{"name"}, new String[]{"DataNodeActivity-ams-hdd001-50010"}), .001);
    }

    @Test
    public void testWhitelist() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nwhitelistObjectNames:\n- java.lang:*\n- java.lang:*\n- org.apache.cassandra.concurrent:*".replace('`','"')).register(registry);

      // Test what should and shouldn't be present.
      assertNotNull(registry.getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[]{}, new String[]{}));
      assertNotNull(registry.getSampleValue("org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount", new String[]{}, new String[]{}));

      assertNull(registry.getSampleValue("org_apache_cassandra_metrics_Compaction_Value", new String[]{"name"}, new String[]{"CompletedTasks"}));
      assertNull(registry.getSampleValue("hadoop_DataNode_replaceBlockOpMinTime", new String[]{"name"}, new String[]{"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testBlacklist() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nwhitelistObjectNames:\n- java.lang:*\n- org.apache.cassandra.concurrent:*\nblacklistObjectNames:\n- org.apache.cassandra.concurrent:*".replace('`','"')).register(registry);

      // Test what should and shouldn't be present.
      assertNotNull(registry.getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[]{}, new String[]{}));

      assertNull(registry.getSampleValue("org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount", new String[]{}, new String[]{}));
      assertNull(registry.getSampleValue("org_apache_cassandra_metrics_Compaction_Value", new String[]{"name"}, new String[]{"CompletedTasks"}));
      assertNull(registry.getSampleValue("hadoop_DataNode_replaceBlockOpMinTime", new String[]{"name"}, new String[]{"DataNodeActivity-ams-hdd001-50010"}));
    }

    @Test
    public void testDefaultExportLowercaseOutputName() throws Exception {
      JmxCollector jc = new JmxCollector("---\nlowercaseOutputName: true").register(registry);
      assertNotNull(registry.getSampleValue("java_lang_operatingsystem_processcputime", new String[]{}, new String[]{}));
    }

    @Test
    public void testServletRequestPattern() throws Exception {
      JmxCollector jc = new JmxCollector(
              "\n---\nrules:\n- pattern: 'Catalina<j2eeType=Servlet, WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]),\n    name=([-a-zA-Z0-9+/$%~_-|!.]*), J2EEApplication=none, \nJ2EEServer=none><>RequestCount:'\n  name: tomcat_request_servlet_count\n  labels:\n    module: `$1`\n    servlet: `$2`\n  help: Tomcat servlet request count\n  type: COUNTER\n  attrNameSnakeCase: false".replace('`','"')).register(registry);
      assertEquals(1.0, registry.getSampleValue("tomcat_request_servlet_count", new String[]{"module", "servlet"}, new String[]{"localhost/host-manager", "HTMLHostManager"}), .001);
    }

    @Test
    public void testBooleanValues() throws Exception {
      JmxCollector jc = new JmxCollector("---").register(registry);

      assertEquals(1.0, registry.getSampleValue("boolean_Test_True", new String[]{}, new String[]{}), .001);
      assertEquals(0.0, registry.getSampleValue("boolean_Test_False", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testValueEmpty() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value:".replace('`','"')).register(registry);
      assertNull(registry.getSampleValue("foo", new String[]{}, new String[]{}));
    }

    @Test
    public void testValueStatic() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1".replace('`','"')).register(registry);
      assertEquals(1.0, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testValueCaptureGroup() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `^hadoop<.+-500(10)>`\n  name: foo\n  value: $1".replace('`','"')).register(registry);
      assertEquals(10.0, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testValueIgnoreNonNumber() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: a".replace('`','"')).register(registry);
      assertNull(registry.getSampleValue("foo", new String[]{}, new String[]{}));
    }

    @Test
    public void testValueFactorEmpty() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor:".replace('`','"')).register(registry);
      assertEquals(1.0, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testValueFactor() throws Exception {
      JmxCollector jc = new JmxCollector("\n---\nrules:\n- pattern: `.*`\n  name: foo\n  value: 1\n  valueFactor: 0.001".replace('`','"')).register(registry);
      assertEquals(0.001, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test(expected=IllegalStateException.class)
    public void testDelayedStartNotReady() throws Exception {
      JmxCollector jc = new JmxCollector("---\nstartDelaySeconds: 1").register(registry);
      assertNull(registry.getSampleValue("boolean_Test_True", new String[]{}, new String[]{}));
      fail();
    }

    @Test
    public void testDelayedStartReady() throws Exception {
      JmxCollector jc = new JmxCollector("---\nstartDelaySeconds: 1").register(registry);
      Thread.sleep(2000);
      assertEquals(1.0, registry.getSampleValue("boolean_Test_True", new String[]{}, new String[]{}), .001);
    }
}
