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
import io.prometheus.jmx.test.support.util.TestContainerLogger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.verifyica.api.Argument;

/** Class to implement ExporterTestEnvironment */
public class JmxExporterTestEnvironment implements Argument<JmxExporterTestEnvironment> {

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
     * Constructor
     *
     * @param javaDockerImage javaDockerImage
     * @param jmxExporterMode jmxExporterMode
     */
    public JmxExporterTestEnvironment(String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.id = UUID.randomUUID().toString();
        this.javaDockerImage = javaDockerImage;
        this.jmxExporterMode = jmxExporterMode;
        this.baseUrl = BASE_URL;
    }

    @Override
    public String getName() {
        return jmxExporterMode + " / " + javaDockerImage;
    }

    @Override
    public JmxExporterTestEnvironment getPayload() {
        return this;
    }

    /**
     * Method to get the ID of the ExporterTestEnvironment
     *
     * @return the ID of the ExporterTestEnvironment
     */
    public String getId() {
        return id;
    }

    /**
     * Method to set the base URL
     *
     * @param baseUrl baseUrl
     * @return the ExporterTestEnvironment
     */
    public JmxExporterTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
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
                    javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                    javaAgentApplicationContainer.start();

                    break;
                }
            case Standalone:
                {
                    standaloneApplicationContainer = createStandaloneApplicationContainer();
                    standaloneApplicationContainer.start();

                    standaloneExporterContainer = createStandaloneExporterContainer();
                    standaloneExporterContainer.start();

                    break;
                }
        }
    }

    /**
     * Method to get a URL (base URL + path)
     *
     * @param path path
     * @return the URL (base URL + path)
     */
    public String getUrl(String path) {
        return !path.startsWith("/") ? getBaseUrl() + "/" + path : getBaseUrl() + path;
    }

    /**
     * Method to get the base URL
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        int port = 0;

        switch (jmxExporterMode) {
            case JavaAgent:
                {
                    port = javaAgentApplicationContainer.getMappedPort(8888);
                    break;
                }
            case Standalone:
                {
                    port = standaloneExporterContainer.getMappedPort(8888);
                    break;
                }
        }

        return baseUrl + ":" + port;
    }

    /** Method to destroy the test environment */
    public void destroy() {
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
     * Method to create an application container
     *
     * @return the return value
     */
    private GenericContainer<?> createJavaAgentApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/JavaAgent",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(8888)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofSeconds(60))
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
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(9999)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an exporter container
     *
     * @return the return value
     */
    private GenericContainer<?> createStandaloneExporterContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
                .withCommand("/bin/sh exporter.sh")
                .withExposedPorts(8888)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("exporter")
                .waitingFor(Wait.forLogMessage(".*Standalone \\| Running.*", 1))
                .withWorkingDirectory("/temp");
    }

    /**
     * Create the ExporterTestEnvironments
     *
     * @return a Stream of ExporterTestEnvironments
     */
    public static Stream<JmxExporterTestEnvironment> createEnvironments() {
        Collection<JmxExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names()
                .forEach(
                        dockerImageName -> {
                            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                                collection.add(
                                        new JmxExporterTestEnvironment(
                                                dockerImageName, jmxExporterMode));
                            }
                        });

        return collection.stream();
    }
}
