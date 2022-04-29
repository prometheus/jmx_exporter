package io.prometheus.jmx;

import org.junit.After;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;

public class HttpServerSslIT {
    private final Volume exampleApplicationVolume;
    private final Volume jmxExporterVolume;
    private final GenericContainer<?> exampleApplicationContainer;
    private final GenericContainer<?> jmxExporterContainer;
    private final Scraper scraper;

    // This is a first example. It's mostly copy-and-paste from HttpServerIT.
    //
    // TODO: Add tests for the following scenarios:
    //
    // General
    // * configuration via config.yaml vs. configuration via command line parameters
    // * all tests with and without client auth
    // * test passwordfile and accessfile
    //
    // JMX exporter HTTP Server:
    // * scrape JMX using SSL but export using plain HTTP
    // * scrape JMX in plain text but export metrics via HTTPS
    // * use SSL for both, but different certificates for scraping and exporting
    //
    // Java agent
    // * export metrics with SSL using the default keystore from the application
    // * export metrics with SSL but use a dedicated keystore for the agent
    // * attach the agent to an application that uses SSL and make sure the agent's SSL does not interfere with the application's SSL

    public HttpServerSslIT() throws Exception {
        String baseImage = "openjdk:11-jre";
        exampleApplicationVolume = Volume.create("http-server-with-ssl-integration-test-example-app-");
        exampleApplicationVolume.copyExampleApplication();
        exampleApplicationVolume.copy("keystore");
        jmxExporterVolume = Volume.create("http-server-with-ssl-integration-test-jmx_exporter-");
        jmxExporterVolume.copyHttpServer();
        jmxExporterVolume.copy("config-httpserver.yml", "config.yaml");
        jmxExporterVolume.copy("truststore");
        String runExampleApplication = "java " +
                // "-Djavax.net.debug=all " + // enable this for SSL debug messages
                "-Dcom.sun.management.jmxremote.port=7092 " +
                "-Dcom.sun.management.jmxremote.authenticate=false " +
                "-Dcom.sun.management.jmxremote.ssl=true " +
                "-Dcom.sun.management.jmxremote.registry.ssl=true " +
                "-Djavax.net.ssl.keyStorePassword=password " +
                "-Djavax.net.ssl.keyStore=keystore " +
                "-jar jmx_example_application.jar";
        String runJmxExporter = "java " +
                // "-Djavax.net.debug=all " + // enable this for SSL debug messages
                "-Djavax.net.ssl.trustStore=truststore " +
                "-Djavax.net.ssl.trustStorePassword=password " +
                "-Dcom.sun.management.jmxremote.authenticate=false " +
                "-jar jmx_prometheus_httpserver.jar 9000 config.yaml";
        // Print the volumes and commands so that the test can be reproduced manually without Docker.
        // Comment out the volume.close() calls in tearDown() to keep the volumes after the test for manual debugging.
        System.out.println("*** example application ***");
        System.out.println("Volume: " + exampleApplicationVolume.getHostPath());
        System.out.println("Cmd: " + runExampleApplication);
        System.out.println("*** jmx_prometheus_httpserver ***");
        System.out.println("Volume: " + jmxExporterVolume.getHostPath());
        System.out.println("Cmd: " + runJmxExporter);
        Network network = Network.newNetwork();
        exampleApplicationContainer = new GenericContainer<>(baseImage)
                .withFileSystemBind(exampleApplicationVolume.getHostPath(), "/app", BindMode.READ_ONLY)
                .withWorkingDirectory("/app")
                .withNetworkAliases("app")
                .withNetwork(network)
                .withExposedPorts(7092)
                .withCommand("/bin/bash", "-c", runExampleApplication)
                .waitingFor(Wait.forLogMessage(".*registered.*", 1))
                .withLogConsumer(f -> System.out.print(f.getUtf8String()));
        exampleApplicationContainer.start();
        jmxExporterVolume.replace("config.yaml", "localhost:7092", "app:7092");// exampleApplicationContainer.get.getHost() + ":" + exampleApplicationContainer.getMappedPort(7092));
        jmxExporterContainer = new GenericContainer<>(baseImage)
                .withFileSystemBind(jmxExporterVolume.getHostPath(), "/app", BindMode.READ_ONLY)
                .withWorkingDirectory("/app")
                .withNetwork(network)
                .withExposedPorts(9000)
                .withCommand("/bin/bash", "-c", runJmxExporter)
                //.waitingFor(Wait.forLogMessage(".*registered.*", 1))
                .withLogConsumer(f -> System.out.print(f.getUtf8String()));
        jmxExporterContainer.start();
        Thread.sleep(2000);
        scraper = new Scraper(jmxExporterContainer.getHost(), jmxExporterContainer.getMappedPort(9000));
    }

    @After
    public void tearDown() throws IOException {
        exampleApplicationContainer.stop();
        jmxExporterContainer.stop();
        exampleApplicationVolume.close();
        jmxExporterVolume.close();
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
}
