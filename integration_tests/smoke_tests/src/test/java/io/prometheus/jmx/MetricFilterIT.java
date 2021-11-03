package io.prometheus.jmx;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Smoke test for the metricFilter configuration.
 * <p/>
 * Run with
 * <pre>mvn verify</pre>
 */
@RunWith(Parameterized.class)
public class MetricFilterIT {

    // The jvm_* metrics come from the DefaultExports, which are only available in the agent.
    private final String JVM_THREADS_DEADLOCKED = "jvm_threads_deadlocked";
    private final String JVM_THREADS_DEADLOCKED_MONITOR = "jvm_threads_deadlocked_monitor";

    // The io_prometheus_* metrics are from JMX beans explicitly defined in the JmxExampleApplication
    private final String IO_PROMETHEUS_JMX_TABULARDATA_SERVER_1_DISK_USAGE_TABLE_AVAIL = "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_avail";
    private final String IO_PROMETHEUS_JMX_TABULARDATA_SERVER_2_DISK_USAGE_TABLE_AVAIL = "io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_avail";

    // The java_lang_* metrics are from standard JMX beans included in the JVM
    private final String JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_USED = "java_lang_Memory_HeapMemoryUsage_used";
    private final String JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_COMMITTED = "java_lang_Memory_HeapMemoryUsage_committed";

    // The jmx_* metrics are about the jmx_exporter itself
    private final String JMX_CONFIG_RELOAD_SUCCESS_TOTAL = "jmx_config_reload_success_total";
    private final String JMX_CONFIG_RELOAD_FAILURE_TOTAL = "jmx_config_reload_failure_total";

    // java_specification_version and java_vm_specification_version are coming from the match pattern
    private final String JAVA_SPECIFICATION_VERSION = "java_specification_version";
    private final String JAVA_VM_SPECIFICATION_VERSION = "java_vm_specification_version";

    private final TestSetup testSetup;
    private final boolean agent;

    @Parameterized.Parameters(name = "{0}")
    public static String[] dist() {
        return new String[]{"agent_java6", "agent", "httpserver" };
    }

    public MetricFilterIT(String distribution) throws IOException, URISyntaxException {
        switch (distribution) {
            case "agent_java6":
                testSetup = new AgentTestSetup("openjdk:11-jre", "jmx_prometheus_javaagent_java6", "config-agent");
                agent = true;
                break;
            case "agent":
                testSetup = new AgentTestSetup("openjdk:11-jre", "jmx_prometheus_javaagent", "config-agent");
                agent = true;
                break;
            case "httpserver":
                testSetup = new HttpServerTestSetup("openjdk:11-jre", "config-httpserver");
                agent = false;
                break;
            default:
                throw new IllegalStateException(distribution + ": unknown distribution");
        }
    }

    @Test
    public void testMetricFilter() throws Exception {
        List<String> metrics = testSetup.scrape(SECONDS.toMillis(10));

        // config.yml
        assertContains(agent, metrics, JVM_THREADS_DEADLOCKED);
        assertContains(agent, metrics, JVM_THREADS_DEADLOCKED_MONITOR);
        assertContains(true, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_1_DISK_USAGE_TABLE_AVAIL);
        assertContains(true, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_2_DISK_USAGE_TABLE_AVAIL);
        assertContains(true, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_USED);
        assertContains(true, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_COMMITTED);
        assertContains(true, metrics, JMX_CONFIG_RELOAD_SUCCESS_TOTAL);
        assertContains(true, metrics, JMX_CONFIG_RELOAD_FAILURE_TOTAL);
        assertContains(true, metrics, JAVA_SPECIFICATION_VERSION);
        assertContains(true, metrics, JAVA_VM_SPECIFICATION_VERSION);

        // config-metric-filter-1.yml
        testSetup.copyConfig("-metric-filter-1.yml");
        metrics = testSetup.scrape(SECONDS.toMillis(10));
        assertContains(false, metrics, JVM_THREADS_DEADLOCKED);
        assertContains(agent, metrics, JVM_THREADS_DEADLOCKED_MONITOR);
        assertContains(false, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_1_DISK_USAGE_TABLE_AVAIL);
        assertContains(true, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_2_DISK_USAGE_TABLE_AVAIL);
        assertContains(false, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_USED);
        assertContains(true, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_COMMITTED);
        assertContains(false, metrics, JMX_CONFIG_RELOAD_SUCCESS_TOTAL);
        assertContains(true, metrics, JMX_CONFIG_RELOAD_FAILURE_TOTAL);
        assertContains(false, metrics, JAVA_SPECIFICATION_VERSION);
        assertContains(true, metrics, JAVA_VM_SPECIFICATION_VERSION);

        // config-metric-filter-2.yml
        testSetup.copyConfig("-metric-filter-2.yml");
        metrics = testSetup.scrape(SECONDS.toMillis(10));
        assertContains(false, metrics, JVM_THREADS_DEADLOCKED);
        assertContains(false, metrics, JVM_THREADS_DEADLOCKED_MONITOR);
        assertContains(false, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_1_DISK_USAGE_TABLE_AVAIL);
        assertContains(false, metrics, IO_PROMETHEUS_JMX_TABULARDATA_SERVER_2_DISK_USAGE_TABLE_AVAIL);
        assertContains(false, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_USED);
        assertContains(false, metrics, JAVA_LANG_MEMORY_HEAPMEMORYUSAGE_COMMITTED);
        assertContains(false, metrics, JMX_CONFIG_RELOAD_SUCCESS_TOTAL);
        assertContains(false, metrics, JMX_CONFIG_RELOAD_FAILURE_TOTAL);
        assertContains(false, metrics, JAVA_SPECIFICATION_VERSION);
        assertContains(false, metrics, JAVA_VM_SPECIFICATION_VERSION);
    }

    private void assertContains(boolean shouldContain, List<String> metrics, String name) {
        boolean contains = metrics.stream()
                .anyMatch(line -> line.startsWith(name + " ") || line.startsWith(name + "{"));
        if (shouldContain) {
            Assert.assertTrue(name + " should exist", contains);
        } else {
            Assert.assertFalse(name + " should not exist", contains);
        }
    }
}
