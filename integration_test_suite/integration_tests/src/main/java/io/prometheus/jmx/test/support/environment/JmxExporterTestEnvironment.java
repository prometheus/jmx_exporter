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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Test environment for the JMX exporter, supporting both Java Agent and Standalone modes.
 *
 * <p>In Java Agent mode a single container runs the exporter as a JVM agent; in Standalone
 * mode separate application and exporter containers are started. The environment requires a
 * Docker network to be passed via {@link #initialize(Network)}. The caller is responsible for
 * network creation and teardown; this class only manages the container lifecycle.
 */
public class JmxExporterTestEnvironment implements AutoCloseable {

    private static final String BASE_URL = "http://localhost";

    private final String id;
    private final Class<?> testClass;
    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private String baseUrl;
    private Network network;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;

    /**
     * Creates a JMX exporter test environment for the specified test class, Docker image, and mode.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @param javaDockerImage the Java Docker image name for the application container
     * @param jmxExporterMode the exporter operational mode (Java Agent or Standalone)
     * @throws NullPointerException if any argument is {@code null}
     */
    public JmxExporterTestEnvironment(Class<?> testClass, String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.id = UUID.randomUUID().toString();
        this.testClass = Objects.requireNonNull(testClass);
        this.javaDockerImage = Objects.requireNonNull(javaDockerImage);
        this.jmxExporterMode = Objects.requireNonNull(jmxExporterMode);
        this.baseUrl = BASE_URL;
    }

    /**
     * Returns a human-readable name identifying this environment, its mode, and Java Docker image.
     *
     * @return the display name of this environment
     */
    public String name() {
        return jmxExporterMode + "(" + javaDockerImage + ")";
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
    public JmxExporterTestEnvironment setBaseUrl(String baseUrl) {
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
     * Returns the operational mode of the JMX exporter.
     *
     * @return the exporter mode, either Java Agent or Standalone
     */
    public JmxExporterMode getJmxExporterMode() {
        return jmxExporterMode;
    }

    /**
     * Initializes this environment using the specified Docker network and starts
     * the appropriate containers. The caller is responsible for creating and closing
     * the network.
     *
     * @param network the Docker network for the containers; must not be {@code null}
     */
    public void initialize(Network network) {
        this.network = Objects.requireNonNull(network);

        switch (jmxExporterMode) {
            case JavaAgent -> {
                javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                javaAgentApplicationContainer.start();
            }
            case Standalone -> {
                standaloneApplicationContainer = createStandaloneApplicationContainer();
                standaloneApplicationContainer.start();

                standaloneExporterContainer = createStandaloneExporterContainer();
                standaloneExporterContainer.start();
            }
        }
    }

    /**
     * Returns whether the exporter container is currently running.
     *
     * @return {@code true} if the relevant exporter container is running; {@code false} otherwise
     */
    public boolean isRunning() {
        return (javaAgentApplicationContainer != null && javaAgentApplicationContainer.isRunning())
                || (standaloneExporterContainer != null && standaloneExporterContainer.isRunning());
    }

    /**
     * Constructs a URL for the specified path, prepending the base URL and mapped port.
     *
     * @param path the URL path; a leading slash is optional
     * @return the fully constructed URL
     */
    public String getUrl(String path) {
        return !path.startsWith("/") ? getBaseUrl() + "/" + path : getBaseUrl() + path;
    }

    /**
     * Returns the base URL including the mapped port for the active exporter mode.
     *
     * @return the base URL with port, e.g. {@code "http://localhost:32768"}
     */
    public String getBaseUrl() {
        int port =
                switch (jmxExporterMode) {
                    case JavaAgent -> javaAgentApplicationContainer.getMappedPort(8888);
                    case Standalone -> standaloneExporterContainer.getMappedPort(8888);
                };

        return baseUrl + ":" + port;
    }

    @Override
    public void close() {
        GenericContainer<?> javaAgentContainer = javaAgentApplicationContainer;
        GenericContainer<?> standaloneAppContainer = standaloneApplicationContainer;
        GenericContainer<?> standaloneExporter = standaloneExporterContainer;

        stopQuietly();

        ContainerSupport.waitForShutdown(javaAgentContainer);
        ContainerSupport.waitForShutdown(standaloneExporter);
        ContainerSupport.waitForShutdown(standaloneAppContainer);
    }

    /**
     * Returns the Docker network used by the containers.
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

        if (standaloneExporterContainer != null) {
            try {
                standaloneExporterContainer.stop();
            } catch (Exception ignored) {
            } finally {
                standaloneExporterContainer = null;
            }
        }

        if (standaloneApplicationContainer != null) {
            try {
                standaloneApplicationContainer.stop();
            } catch (Exception ignored) {
            } finally {
                standaloneApplicationContainer = null;
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
                .withExposedPorts(8888)
                .withLogConsumer(new ContainerLogConsumer("JMX_EXPORTER_JAVAAGENT", javaDockerImage))
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofSeconds(60))
                .withWorkingDirectory("/temp");
    }

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
                .withStartupTimeout(Duration.ofSeconds(60))
                .withWorkingDirectory("/temp");
    }

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
     * Creates one test environment per combination of configured Java Docker image and
     * JMX exporter mode for the given test class.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @return an unmodifiable list of test environments covering all image and mode combinations
     */
    public static List<JmxExporterTestEnvironment> createTestEnvironments(Class<?> testClass) {
        return JavaDockerImages.names().stream()
                .flatMap(dockerImageName -> Arrays.stream(JmxExporterMode.values())
                        .map(jmxExporterMode ->
                                new JmxExporterTestEnvironment(testClass, dockerImageName, jmxExporterMode)))
                .toList();
    }
}
