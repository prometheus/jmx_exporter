package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/** Class to implement AbstractOpenTelemetryTest */
public abstract class AbstractOpenTelemetryTest {

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

    @TestEngine.Prepare
    public void prepare() {
        // Get the Network and get the id to force the network creation
        network = Network.newNetwork();
        network.getId();
    }

    @TestEngine.BeforeAll
    public void beforeAll() {
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

    /** Method to test that Prometheus is up */
    @TestEngine.Test
    @TestEngine.Order(order = 0)
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

    /** Method to test that metrics exist in Prometheus */
    @TestEngine.Test
    public void testPrometheusMetrics() {
        AtomicInteger pollCount = new AtomicInteger(10);
        AtomicBoolean wasSuccessful = new AtomicBoolean();

        do {
            sendPrometheusQuery("jmx_exporter_build_info")
                    .accept(
                            httpResponse -> {
                                assertThat(httpResponse).isNotNull();
                                assertThat(httpResponse.getStatusCode()).isEqualTo(200);
                                assertThat(httpResponse.body()).isNotNull();
                                assertThat(httpResponse.body().string()).isNotNull();

                                Map<Object, Object> map =
                                        new Yaml().load(httpResponse.body().string());
                                String status = (String) map.get("status");

                                if ("success".equals(status) && map.containsKey("$.data.result")) {
                                    // TODO real logic
                                    pollCount.set(0);
                                    wasSuccessful.set(true);
                                }

                                pollCount.decrementAndGet();
                            });

            if (wasSuccessful.get()) {
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // DO NOTHING
            }
        } while (pollCount.get() > 0);

        assertThat(wasSuccessful).isTrue();
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
    protected HttpResponse sendPrometheusQuery(String query) {
        return sendRequest(
                "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Method to send a Http GET request
     *
     * @param path path
     * @return an HttpResponse
     */
    protected HttpResponse sendRequest(String path) {
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
