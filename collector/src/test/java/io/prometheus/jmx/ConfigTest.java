package io.prometheus.jmx;

import io.prometheus.client.Collector;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConfigTest {

    @Test
    public void testNewConfig() throws Config.ConfigException {
        Map<?, ?> data = new Yaml().load(getClass().getResourceAsStream("/test-config-new.yaml"));
        Config config = Config.load(data);
        assertEquals(2, config.getStartDelaySeconds());
        assertFalse(config.isLowercaseOutputName());
        assertFalse(config.isLowercaseOutputLabelNames());
        // collector
        assertEquals("service:jmx:rmi:///jndi/rmi://127.0.0.1:1234/jmxrmi", config.getCollectorConfig().getJmxUrl());
        assertEquals("prometheus", config.getCollectorConfig().getUsername());
        assertEquals("secret", config.getCollectorConfig().getPassword());
        assertTrue(config.getCollectorConfig().isSslEnabled());
        assertTrue(config.getCollectorConfig().isSslClientAuth());
        assertEquals("jmx", config.getCollectorConfig().getSslKeyAlias());
        // httpServer
        assertEquals("127.0.0.1", config.getHttpServerConfig().getAddress());
        assertEquals(Integer.valueOf(9012), config.getHttpServerConfig().getPort());
        assertEquals("scraper", config.getHttpServerConfig().getUsername());
        assertEquals("changeMe", config.getHttpServerConfig().getPassword());
        assertTrue(config.getHttpServerConfig().isSslEnabled());
        assertTrue(config.getHttpServerConfig().isSslClientAuth());
        assertEquals("server", config.getHttpServerConfig().getSslKeyAlias());
        // ssl
        assertEquals("/etc/my-key-store", config.getSslConfig().getSslKeyStore());
        assertEquals("keyStorePwd", config.getSslConfig().getSslKeyStorePassword());
        assertEquals("/etc/my-trust-store", config.getSslConfig().getSslTrustStore());
        assertEquals("trustStorePwd", config.getSslConfig().getSslTrustStorePassword());
        // jmxBeanFilter
        assertEquals(1, config.getJmxBeanFilterConfig().getIncludedObjectNames().size());
        assertEquals("org.apache.cassandra.metrics:*", config.getJmxBeanFilterConfig().getIncludedObjectNames().get(0).toString());
        assertEquals(1, config.getJmxBeanFilterConfig().getExcludedObjectNames().size());
        assertEquals("org.apache.cassandra.metrics:type=ColumnFamily,*", config.getJmxBeanFilterConfig().getExcludedObjectNames().get(0).toString());
        // metricFilter
        assertEquals(1, config.getMetricFilterConfig().getNameMustStartWith().size());
        assertEquals("jvm_", config.getMetricFilterConfig().getNameMustStartWith().get(0));
        assertEquals(1, config.getMetricFilterConfig().getNameMustNotStartWith().size());
        assertEquals("io_", config.getMetricFilterConfig().getNameMustNotStartWith().get(0));
        assertEquals(2, config.getMetricFilterConfig().getNameMustBeEqualTo().size());
        assertEquals("java_lang_Memory_HeapMemoryUsage_used", config.getMetricFilterConfig().getNameMustBeEqualTo().get(0));
        assertEquals("jmx_config_reload_success_total", config.getMetricFilterConfig().getNameMustBeEqualTo().get(1));
        assertEquals(2, config.getMetricFilterConfig().getNameMustNotBeEqualTo().size());
        assertEquals("java_specification_version", config.getMetricFilterConfig().getNameMustNotBeEqualTo().get(0));
        assertEquals("jvm_threads_deadlocked", config.getMetricFilterConfig().getNameMustNotBeEqualTo().get(1));
        // rules
        assertEquals(1, config.getRules().size());
        assertEquals("cassandra_$1_$2", config.getRules().get(0).getName());
        assertEquals("$3", config.getRules().get(0).getValue());
        assertEquals(0.001, config.getRules().get(0).getValueFactor(), 0.00001);
        assertEquals(2, config.getRules().get(0).getLabelNames().size());
        assertEquals("name1", config.getRules().get(0).getLabelNames().get(0));
        assertEquals("name2", config.getRules().get(0).getLabelNames().get(1));
        assertEquals(2, config.getRules().get(0).getLabelValues().size());
        assertEquals("value1", config.getRules().get(0).getLabelValues().get(0));
        assertEquals("value2", config.getRules().get(0).getLabelValues().get(1));
        assertEquals("Cassandra metric $1 $2", config.getRules().get(0).getHelp());
        assertFalse(config.getRules().get(0).isCache());
        assertEquals(Collector.Type.GAUGE, config.getRules().get(0).getType());
        assertFalse(config.getRules().get(0).isAttrNameSnakeCase());
    }

    @Test
    public void testOldConfig() throws Config.ConfigException {
        Map<?, ?> data = new Yaml().load(getClass().getResourceAsStream("/test-config-old.yaml"));
        Config config = Config.load(data);
        assertEquals(2, config.getStartDelaySeconds());
        assertTrue(config.isLowercaseOutputName());
        assertTrue(config.isLowercaseOutputLabelNames());
        // collector
        assertEquals("service:jmx:rmi:///jndi/rmi://127.0.0.1:1234/jmxrmi", config.getCollectorConfig().getJmxUrl());
        assertEquals("prometheus", config.getCollectorConfig().getUsername());
        assertEquals("secret", config.getCollectorConfig().getPassword());
        assertTrue(config.getCollectorConfig().isSslEnabled());
        assertFalse(config.getCollectorConfig().isSslClientAuth());
        assertNull(config.getCollectorConfig().getSslKeyAlias());
        // httpServer
        assertEquals("0.0.0.0", config.getHttpServerConfig().getAddress());
        assertNull(config.getHttpServerConfig().getPort());
        assertNull(config.getHttpServerConfig().getUsername());
        assertNull(config.getHttpServerConfig().getPassword());
        assertFalse(config.getHttpServerConfig().isSslEnabled());
        assertFalse(config.getHttpServerConfig().isSslClientAuth());
        assertNull(config.getHttpServerConfig().getSslKeyAlias());
        // ssl
        assertNull(config.getSslConfig().getSslKeyStore());
        assertNull(config.getSslConfig().getSslKeyStorePassword());
        assertNull(config.getSslConfig().getSslTrustStore());
        assertNull(config.getSslConfig().getSslTrustStorePassword());
        // jmxBeanFilter
        assertEquals(1, config.getJmxBeanFilterConfig().getIncludedObjectNames().size());
        assertEquals("org.apache.cassandra.metrics:*", config.getJmxBeanFilterConfig().getIncludedObjectNames().get(0).toString());
        assertEquals(1, config.getJmxBeanFilterConfig().getExcludedObjectNames().size());
        assertEquals("org.apache.cassandra.metrics:type=ColumnFamily,*", config.getJmxBeanFilterConfig().getExcludedObjectNames().get(0).toString());
        // metricFilter
        assertEquals(0, config.getMetricFilterConfig().getNameMustStartWith().size());
        assertEquals(0, config.getMetricFilterConfig().getNameMustNotStartWith().size());
        assertEquals(0, config.getMetricFilterConfig().getNameMustBeEqualTo().size());
        assertEquals(0, config.getMetricFilterConfig().getNameMustNotBeEqualTo().size());
        // rules
        assertEquals(1, config.getRules().size());
        assertEquals("cassandra_$1_$2", config.getRules().get(0).getName());
        assertEquals("$3", config.getRules().get(0).getValue());
        assertEquals(0.001, config.getRules().get(0).getValueFactor(), 0.00001);
        assertEquals(2, config.getRules().get(0).getLabelNames().size());
        assertEquals("name1", config.getRules().get(0).getLabelNames().get(0));
        assertEquals("name2", config.getRules().get(0).getLabelNames().get(1));
        assertEquals(2, config.getRules().get(0).getLabelValues().size());
        assertEquals("value1", config.getRules().get(0).getLabelValues().get(0));
        assertEquals("value2", config.getRules().get(0).getLabelValues().get(1));
        assertEquals("Cassandra metric $1 $2", config.getRules().get(0).getHelp());
        assertFalse(config.getRules().get(0).isCache());
        assertEquals(Collector.Type.GAUGE, config.getRules().get(0).getType());
        assertFalse(config.getRules().get(0).isAttrNameSnakeCase());
    }

    @Test
    public void testEmptyConfig() throws Config.ConfigException {
        Config config = Config.load(new HashMap<Object, Object>());
        assertEquals(0, config.getStartDelaySeconds());
        assertNotNull(config.getCollectorConfig());
        assertNotNull(config.getHttpServerConfig());
        assertNotNull(config.getSslConfig());
        assertNotNull(config.getSslConfig());
        assertNotNull(config.getJmxBeanFilterConfig());
        assertNotNull(config.getMetricFilterConfig());
        assertEquals(0, config.getRules().size());
    }
}
