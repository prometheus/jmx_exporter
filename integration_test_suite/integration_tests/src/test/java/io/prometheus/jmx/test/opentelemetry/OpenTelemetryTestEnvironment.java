package io.prometheus.jmx.test.opentelemetry;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import java.time.Duration;
import org.antublue.test.engine.api.Argument;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

public class OpenTelemetryTestEnvironment implements Argument<OpenTelemetryTestEnvironment> {

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;
    private static final String BASE_URL = "http://localhost";

    private final String prometheusDockerImageName;
    private final String javaDockerImageName;
    private final JmxExporterMode jmxExporterMode;

    private Class<?> testClass;
    private Network network;
    private GenericContainer<?> prometheusContainer;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;
    private HttpClient httpClient;

    public OpenTelemetryTestEnvironment(
            String prometheusDockerImageName,
            String javaDockerImageName,
            JmxExporterMode jmxExporterMode) {
        this.prometheusDockerImageName = prometheusDockerImageName;
        this.javaDockerImageName = javaDockerImageName;
        this.jmxExporterMode = jmxExporterMode;
    }

    @Override
    public String getName() {
        return prometheusDockerImageName + " / " + javaDockerImageName + " / " + jmxExporterMode;
    }

    @Override
    public OpenTelemetryTestEnvironment getPayload() {
        return this;
    }

    /**
     * Method to get the Prometheus Docker image name
     *
     * @return the Prometheus Docker image name
     */
    public String getPrometheusDockerImageName() {
        return prometheusDockerImageName;
    }

    /**
     * Method to get the Java Docker image name
     *
     * @return the Java Docker image name
     */
    public String getJavaDockerImageName() {
        return javaDockerImageName;
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
                    prometheusContainer = createPrometheusContainer();
                    javaAgentApplicationContainer = createJavaAgentApplicationContainer();

                    prometheusContainer.start();
                    javaAgentApplicationContainer.start();

                    break;
                }
            case Standalone:
                {
                    prometheusContainer = createPrometheusContainer();
                    standaloneApplicationContainer = createStandaloneApplicationContainer();
                    standaloneExporterContainer = createStandaloneExporterContainer();

                    prometheusContainer.start();
                    standaloneApplicationContainer.start();
                    standaloneExporterContainer.start();

                    break;
                }
        }

        httpClient = createPrometheusHttpClient(prometheusContainer, BASE_URL, 9090);
    }

    /**
     * Method to get an HttpClient for the test environment
     *
     * @return an HttpClient
     */
    public HttpClient getPrometheusHttpClient() {
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

        if (prometheusContainer != null) {
            prometheusContainer.close();
            prometheusContainer = null;
        }
    }

    /**
     * Method to create a Prometheus container
     *
     * @return the return value
     */
    private GenericContainer<?> createPrometheusContainer() {
        return new GenericContainer<>(prometheusDockerImageName)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/")
                                + "/"
                                + jmxExporterMode
                                + "/prometheus.yml",
                        "/etc/prometheus/prometheus.yml",
                        BindMode.READ_ONLY)
                .withWorkingDirectory("/prometheus")
                .withCommand(
                        "--config.file=/etc/prometheus/prometheus.yml",
                        "--storage.tsdb.path=/prometheus",
                        "--web.console.libraries=/usr/share/prometheus/console_libraries",
                        "--web.console.templates=/usr/share/prometheus/consoles",
                        "--enable-feature=otlp-write-receiver")
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
                .withExposedPorts(9090)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("prometheus")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000));
    }

    /**
     * Method to create an application container
     *
     * @return the return value
     */
    private GenericContainer<?> createJavaAgentApplicationContainer() {
        return new GenericContainer<>(javaDockerImageName)
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
        return new GenericContainer<>(javaDockerImageName)
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
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an exporter container
     *
     * @return the return value
     */
    private GenericContainer<?> createStandaloneExporterContainer() {
        return new GenericContainer<>(javaDockerImageName)
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
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an HttpClient
     *
     * @param genericContainer genericContainer
     * @param baseUrl baseUrl
     * @return the return value
     */
    private static HttpClient createPrometheusHttpClient(
            GenericContainer<?> genericContainer, String baseUrl, int mappedPort) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(mappedPort));
    }
}
