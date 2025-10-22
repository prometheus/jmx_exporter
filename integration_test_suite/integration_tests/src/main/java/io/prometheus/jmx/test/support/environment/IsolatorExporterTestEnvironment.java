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

/** Class to implement MultiExporterTestEnvironment */
public class IsolatorExporterTestEnvironment implements Argument<IsolatorExporterTestEnvironment> {

    private static final String BASE_URL = "http://localhost";
    private static final int BASE_PORT = 8888;

    private final String id;
    private final String javaDockerImage;

    private Class<?> testClass;
    private String baseUrl;
    private Network network;
    private GenericContainer<?> javaAgentApplicationContainer;

    /**
     * Constructor
     *
     * @param javaDockerImage javaDockerImage
     */
    public IsolatorExporterTestEnvironment(String javaDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.javaDockerImage = javaDockerImage;
        this.baseUrl = BASE_URL;
    }

    @Override
    public String getName() {
        return "IsolatorJavaAgent / " + javaDockerImage;
    }

    @Override
    public IsolatorExporterTestEnvironment getPayload() {
        return this;
    }

    /**
     * Method to get the ID of the test environment
     *
     * @return the ID of the test environment
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
    public IsolatorExporterTestEnvironment setBaseUrl(String baseUrl) {
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
     * Method to initialize the test environment
     *
     * @param network network
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        javaAgentApplicationContainer = createJavaAgentApplicationContainer();
        javaAgentApplicationContainer.start();
    }

    /**
     * Method to get a URL (base URL + path)
     *
     * @param index index
     * @param path path
     * @return the URL (base URL + path)
     */
    public String getUrl(int index, String path) {
        return !path.startsWith("/") ? getBaseUrl(index) + "/" + path : getBaseUrl(index) + path;
    }

    /**
     * Method to get the base URL
     *
     * @param index index
     * @return the base URL
     */
    public String getBaseUrl(int index) {
        int port = javaAgentApplicationContainer.getMappedPort(BASE_PORT + index);
        return baseUrl + ":" + port;
    }

    /** Method to destroy the test environment */
    public void destroy() {
        if (javaAgentApplicationContainer != null) {
            javaAgentApplicationContainer.stop();
            javaAgentApplicationContainer = null;
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
                .withExposedPorts(BASE_PORT, BASE_PORT + 1, BASE_PORT + 2)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*\\n", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Create the MultiExporterTestEnvironment
     *
     * @return a Stream of MultiExporterTestEnvironments
     */
    public static Stream<IsolatorExporterTestEnvironment> createEnvironments() {
        Collection<IsolatorExporterTestEnvironment> collection = new ArrayList<>();

        JavaDockerImages.names()
                .forEach(
                        dockerImageName -> {
                            collection.add(new IsolatorExporterTestEnvironment(dockerImageName));
                        });

        return collection.stream();
    }
}
