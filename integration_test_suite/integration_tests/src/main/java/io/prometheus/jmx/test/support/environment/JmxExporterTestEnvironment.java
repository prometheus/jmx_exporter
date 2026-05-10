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
 * Manages JMX exporter Docker containers in Java Agent or Standalone mode for integration testing.
 *
 * <p>In Java Agent mode, a single container runs the application with the JMX exporter agent attached.
 * In Standalone mode, separate application and exporter containers are started.
 */
public class JmxExporterTestEnvironment implements AutoCloseable {

    private static final String BASE_URL = "http://localhost";

    private final String id;
    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private Class<?> testClass;
    private String baseUrl;
    private Network network;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;

    /**
     * Creates a JMX exporter test environment for the specified Docker image and exporter mode.
     *
     * @param javaDockerImage the Docker image name for the Java application container
     * @param jmxExporterMode the operational mode of the JMX exporter
     */
    public JmxExporterTestEnvironment(String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.id = UUID.randomUUID().toString();
        this.javaDockerImage = javaDockerImage;
        this.jmxExporterMode = jmxExporterMode;
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns the display name of the test environment, combining mode and Docker image.
     *
     * @return the display name of the test environment
     */
    public String getName() {
        return jmxExporterMode + "(" + javaDockerImage + ")";
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
    public JmxExporterTestEnvironment setBaseUrl(String baseUrl) {
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
     * Returns the JMX exporter operational mode.
     *
     * @return the JMX exporter operational mode
     */
    public JmxExporterMode getJmxExporterMode() {
        return jmxExporterMode;
    }

    /**
     * Starts the Docker containers for the test environment.
     *
     * @param testClass the test class whose classpath resources configure the containers
     * @param network the Docker network for inter-container communication
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        switch (jmxExporterMode) {
            case JavaAgent: {
                javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                javaAgentApplicationContainer.start();

                break;
            }
            case Standalone: {
                standaloneApplicationContainer = createStandaloneApplicationContainer();
                standaloneApplicationContainer.start();

                standaloneExporterContainer = createStandaloneExporterContainer();
                standaloneExporterContainer.start();

                break;
            }
        }
    }

    /**
     * Constructs a URL by appending the specified path to the base URL.
     *
     * @param path the path to append (with or without a leading slash)
     * @return the full URL
     */
    public String getUrl(String path) {
        return !path.startsWith("/") ? getBaseUrl() + "/" + path : getBaseUrl() + path;
    }

    /**
     * Returns the base URL including the dynamically mapped port for the exporter endpoint.
     *
     * @return the base URL with the mapped port
     */
    public String getBaseUrl() {
        int port = 0;

        switch (jmxExporterMode) {
            case JavaAgent: {
                port = javaAgentApplicationContainer.getMappedPort(8888);
                break;
            }
            case Standalone: {
                port = standaloneExporterContainer.getMappedPort(8888);
                break;
            }
        }

        return baseUrl + ":" + port;
    }

    /**
     * Stops all running Docker containers and releases resources.
     */
    public void close() {
        if (javaAgentApplicationContainer != null) {
            javaAgentApplicationContainer.stop();
            javaAgentApplicationContainer = null;
        }

        if (standaloneExporterContainer != null) {
            standaloneExporterContainer.stop();
            standaloneExporterContainer = null;
        }

        if (standaloneApplicationContainer != null) {
            standaloneApplicationContainer.stop();
            standaloneApplicationContainer = null;
        }
    }

    /**
     * Creates a Docker container for the Java Agent mode, running the application
     * with the JMX exporter agent attached on port 8888.
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
                .withExposedPorts(8888)
                .withLogConsumer(new ContainerLogConsumer("JMX_EXPORTER_JAVAAGENT", javaDockerImage))
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofSeconds(60))
                .withWorkingDirectory("/temp");
    }

    /**
     * Creates a Docker container for the example application in Standalone mode,
     * exposing JMX on port 9999.
     *
     * @return the configured GenericContainer
     */
    private GenericContainer<?> createStandaloneApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(9999)
                .withLogConsumer(new ContainerLogConsumer("EXAMPLE_APPLICATION", javaDockerImage))
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Creates a Docker container for the standalone JMX exporter,
     * scraping the application JMX endpoint and exposing metrics on port 8888.
     *
     * @return the configured GenericContainer
     */
    private GenericContainer<?> createStandaloneExporterContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh exporter.sh")
                .withExposedPorts(8888)
                .withLogConsumer(new ContainerLogConsumer("JMX_EXPORTER_STANDALONE", javaDockerImage))
                .withNetwork(network)
                .withNetworkAliases("exporter")
                .waitingFor(Wait.forLogMessage(".*Standalone \\| Running.*", 1))
                .withWorkingDirectory("/temp");
    }

    /**
     * Creates a stream of JMX exporter test environments for all combinations
     * of configured Java Docker images and exporter modes.
     *
     * @return a stream of {@link JmxExporterTestEnvironment} instances
     */
    public static Stream<JmxExporterTestEnvironment> createEnvironments() {
        Collection<JmxExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names().forEach(dockerImageName -> {
            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                collection.add(new JmxExporterTestEnvironment(dockerImageName, jmxExporterMode));
            }
        });

        return collection.stream();
    }
}
