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

import io.prometheus.jmx.test.support.JavaDockerImages;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Manages an isolated JMX exporter Docker container that exposes multiple ports
 * for testing multiple exporter instances simultaneously.
 */
public class IsolatorExporterTestEnvironment {

    private static final String BASE_URL = "http://localhost";
    private static final int BASE_PORT = 8888;

    private final String id;
    private final String javaDockerImage;

    private Class<?> testClass;
    private String baseUrl;
    private Network network;
    private GenericContainer<?> javaAgentApplicationContainer;

    /**
     * Creates an isolator exporter test environment using the specified Java Docker image.
     *
     * @param javaDockerImage the Docker image name for the Java application container
     */
    public IsolatorExporterTestEnvironment(String javaDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.javaDockerImage = javaDockerImage;
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns the display name of the test environment.
     *
     * @return the display name of the test environment
     */
    public String getName() {
        return "IsolatorJavaAgent / " + javaDockerImage;
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
     * Sets the base URL for constructing test request URLs.
     *
     * @param baseUrl the base URL (e.g., {@code http://localhost})
     * @return this test environment for method chaining
     */
    public IsolatorExporterTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Returns the Java Docker image name used for the application container.
     *
     * @return the Java Docker image name
     */
    public String getJavaDockerImage() {
        return javaDockerImage;
    }

    /**
     * Starts the Docker container for the test environment.
     *
     * @param testClass the test class whose classpath resources configure the container
     * @param network the Docker network for inter-container communication
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        javaAgentApplicationContainer = createJavaAgentApplicationContainer();
        javaAgentApplicationContainer.start();
    }

    /**
     * Constructs a URL by appending the specified path to the base URL for the given port index.
     *
     * @param index the port index (0, 1, or 2) selecting which exposed exporter port to use
     * @param path the path to append (with or without a leading slash)
     * @return the full URL
     */
    public String getUrl(int index, String path) {
        return !path.startsWith("/") ? getBaseUrl(index) + "/" + path : getBaseUrl(index) + path;
    }

    /**
     * Returns the base URL including the dynamically mapped port for the specified port index.
     *
     * @param index the port index (0, 1, or 2) selecting which exposed exporter port to use
     * @return the base URL with the mapped port
     */
    public String getBaseUrl(int index) {
        int port = javaAgentApplicationContainer.getMappedPort(BASE_PORT + index);
        return baseUrl + ":" + port;
    }

    /**
     * Stops the Docker container and releases resources.
     */
    public void destroy() {
        if (javaAgentApplicationContainer != null) {
            javaAgentApplicationContainer.stop();
            javaAgentApplicationContainer = null;
        }
    }

    /**
     * Creates a Docker container for the Java Agent mode, exposing multiple exporter ports
     * (8888, 8889, 8890) for isolated metric scraping.
     *
     * @return the configured GenericContainer
     */
    private GenericContainer<?> createJavaAgentApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/JavaAgent", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(BASE_PORT, BASE_PORT + 1, BASE_PORT + 2)
                .withLogConsumer(new ContainerLogConsumer("JMX_EXPORTER_ISOLATOR_JAVAAGENT", javaDockerImage))
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*\\n", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Creates a stream of isolator exporter test environments for all configured Java Docker images.
     *
     * @return a stream of {@link IsolatorExporterTestEnvironment} instances
     */
    public static Stream<IsolatorExporterTestEnvironment> createEnvironments() {
        Collection<IsolatorExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names().forEach(dockerImageName -> {
            collection.add(new IsolatorExporterTestEnvironment(dockerImageName));
        });

        return collection.stream();
    }
}
