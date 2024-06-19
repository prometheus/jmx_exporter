package io.prometheus.jmx.test.opentelemetry;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.DockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

public class OpenTelemetryTest {

    private static final String PROMETHEUS_DOCKER_IMAGE_NAME = "prom/prometheus:latest";

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;

    private Network network;
    private GenericContainer<?> prometheusContainer;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;

    @TestEngine.Argument public OpenTelemetryTestArguments openTelemetryTestArguments;

    /**
     * Method to get the list of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    public static Stream<OpenTelemetryTestArguments> arguments() {
        List<OpenTelemetryTestArguments> openTelemetryTestArguments = new ArrayList<>();

        List<String> prometheusDockerImageNames = new ArrayList<>();
        prometheusDockerImageNames.add(PROMETHEUS_DOCKER_IMAGE_NAME);

        prometheusDockerImageNames.forEach(
                prometheusDockerImage ->
                        DockerImageNames.names()
                                .forEach(
                                        javaDockerImageName -> {
                                            for (JmxExporterMode jmxExporterMode :
                                                    JmxExporterMode.values()) {
                                                openTelemetryTestArguments.add(
                                                        OpenTelemetryTestArguments.of(
                                                                prometheusDockerImage
                                                                        + " / "
                                                                        + javaDockerImageName
                                                                        + " / "
                                                                        + jmxExporterMode,
                                                                PROMETHEUS_DOCKER_IMAGE_NAME,
                                                                javaDockerImageName,
                                                                jmxExporterMode));
                                            }
                                        }));

        return openTelemetryTestArguments.stream();
    }

    @TestEngine.Prepare
    public void prepare() {
        // Get the Network and get the id to force the network creation
        network = Network.newNetwork();
        network.getId();
    }

    @TestEngine.BeforeAll
    public void beforeAll() throws Throwable {
        switch (openTelemetryTestArguments.getJmxExporterMode()) {
            case JavaAgent:
                {
                    prometheusContainer =
                            createPrometheusContainer(
                                    openTelemetryTestArguments.getJmxExporterMode(),
                                    network,
                                    openTelemetryTestArguments.getPrometheusDockerImageName(),
                                    getClass().getName());

                    javaAgentApplicationContainer =
                            createJavaAgentApplicationContainer(
                                    network,
                                    openTelemetryTestArguments.getJavaDockerImageName(),
                                    getClass().getName());

                    prometheusContainer.start();
                    javaAgentApplicationContainer.start();
                    break;
                }
            case Standalone:
                {
                    prometheusContainer =
                            createPrometheusContainer(
                                    openTelemetryTestArguments.getJmxExporterMode(),
                                    network,
                                    openTelemetryTestArguments.getPrometheusDockerImageName(),
                                    getClass().getName());

                    standaloneApplicationContainer =
                            createStandaloneApplicationContainer(
                                    network,
                                    openTelemetryTestArguments.getJavaDockerImageName(),
                                    getClass().getName());

                    standaloneExporterContainer =
                            createStandaloneExporterContainer(
                                    network,
                                    openTelemetryTestArguments.getJavaDockerImageName(),
                                    getClass().getName());

                    prometheusContainer.start();
                    standaloneApplicationContainer.start();
                    standaloneExporterContainer.start();
                    break;
                }
        }
    }

    @TestEngine.Test
    public void testOpenTelemetry() throws Throwable {
        System.out.println("testOpenTelemetry()");
    }

    @TestEngine.AfterAll
    public void afterAll() {
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

    @TestEngine.Conclude
    public void conclude() {
        if (network != null) {
            network.close();
        }
    }

    /**
     * Method to create a Prometheus container
     *
     * @param network network
     * @param prometheusDockerImageName prometheusDockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createPrometheusContainer(
            JmxExporterMode jmxExporterMode,
            Network network,
            String prometheusDockerImageName,
            String testName) {
        return new GenericContainer<>(prometheusDockerImageName)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/" + jmxExporterMode + "/prometheus.yml",
                        "/etc/prometheus/prometheus.yml",
                        BindMode.READ_ONLY)
                /*
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(PROMETHEUS_MEMORY_BYTES)
                                        .withMemorySwap(PROMETHEUS_MEMORY_SWAP_BYTES))
                 */
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withExposedPorts(7777)
                .withNetwork(network)
                .withNetworkAliases("prometheus")
                .withStartupTimeout(Duration.ofMillis(30000));
    }

    /**
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createStandaloneApplicationContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
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
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createStandaloneExporterContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
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
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createJavaAgentApplicationContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/JavaAgent", "/temp", BindMode.READ_ONLY)
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
}
