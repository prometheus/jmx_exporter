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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test environment for the isolator Java agent mode, managing a single Docker container
 * that runs the JMX exporter as a Java agent inside the application JVM.
 *
 * <p>The environment requires a Docker network to be passed via {@link #initialize(Network)}.
 * The caller is responsible for network creation and teardown; this class only manages
 * the container lifecycle.
 */
public class IsolatorExporterTestEnvironment implements AutoCloseable {

    private static final String BASE_URL = "http://localhost";
    private static final int BASE_PORT = 8888;

    private final String id;
    private final Class<?> testClass;
    private final String javaDockerImage;

    private String baseUrl;
    private Network network;
    private GenericContainer<?> javaAgentApplicationContainer;

    /**
     * Creates an isolator exporter test environment for the specified test class and Java Docker image.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @param javaDockerImage the Java Docker image name for the application container
     * @throws NullPointerException if {@code testClass} or {@code javaDockerImage} is {@code null}
     */
    public IsolatorExporterTestEnvironment(Class<?> testClass, String javaDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.testClass = Objects.requireNonNull(testClass);
        this.javaDockerImage = Objects.requireNonNull(javaDockerImage);
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns a human-readable name identifying this environment and its Java Docker image.
     *
     * @return the display name of this environment
     */
    public String name() {
        return "IsolatorJavaAgent / " + javaDockerImage;
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
     * Sets the base URL used to construct request URLs, returning this instance for chaining.
     *
     * @param baseUrl the base URL (e.g. {@code "http://localhost"}); must not be {@code null}
     * @return this environment instance
     */
    public IsolatorExporterTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Returns the Java Docker image name used by the application container.
     *
     * @return the Java Docker image name
     */
    public String getJavaDockerImage() {
        return javaDockerImage;
    }

    /**
     * Initializes this environment using the specified Docker network and starts
     * the application container. The caller is responsible for creating and closing
     * the network.
     *
     * @param network the Docker network for the application container; must not be {@code null}
     */
    public void initialize(Network network) {
        this.network = Objects.requireNonNull(network);

        javaAgentApplicationContainer = createJavaAgentApplicationContainer();
        javaAgentApplicationContainer.start();
    }

    /**
     * Returns whether the application container is currently running.
     *
     * @return {@code true} if the container is running; {@code false} otherwise
     */
    public boolean isRunning() {
        return javaAgentApplicationContainer != null && javaAgentApplicationContainer.isRunning();
    }

    /**
     * Constructs a URL for the specified port index and path.
     *
     * @param index the zero-based port offset added to the base port
     * @param path the URL path; a leading slash is optional
     * @return the fully constructed URL
     */
    public String getUrl(int index, String path) {
        return !path.startsWith("/") ? getBaseUrl(index) + "/" + path : getBaseUrl(index) + path;
    }

    /**
     * Returns the base URL including the mapped port for the specified index.
     *
     * @param index the zero-based port offset added to the base port
     * @return the base URL with port, e.g. {@code "http://localhost:32768"}
     */
    public String getBaseUrl(int index) {
        int port = javaAgentApplicationContainer.getMappedPort(BASE_PORT + index);
        return baseUrl + ":" + port;
    }

    @Override
    public void close() {
        GenericContainer<?> container = javaAgentApplicationContainer;
        stopQuietly();
        ContainerSupport.waitForShutdown(container);
    }

    /**
     * Returns the Docker network used by the application container.
     *
     * @return the Docker network; may be {@code null} if not yet initialized
     */
    public Network getNetwork() {
        return network;
    }

    private void stopQuietly() {
        if (javaAgentApplicationContainer != null) {
            try {
                javaAgentApplicationContainer.stop();
            } catch (Exception ignored) {
            } finally {
                javaAgentApplicationContainer = null;
            }
        }
    }

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
                .withStartupTimeout(Duration.ofSeconds(60))
                .withWorkingDirectory("/temp");
    }

    /**
     * Creates one test environment per configured Java Docker image for the given test class.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @return an unmodifiable list of test environments, one per Java Docker image
     */
    public static List<IsolatorExporterTestEnvironment> createTestEnvironments(Class<?> testClass) {
        return JavaDockerImages.names().stream()
                .map(dockerImageName -> new IsolatorExporterTestEnvironment(testClass, dockerImageName))
                .toList();
    }
}
