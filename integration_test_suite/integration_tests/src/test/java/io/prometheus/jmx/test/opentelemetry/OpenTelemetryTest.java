package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.DockerImageNames;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

public class OpenTelemetryTest {

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;

    private static final String BASE_URL = "http://localhost";

    private Network network;
    private GenericContainer<?> prometheusContainer;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;
    private HttpClient httpClient;

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
        prometheusDockerImageNames.add("prom/prometheus:v2.46.0");
        prometheusDockerImageNames.add("prom/prometheus:v2.47.2");
        prometheusDockerImageNames.add("prom/prometheus:v2.48.1");
        prometheusDockerImageNames.add("prom/prometheus:v2.49.1");
        prometheusDockerImageNames.add("prom/prometheus:v2.50.1");
        prometheusDockerImageNames.add("prom/prometheus:v2.51.2");
        prometheusDockerImageNames.add("prom/prometheus:v2.52.0");
        prometheusDockerImageNames.add("prom/prometheus:v2.53.0");

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
                                                                prometheusDockerImage,
                                                                javaDockerImageName,
                                                                jmxExporterMode));
                                            }
                                        }));

        System.out.println("test argument count [" + openTelemetryTestArguments.size() + "]");

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

        httpClient = createPrometheusHttpClient(prometheusContainer, BASE_URL, 9090);
    }

    @TestEngine.Test
    public void testPrometheusIsUp() {
        sendPrometheusQuery("up")
                .accept(
                        httpResponse -> {
                            assertThat(httpResponse).isNotNull();
                            assertThat(httpResponse.getStatusCode()).isEqualTo(200);
                            assertThat(httpResponse.body()).isNotNull();
                            assertThat(httpResponse.body().string()).isNotNull();

                            Map<Object, Object> map = new Yaml().load(httpResponse.body().string());
                            String status = (String) map.get("status");
                            assertThat(status).isEqualTo("success");
                        });
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
     * Method to send a Prometheus query
     *
     * @param query query
     * @return an HttpResponse
     */
    private HttpResponse sendPrometheusQuery(String query) {
        return sendRequest(
                "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Method to send a Http GET request
     *
     * @param path path
     * @return an HttpResponse
     */
    private HttpResponse sendRequest(String path) {
        return httpClient.send(new HttpRequest(path));
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
}
