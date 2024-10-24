package io.prometheus.jmx.test.opentelemetry;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.verifyica.api.Argument;

/** Class to implement OpenTelemetryTestEnvironment */
public class OpenTelemetryTestEnvironment implements Argument<OpenTelemetryTestEnvironment> {

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;
    private static final String BASE_URL = "http://localhost";

    private final String prometheusDockerImage;
    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private Class<?> testClass;
    private Network network;
    private GenericContainer<?> prometheusContainer;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;
    private HttpClient httpClient;

    /**
     * Constructor
     *
     * @param prometheusDockerImage prometheusDockerImage
     * @param javaDockerImage javaDockerImage
     * @param jmxExporterMode jmxExporterMode
     */
    public OpenTelemetryTestEnvironment(
            String prometheusDockerImage, String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.prometheusDockerImage = prometheusDockerImage;
        this.javaDockerImage = javaDockerImage;
        this.jmxExporterMode = jmxExporterMode;
    }

    @Override
    public String getName() {
        return prometheusDockerImage + " / " + javaDockerImage + " / " + jmxExporterMode;
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
    public String getPrometheusDockerImage() {
        return prometheusDockerImage;
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
                    prometheusContainer = createPrometheusContainer();
                    prometheusContainer.start();

                    javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                    javaAgentApplicationContainer.start();

                    break;
                }
            case Standalone:
                {
                    prometheusContainer = createPrometheusContainer();
                    prometheusContainer.start();

                    standaloneApplicationContainer = createStandaloneApplicationContainer();
                    standaloneApplicationContainer.start();

                    standaloneExporterContainer = createStandaloneExporterContainer();
                    standaloneExporterContainer.start();

                    break;
                }
        }

        if (standaloneApplicationContainer != null && !standaloneApplicationContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
        }

        if (standaloneApplicationContainer != null && !standaloneApplicationContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
        }

        if (prometheusContainer != null && !prometheusContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
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
        List<String> commands = new ArrayList<>();

        commands.add("--config.file=/etc/prometheus/prometheus.yaml");
        commands.add("--storage.tsdb.path=/prometheus");
        commands.add("--web.console.libraries=/usr/share/prometheus/console_libraries");
        commands.add("--web.console.templates=/usr/share/prometheus/consoles");
        commands.add("--enable-feature=otlp-write-receiver");

        String webYml =
                "/" + testClass.getName().replace(".", "/") + "/" + jmxExporterMode + "/web.yaml";

        boolean hasWebYaml = hasResource(webYml);

        if (hasWebYaml) {
            commands.add("--web.config.file=/etc/prometheus/web.yaml");
        }

        GenericContainer<?> genericContainer =
                new GenericContainer<>(prometheusDockerImage)
                        .withClasspathResourceMapping(
                                testClass.getName().replace(".", "/")
                                        + "/"
                                        + jmxExporterMode
                                        + "/prometheus.yaml",
                                "/etc/prometheus/prometheus.yaml",
                                BindMode.READ_ONLY)
                        .withWorkingDirectory("/prometheus")
                        .withCommand(commands.toArray(new String[0]))
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
                                    String string =
                                            outputFrame.getUtf8StringWithoutLineEnding().trim();
                                    if (!string.isBlank()) {
                                        System.out.println(string);
                                    }
                                })
                        .withNetwork(network)
                        .withNetworkAliases("prometheus")
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withStartupTimeout(Duration.ofMillis(30000))
                        .waitingFor(
                                Wait.forLogMessage(
                                        ".*Server is ready to receive web requests.*", 1));

        if (hasWebYaml) {
            genericContainer.withClasspathResourceMapping(
                    webYml, "/etc/prometheus/web.yaml", BindMode.READ_ONLY);
        }

        return genericContainer;
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
    private static HttpClient createPrometheusHttpClient(
            GenericContainer<?> genericContainer, String baseUrl, int mappedPort) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(mappedPort));
    }

    /**
     * Method to determine if a resource exists
     *
     * @param resource resource
     * @return true if the resource exists, else false
     */
    private static boolean hasResource(String resource) {
        boolean hasResource = false;

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        InputStream inputStream = ResourceSupport.class.getResourceAsStream(resource);
        if (inputStream != null) {
            try (BufferedReader bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }

                    hasResource = true;
                    break;
                }
            } catch (Throwable t) {
                // DO NOTHING
            }
        }

        return hasResource;
    }
}