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
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.paramixel.core.support.Retry;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Manages a Prometheus Docker container for integration testing, providing lifecycle
 * control and readiness checks.
 */
public class PrometheusTestEnvironment implements AutoCloseable {

    private static final String BASE_URL = "http://localhost";

    private final String id;
    private final String prometheusDockerImage;

    private Class<?> testClass;
    private Network network;
    private String baseUrl;
    private GenericContainer<?> prometheusContainer;

    /**
     * Creates a Prometheus test environment using the specified Docker image.
     *
     * @param prometheusDockerImage the Docker image name for the Prometheus container
     */
    public PrometheusTestEnvironment(String prometheusDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.prometheusDockerImage = prometheusDockerImage;
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns the unique identifier of the test environment.
     *
     * @return the unique identifier of the test environment
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the base URL for constructing Prometheus request URLs.
     *
     * @param baseUrl the base URL (e.g., {@code http://localhost})
     * @return this test environment for method chaining
     */
    public PrometheusTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Returns the Prometheus Docker image name.
     *
     * @return the Prometheus Docker image name
     */
    public String getPrometheusDockerImage() {
        return prometheusDockerImage;
    }

    /**
     * Starts the Prometheus Docker container for the test environment.
     *
     * @param testClass the test class whose classpath resources configure the container
     * @param network the Docker network for inter-container communication
     * @throws IllegalStateException if the Prometheus container fails to start
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        prometheusContainer = createPrometheusContainer();
        prometheusContainer.start();

        if (!prometheusContainer.isRunning()) {
            throw new IllegalStateException("Prometheus container is not running");
        }
    }

    /**
     * Constructs a Prometheus URL by appending the specified path to the Prometheus base URL.
     *
     * @param path the path to append (with or without a leading slash)
     * @return the full Prometheus URL
     */
    public String getPrometheusUrl(String path) {
        return !path.startsWith("/") ? getPrometheusBaseUrl() + "/" + path : getPrometheusBaseUrl() + path;
    }

    /**
     * Returns the Prometheus base URL including the dynamically mapped port.
     *
     * @return the Prometheus base URL with the mapped port
     */
    private String getPrometheusBaseUrl() {
        return baseUrl + ":" + prometheusContainer.getMappedPort(9090);
    }

    /**
     * Waits for the Prometheus container to become ready by polling the {@code /-/ready} endpoint.
     */
    public void waitForReady() {
        waitForReady(null, null);
    }

    /**
     * Waits for the Prometheus container to become ready by polling the {@code /-/ready} endpoint
     * with optional Basic authentication credentials.
     *
     * @param username the username for Basic authentication, or {@code null} if unauthenticated
     * @param password the password for Basic authentication, or {@code null} if unauthenticated
     * @throws EnvironmentException if the container does not become ready within the retry limit
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

        if (!result.isPass()) {
            throw new EnvironmentException(
                    format("Prometheus [%s] not ready after retry", prometheusContainer.getDockerImageName()));
        }
    }

    /**
     * Stops the Prometheus Docker container and releases resources.
     */
    public void close() {
        if (prometheusContainer != null) {
            prometheusContainer.stop();
            prometheusContainer = null;
        }
    }

    /**
     * Creates a Docker container for Prometheus with classpath-mapped configuration files
     * and OTLP receiver support.
     *
     * @return the configured GenericContainer
     */
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
                .withStartupTimeout(Duration.ofMillis(60000));

        if (hasWebYaml) {
            genericContainer.withClasspathResourceMapping(webYml, "/etc/prometheus/web.yaml", BindMode.READ_ONLY);
        }

        return genericContainer;
    }
}
