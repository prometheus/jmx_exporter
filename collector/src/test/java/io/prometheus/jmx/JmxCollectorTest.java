package io.prometheus.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.json.simple.parser.ParseException;
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
    }

    @Before
    public void setUp() throws Exception {
        registry = new CollectorRegistry();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHaveNameWithHelp() throws ParseException {
      JmxCollector jc = new JmxCollector("{`rules`: [{`help`: `foo`}] }".replace('`', '"'));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHaveNameWithLabels() throws ParseException {
      JmxCollector jc = new JmxCollector("{`rules`: [{`labels`: {}}] }".replace('`', '"'));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testRulesMustHavePatternWithName() throws ParseException {
      JmxCollector jc = new JmxCollector("{`rules`: [{`name`: `foo`}] }".replace('`', '"'));
    }

    @Test
    public void testNameIsReplacedOnMatch() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `foo`}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testSnakeCaseAttrName() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replace_block_op_min_time:`, `name`: `foo`, `attrNameSnakeCase`: true}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLabelsAreSet() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `foo`, `labels`: {`l`: `v`}}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{"l"}, new String[]{"v"}), .001);
    }

    @Test
    public void testEmptyLabelsAreIgnored() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `foo`, `labels`: {``: `v`, `l`: ``}}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLowercaseOutputName() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`lowercaseOutputName`: true, `rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `Foo`}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("foo", new String[]{}, new String[]{}), .001);
    }

    @Test
    public void testLowercaseOutputLabelNames() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`lowercaseOutputLabelNames`: true, `rules`: [{`pattern`: `^hadoop<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `Foo` , `labels`: {`ABC`: `DEF`}}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("Foo", new String[]{"abc"}, new String[]{"DEF"}), .001);
    }

    @Test
    public void testNameAndLabelsFromPattern() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^hadoop<(service)=(DataNode), name=DataNodeActivity-ams-hdd001-50010><>(replaceBlockOpMinTime):`, `name`: `hadoop_$3`, `labels`: {`$1`: `$2`}}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("hadoop_replaceBlockOpMinTime", new String[]{"service"}, new String[]{"DataNode"}), .001);
    }

    @Test
    public void testNameAndLabelSanatized() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^(hadoop<service=DataNode, )name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `$1`, `labels`: {`$1`: `$1`}}]}".replace('`', '"')).register(registry);
      assertEquals(200, registry.getSampleValue("hadoop_service_DataNode_", new String[]{"hadoop_service_DataNode_"}, new String[]{"hadoop<service=DataNode, "}), .001);
    }

    @Test
    public void testHelpFromPattern() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `^(hadoop)<service=DataNode, name=DataNodeActivity-ams-hdd001-50010><>replaceBlockOpMinTime:`, `name`: `foo`, `help`: `bar $1`}]}".replace('`', '"')).register(registry);
      for(Collector.MetricFamilySamples mfs : jc.collect()) {
        if (mfs.name.equals("foo") && mfs.help.equals("bar hadoop")) {
          return;
        }
      }
      fail("MetricFamilySamples foo with help 'bar hadoop' not found.");
    }

    @Test
    public void stopsOnFirstMatchingRule() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `.*`, `name`: `foo`}, {`pattern`: `.*`, `name`: `bar`}] }".replace('`', '"')).register(registry);
      assertNotNull(registry.getSampleValue("foo", new String[]{}, new String[]{}));
      assertNull(registry.getSampleValue("bar", new String[]{}, new String[]{}));
    }

    @Test
    public void defaultExportTest() throws ParseException {
      JmxCollector jc = new JmxCollector("{}").register(registry);

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
    public void testDefaultExportLowercaseOutputName() throws ParseException {
      JmxCollector jc = new JmxCollector("{`lowercaseOutputName`: true}".replace('`', '"')).register(registry);
      assertNotNull(registry.getSampleValue("java_lang_operatingsystem_processcputime", new String[]{}, new String[]{}));
    }

    @Test
    public void testServletRequestPattern() throws ParseException {
      JmxCollector jc = new JmxCollector(
          "{`rules`: [{`pattern`: `Catalina<j2eeType=Servlet, WebModule=//([-a-zA-Z0-9+&@#/%?=~_|!:.,;]*[-a-zA-Z0-9+&@#/%=~_|]), name=([-a-zA-Z0-9+/$%~_-|!.]*), J2EEApplication=none, J2EEServer=none><>RequestCount:`,`name`: `tomcat_request_servlet_count`,`labels`: {`module`:`$1`,`servlet`:`$2` },`help`: `Tomcat servlet request count`,`type`: `COUNTER`,`attrNameSnakeCase`: false}]}".replace('`', '"')).register(registry);
      assertEquals(1.0, registry.getSampleValue("tomcat_request_servlet_count", new String[]{"module", "servlet"}, new String[]{"localhost/host-manager", "HTMLHostManager"}), .001);
    }
}
