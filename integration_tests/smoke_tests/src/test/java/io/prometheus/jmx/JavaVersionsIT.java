package io.prometheus.jmx;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Smoke test with a couple of different Java versions.
 * <p/>
 * Run with
 * <pre>mvn verify</pre>
 */
@RunWith(Parameterized.class)
public class JavaVersionsIT {

    private final TestSetup testSetup;

    @Parameterized.Parameters(name = "{0} - {1}")
    public static String[][] images() {
        return new String[][]{

                // HotSpot
                {"openjdk:8-jre", "httpserver"},
                {"openjdk:8-jre", "agent_java6"},
                {"openjdk:8-jre", "agent"},

                {"openjdk:11-jre", "httpserver"},
                {"openjdk:11-jre", "agent_java6"},
                {"openjdk:11-jre", "agent"},

                {"ticketfly/java:6", "httpserver"},
                {"ticketfly/java:6", "agent_java6"},

                {"openjdk:7", "httpserver"},
                {"openjdk:7", "agent_java6"},
                {"openjdk:7", "agent"},

                {"adoptopenjdk/openjdk16:ubi-minimal-jre", "httpserver"},
                {"adoptopenjdk/openjdk16:ubi-minimal-jre", "agent_java6"},
                {"adoptopenjdk/openjdk16:ubi-minimal-jre", "agent"},

                // OpenJ9
                {"ibmjava:8-jre", "httpserver"},
                {"ibmjava:8-jre", "agent_java6"},
                {"ibmjava:8-jre", "agent"},

                {"ibmjava:8-jre", "httpserver"},
                {"ibmjava:8-jre", "agent_java6"},
                {"ibmjava:8-jre", "agent"},

                {"adoptopenjdk/openjdk11-openj9", "httpserver"},
                {"adoptopenjdk/openjdk11-openj9", "agent_java6"},
                {"adoptopenjdk/openjdk11-openj9", "agent"},
        };
    }

    public JavaVersionsIT(String baseImage, String distribution) throws IOException, URISyntaxException {
        switch (distribution) {
            case "httpserver":
                testSetup = new HttpServerTestSetup(baseImage, "config-httpserver");
                break;
            case "agent":
                testSetup = new AgentTestSetup(baseImage, "jmx_prometheus_javaagent", "config-agent");
                break;
            case "agent_java6":
                testSetup = new AgentTestSetup(baseImage, "jmx_prometheus_javaagent_java6", "config-agent");
                break;
            default:
                throw new IllegalStateException(distribution + ": unknown distribution");
        }
    }

    @After
    public void tearDown() throws Exception {
        testSetup.close();
    }

    @Test
    public void testJvmMetric() throws Exception {
        String metricName = "java_lang_Memory_NonHeapMemoryUsage_committed";
        String metric = testSetup.scrape(10 * 1000).stream()
                .filter(line -> line.startsWith(metricName))
                .findAny()
                .orElseThrow(() -> new AssertionError("Metric " + metricName + " not found."));
        double value = Double.parseDouble(metric.split(" ")[1]);
        Assert.assertTrue(metricName + " should be > 0", value > 0);
    }

    @Test
    public void testTabularMetric() throws Exception {
        String[] expectedMetrics = new String[]{
                "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size{source=\"/dev/sda1\"} 7.516192768E9",
                "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size{source=\"/dev/sda2\"} 1.5032385536E10",
                "io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_size{source=\"/dev/sda1\"} 2.5769803776E10",
                "io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_size{source=\"/dev/sda2\"} 1.073741824E11"
        };
        List<String> metrics = testSetup.scrape(10 * 1000);
        for (String expectedMetric : expectedMetrics) {
            metrics.stream()
                    .filter(line -> line.startsWith(expectedMetric))
                    .findAny()
                    .orElseThrow(() -> new AssertionError("Metric " + expectedMetric + " not found."));
        }
    }
}
