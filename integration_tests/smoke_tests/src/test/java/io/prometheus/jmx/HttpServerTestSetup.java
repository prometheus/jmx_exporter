package io.prometheus.jmx;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Run the jmx_example_application and the jmx_prometheus_httpserver in a Docker container.
 */
public class HttpServerTestSetup implements TestSetup {

    private final String configFilePrefix;
    private final Volume volume;
    private final GenericContainer<?> javaContainer;
    private final Scraper  scraper;

    public HttpServerTestSetup(String baseImage, String configFilePrefix) throws IOException, URISyntaxException {
        this.configFilePrefix = configFilePrefix;
        volume = Volume.create("http-server-integration-test-");
        volume.copyHttpServer();
        volume.copyConfigYaml(makeConfigFileName(".yml"));
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
                .withExposedPorts(9000)
                .withCommand("/bin/bash", "-c", runExampleConfig + " & " + runHttpServer)
                .waitingFor(Wait.forLogMessage(".*registered.*", 1))
                .withLogConsumer(System.out::print);
        javaContainer.start();
        scraper = new Scraper(javaContainer.getHost(), javaContainer.getMappedPort(9000));
    }

    private String makeConfigFileName(String suffix) {
        return configFilePrefix + suffix;
    }

    @Override
    public void close() throws Exception {
        javaContainer.stop();
        volume.close();
    }

    @Override
    public List<String> scrape(long timeoutMillis) {
        return scraper.scrape(timeoutMillis);
    }

    @Override
    public void copyConfig(String suffix) throws IOException, URISyntaxException {
        volume.copyConfigYaml(makeConfigFileName(suffix));
    }
}
