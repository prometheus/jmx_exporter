package io.prometheus.jmx;

import com.github.dockerjava.api.model.Ulimit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple test of the jmx_prometheus_httpserver getting metrics from the JmxExampleApplication.
 */
@RunWith(Parameterized.class)
public class HttpServerIT {

    private final String httpServerModule;
    private final Volume volume;
    private final GenericContainer<?> javaContainer;
    private final Scraper scraper;

    @Parameterized.Parameters(name="{0}")
    public static String[][] images() {
        return new String[][] {

            // HotSpot
            { "openjdk:8-jre", "jmx_prometheus_httpserver" },
            { "openjdk:8-jre","jmx_prometheus_httpserver_java6" },

            { "openjdk:11-jre", "jmx_prometheus_httpserver_java6" },
            { "openjdk:11-jre", "jmx_prometheus_httpserver" },

            { "openjdk:17-oracle", "jmx_prometheus_httpserver_java6" },
            { "openjdk:17-oracle", "jmx_prometheus_httpserver" },

            { "ticketfly/java:6",  "jmx_prometheus_httpserver_java6" },

            { "openjdk:7", "jmx_prometheus_httpserver_java6" },
            { "openjdk:7", "jmx_prometheus_httpserver" },

            // OpenJ9
            //{ "ibmjava:8-jre", "jmx_prometheus_httpserver_java6" },
            //{ "ibmjava:8-jre", "jmx_prometheus_httpserver" },

            { "ibmjava:11", "jmx_prometheus_httpserver_java6" },
            { "ibmjava:11", "jmx_prometheus_httpserver" },

            { "adoptopenjdk/openjdk11-openj9", "jmx_prometheus_httpserver_java6" },
            { "adoptopenjdk/openjdk11-openj9", "jmx_prometheus_httpserver" },
        };
    }

    public HttpServerIT(String baseImage, String httpServerModule) throws IOException, URISyntaxException {
        this.httpServerModule = httpServerModule;
        volume = Volume.create("http-server-integration-test-");
        volume.copyHttpServer(httpServerModule);
        volume.copyConfigYaml("config-httpserver.yml");
        volume.copyExampleApplication();
        String runExampleConfig = "java " +
                "-Dcom.sun.management.jmxremote.port=9999 " +
                "-Dcom.sun.management.jmxremote.authenticate=false " +
                "-Dcom.sun.management.jmxremote.ssl=false " +
                "-jar jmx_example_application.jar";
        String runHttpServer = "java -jar jmx_prometheus_httpserver.jar 9000 config.yaml";
        javaContainer = new GenericContainer<>(baseImage)
                .withFileSystemBind(volume.getHostPath(), "/app", BindMode.READ_ONLY)
                .withWorkingDirectory("/app")
                // The firefly/java:6 container needs an increased number of file descriptors, so set the nofile ulimit here.
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                .withExposedPorts(9000)
                .withCommand("/bin/bash", "-c", runExampleConfig + " & " + runHttpServer)
                .waitingFor(Wait.forLogMessage(".*registered.*", 1))
                .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
        javaContainer.start();
        scraper = new Scraper(javaContainer.getHost(), javaContainer.getMappedPort(9000));
    }

    @After
    public void tearDown() throws IOException {
        javaContainer.stop();
        volume.close();
    }

    @Test
    public void testExampleMetrics() throws Exception {
        for (String metric : new String[]{
                "java_lang_Memory_NonHeapMemoryUsage_committed",
                "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size{source=\"/dev/sda1\"} 7.516192768E9",
        }) {
            scraper.scrape(10 * 1000).stream()
                    .filter(line -> line.startsWith(metric))
                    .findAny()
                    .orElseThrow(() -> new AssertionError("Metric " + metric + " not found."));
        }
    }

    @Test
    public void testBuildInfoMetricName() {
        AtomicReference expectedName = new AtomicReference("\"jmx_prometheus_httpserver\"");
        if (httpServerModule.endsWith("_java6")) {
            expectedName.set("\"jmx_prometheus_httpserver_java6\"");
        }

        List<String> metrics = scraper.scrape(10 * 1000);
        metrics.stream()
                .filter(line -> line.startsWith("jmx_exporter_build_info"))
                .findAny()
                .ifPresent(line -> {
                    Assert.assertTrue(line.contains((String) expectedName.get()));
                });
    }

    @Test
    public void testBuildVersionMetricNotUnknown() {
        // No easy way to test the version, so make sure it's not "unknown"
        List<String> metrics = scraper.scrape(10 * 1000);
        metrics.stream()
                .filter(line -> line.startsWith("jmx_exporter_build_info"))
                .findAny()
                .ifPresent(line -> {
                    Assert.assertTrue(!line.contains("\"unknown\""));
                });
    }
}
