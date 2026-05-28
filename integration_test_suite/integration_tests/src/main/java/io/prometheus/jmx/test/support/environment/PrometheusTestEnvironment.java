/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support.environment;

import static java.lang.String.format;

import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.test.support.NetworkFactory;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.paramixel.api.support.Retry;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test environment for a Prometheus server instance, managing a Docker container
 * that scrapes the JMX exporter endpoints for integration testing.
 *
 * <p>The environment creates its own Docker network when initialized without one and
 * tears down the container and network on {@link #close()}.
 */
public class PrometheusTestEnvironment implements AutoCloseable {

    private static final String BASE_URL = "http://localhost";

    private final String id;
    private final Class<?> testClass;
    private final String prometheusDockerImage;

    private String baseUrl;
    private Network network;
    private boolean ownsNetwork;
    private GenericContainer<?> prometheusContainer;

    /**
     * Creates a Prometheus test environment for the specified test class and Docker image.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @param prometheusDockerImage the Prometheus Docker image name for the container
     * @throws NullPointerException if {@code testClass} or {@code prometheusDockerImage} is {@code null}
     */
    public PrometheusTestEnvironment(Class<?> testClass, String prometheusDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.testClass = Objects.requireNonNull(testClass);
        this.prometheusDockerImage = Objects.requireNonNull(prometheusDockerImage);
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns a human-readable name identifying this environment and its Prometheus Docker image.
     *
     * @return the display name of this environment
     */
    public String name() {
        return prometheusDockerImage;
    }

    /**
     * Returns the unique identifier for this environment instance.
     *
     * @return the unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the base URL used to construct Prometheus request URLs, returning this instance for chaining.
     *
     * @param baseUrl the base URL (e.g. {@code "http://localhost"}); must not be {@code null}
     * @return this environment instance
     */
    public PrometheusTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Returns the Prometheus Docker image name used by the container.
     *
     * @return the Prometheus Docker image name
     */
    public String getPrometheusDockerImage() {
        return prometheusDockerImage;
    }

    /**
     * Initializes this environment by creating a new Docker network and starting
     * the Prometheus container. The network is owned and will be closed on {@link #close()}.
     */
    public void initialize() {
        initialize(NetworkFactory.createNetwork(), true);
    }

    /**
     * Initializes this environment using an existing Docker network. The network is
     * <em>not</em> owned and will not be closed on {@link #close()}.
     *
     * @param network the shared Docker network; must not be {@code null}
     */
    public void initialize(Network network) {
        initialize(network, false);
    }

    private void initialize(Network network, boolean ownsNetwork) {
        this.network = Objects.requireNonNull(network);
        this.ownsNetwork = ownsNetwork;

        prometheusContainer = createPrometheusContainer();
        prometheusContainer.start();

        if (!prometheusContainer.isRunning()) {
            throw new IllegalStateException("Prometheus container is not running");
        }
    }

    /**
     * Returns whether the Prometheus container is currently running.
     *
     * @return {@code true} if the container is running; {@code false} otherwise
     */
    public boolean isRunning() {
        return prometheusContainer != null && prometheusContainer.isRunning();
    }

    /**
     * Constructs a Prometheus URL for the specified path, prepending the base URL and mapped port.
     *
     * @param path the URL path; a leading slash is optional
     * @return the fully constructed Prometheus URL
     */
    public String getPrometheusUrl(String path) {
        return !path.startsWith("/") ? getPrometheusBaseUrl() + "/" + path : getPrometheusBaseUrl() + path;
    }

    private String getPrometheusBaseUrl() {
        return baseUrl + ":" + prometheusContainer.getMappedPort(9090);
    }

    /**
     * Waits for Prometheus to become ready by polling the {@code /-/ready} endpoint without authentication.
     *
     * @throws EnvironmentException if Prometheus does not become ready within the retry policy
     */
    public void waitForReady() {
        waitForReady(null, null);
    }

    /**
     * Waits for Prometheus to become ready by polling the {@code /-/ready} endpoint
     * with basic authentication.
     *
     * @param username the basic authentication username; may be {@code null} to skip authentication
     * @param password the basic authentication password; may be {@code null} to skip authentication
     * @throws EnvironmentException if Prometheus does not become ready within the retry policy
     */
    public void waitForReady(String username, String password) {
        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(5)))
                .retryOn(t -> t instanceof Exception)
                .run(() -> {
                    HttpRequest.Builder httpRequestBuilder =
                            HttpRequest.builder().url(getPrometheusBaseUrl() + "/-/ready");

                    if (username != null && password != null) {
                        httpRequestBuilder.basicAuthentication(username, password);
                    }

                    HttpResponse httpResponse = HttpClient.sendRequest(httpRequestBuilder.build());

                    if (httpResponse.statusCode() == 200) {
                        return;
                    }

                    throw new EnvironmentException("Prometheus not ready, status: " + httpResponse.statusCode());
                });

        if (!result.isSuccessful()) {
            throw new EnvironmentException(
                    format("Prometheus [%s] not ready after retry", prometheusContainer.getDockerImageName()));
        }
    }

    @Override
    public void close() {
        GenericContainer<?> container = prometheusContainer;
        stopQuietly();
        ContainerSupport.waitForShutdown(container);
        closeNetworkIfOwned();
    }

    private void closeNetworkIfOwned() {
        if (ownsNetwork && network != null) {
            try {
                ContainerSupport.waitForShutdown(network);
            } finally {
                network = null;
                ownsNetwork = false;
            }
        }
    }

    private void stopQuietly() {
        if (prometheusContainer != null) {
            try {
                prometheusContainer.stop();
            } catch (Exception ignored) {
            } finally {
                prometheusContainer = null;
            }
        }
    }

    private GenericContainer<?> createPrometheusContainer() {
        List<String> commands = new ArrayList<>();

        commands.add("--config.file=/etc/prometheus/prometheus.yaml");
        commands.add("--storage.tsdb.path=/prometheus");
        commands.add("--web.console.libraries=/usr/share/prometheus/console_libraries");
        commands.add("--web.console.templates=/usr/share/prometheus/consoles");

        if (prometheusDockerImage.contains("v3.")) {
            commands.add("--web.enable-otlp-receiver");
        } else {
            commands.add("--enable-feature=otlp-write-receiver");
        }

        String webYml = "/" + testClass.getName().replace(".", "/") + "/web.yaml";

        boolean hasWebYaml = ResourceSupport.exists(webYml);

        if (hasWebYaml) {
            commands.add("--web.config.file=/etc/prometheus/web.yaml");
        }

        GenericContainer<?> genericContainer = new GenericContainer<>(prometheusDockerImage)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/prometheus.yaml",
                        "/etc/prometheus/prometheus.yaml",
                        BindMode.READ_ONLY)
                .withWorkingDirectory("/prometheus")
                .withCommand(commands.toArray(new String[0]))
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withExposedPorts(9090)
                .withLogConsumer(new ContainerLogConsumer("PROMETHEUS", prometheusDockerImage))
                .withNetwork(network)
                .withNetworkAliases("prometheus")
                .waitingFor(Wait.forLogMessage(".*Server is ready to receive web requests.*", 1))
                .withStartupTimeout(Duration.ofSeconds(60));

        if (hasWebYaml) {
            genericContainer.withClasspathResourceMapping(webYml, "/etc/prometheus/web.yaml", BindMode.READ_ONLY);
        }

        return genericContainer;
    }
}
