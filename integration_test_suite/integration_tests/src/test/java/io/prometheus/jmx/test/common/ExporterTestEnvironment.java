package io.prometheus.jmx.test.common;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import java.time.Duration;
import org.antublue.verifyica.api.Argument;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

/** Class to implement OpenTelemetryTestEnvironment */
public class ExporterTestEnvironment implements Argument<ExporterTestEnvironment> {

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;
    private static final String BASE_URL = "http://localhost";

    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private Class<?> testClass;
    private String baseUrl;
    private Network network;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;
    private HttpClient httpClient;

    /**
     * Constructor
     *
     * @param javaDockerImage javaDockerImage
     * @param jmxExporterMode jmxExporterMode
     */
    public ExporterTestEnvironment(String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.javaDockerImage = javaDockerImage;
        this.jmxExporterMode = jmxExporterMode;
        this.baseUrl = BASE_URL;
    }

    @Override
    public String getName() {
        return javaDockerImage + " / " + jmxExporterMode;
    }

    @Override
    public ExporterTestEnvironment getPayload() {
        return this;
    }

    /**
     * Method to set the base URL
     *
     * @param baseUrl baseUrl
     * @return the ExporterTestEnvironment
     */
    public ExporterTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Method to get the Java Docker image name
     *
     * @return the Java Docker image name
     */
    public String getJavaDockerImage() {
        return javaDockerImage;
    }

    /**
     * Method to get the JMX Exporter mode
     *
     * @return the JMX Exporter mode
     */
    public JmxExporterMode getJmxExporterMode() {
        return jmxExporterMode;
    }

    /**
     * Method to initialize the test environment
     *
     * @param network network
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        switch (jmxExporterMode) {
            case JavaAgent:
                {
                    javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                    javaAgentApplicationContainer.start();

                    httpClient = createHttpClient(javaAgentApplicationContainer, baseUrl, 8888);

                    break;
                }
            case Standalone:
                {
                    standaloneApplicationContainer = createStandaloneApplicationContainer();
                    standaloneApplicationContainer.start();

                    standaloneExporterContainer = createStandaloneExporterContainer();
                    standaloneExporterContainer.start();

                    httpClient = createHttpClient(standaloneExporterContainer, baseUrl, 8888);

                    break;
                }
        }
    }

    /**
     * Method to get an HttpClient for the test environment
     *
     * @return an HttpClient
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /** Method to destroy the test environment */
    public void destroy() {
        if (javaAgentApplicationContainer != null) {
            javaAgentApplicationContainer.close();
            javaAgentApplicationContainer = null;
        }

        if (standaloneExporterContainer != null) {
            standaloneExporterContainer.close();
            standaloneExporterContainer = null;
        }

        if (standaloneApplicationContainer != null) {
            standaloneApplicationContainer.close();
            standaloneApplicationContainer = null;
        }
    }

    /**
     * Method to create an application container
     *
     * @return the return value
     */
    private GenericContainer<?> createJavaAgentApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/JavaAgent",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(8888)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("application")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an application container
     *
     * @return the return value
     */
    private GenericContainer<?> createStandaloneApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(9999)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("application")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp")
                .waitingFor(Wait.forLogMessage(".*Running.*", 1));
    }

    /**
     * Method to create an exporter container
     *
     * @return the return value
     */
    private GenericContainer<?> createStandaloneExporterContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh exporter.sh")
                .withExposedPorts(8888)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("exporter")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp")
                .waitingFor(Wait.forLogMessage(".*Running.*", 1));
    }

    /**
     * Method to create a Prometheus HttpClient
     *
     * @param genericContainer genericContainer
     * @param baseUrl baseUrl
     * @return the return value
     */
    private static HttpClient createHttpClient(
            GenericContainer<?> genericContainer, String baseUrl, int mappedPort) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(mappedPort));
    }
}
