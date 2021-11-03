package io.prometheus.jmx;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Run the jmx_example_application with the agent attached in a Docker container.
 */
public class AgentTestSetup implements TestSetup {

    private final String configFilePrefix;
    private final Volume volume;
    private final GenericContainer<?> javaContainer;
    private final Scraper scraper;

    public AgentTestSetup(String baseImage, String agentModule, String configFilePrefix) throws IOException, URISyntaxException {
        this.configFilePrefix = configFilePrefix;
        volume = Volume.create("agent-integration-test-");
        volume.copyAgentJar(agentModule);
        volume.copyConfigYaml(makeConfigFileName(".yml"));
        volume.copyExampleApplication();
        String cmd = "java -javaagent:agent.jar=9000:config.yaml -jar jmx_example_application.jar";
        javaContainer = new GenericContainer<>(baseImage)
                .withFileSystemBind(volume.getHostPath(), "/app", BindMode.READ_ONLY)
                .withWorkingDirectory("/app")
                .withExposedPorts(9000)
                .withCommand(cmd)
                .waitingFor(Wait.forLogMessage(".*registered.*", 1))
                .withLogConsumer(System.out::print);
        javaContainer.start();
        scraper = new Scraper(javaContainer.getHost(), javaContainer.getMappedPort(9000));
    }

    private String makeConfigFileName(String suffix) {
        return configFilePrefix + suffix;
    }

    @Override
    public List<String> scrape(long timeoutMillis) {
        return scraper.scrape(timeoutMillis);
    }

    @Override
    public void copyConfig(String suffix) throws IOException, URISyntaxException {
        volume.copyConfigYaml(makeConfigFileName(suffix));
    }

    @Override
    public void close() throws Exception {
        javaContainer.stop();
        volume.close();
    }

}
