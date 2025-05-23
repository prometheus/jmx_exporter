/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support;

import io.prometheus.jmx.common.util.ResourceSupport;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.verifyica.api.Argument;

/** Class to implement OpenTelemetryTestEnvironment */
public class OpenTelemetryTestEnvironment implements Argument<OpenTelemetryTestEnvironment> {

    private static final long MEMORY_BYTES = 1073741824; // 1 GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;
    private static final String BASE_URL = "http://localhost";

    private final String prometheusDockerImage;
    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private Class<?> testClass;
    private Network network;
    private String baseUrl;
    private GenericContainer<?> prometheusContainer;
    private GenericContainer<?> standaloneApplicationContainer;
    private GenericContainer<?> javaAgentApplicationContainer;
    private GenericContainer<?> standaloneExporterContainer;

    /**
     * Constructor
     *
     * @param prometheusDockerImage prometheusDockerImage
     * @param javaDockerImage javaDockerImage
     * @param jmxExporterMode jmxExporterMode
     */
    public OpenTelemetryTestEnvironment(
            String prometheusDockerImage, String javaDockerImage, JmxExporterMode jmxExporterMode) {
        this.prometheusDockerImage = prometheusDockerImage;
        this.javaDockerImage = javaDockerImage;
        this.jmxExporterMode = jmxExporterMode;
        this.baseUrl = BASE_URL;
    }

    @Override
    public String getName() {
        return prometheusDockerImage + " / " + javaDockerImage + " / " + jmxExporterMode;
    }

    @Override
    public OpenTelemetryTestEnvironment getPayload() {
        return this;
    }

    /**
     * Method to set the base URL
     *
     * @param baseUrl baseUrl
     * @return the ExporterTestEnvironment
     */
    public OpenTelemetryTestEnvironment setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Method to get the Prometheus Docker image name
     *
     * @return the Prometheus Docker image name
     */
    public String getPrometheusDockerImage() {
        return prometheusDockerImage;
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
                    prometheusContainer = createPrometheusContainer();
                    prometheusContainer.start();

                    javaAgentApplicationContainer = createJavaAgentApplicationContainer();
                    javaAgentApplicationContainer.start();

                    break;
                }
            case Standalone:
                {
                    prometheusContainer = createPrometheusContainer();
                    prometheusContainer.start();

                    standaloneApplicationContainer = createStandaloneApplicationContainer();
                    standaloneApplicationContainer.start();

                    standaloneExporterContainer = createStandaloneExporterContainer();
                    standaloneExporterContainer.start();

                    break;
                }
        }

        if (standaloneApplicationContainer != null && !standaloneApplicationContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
        }

        if (standaloneApplicationContainer != null && !standaloneApplicationContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
        }

        if (prometheusContainer != null && !prometheusContainer.isRunning()) {
            throw new IllegalStateException("standalone exporter container is not running");
        }
    }

    /**
     * Method to get an exporter URL
     *
     * @param path path
     * @return an exporter URL
     */
    public String getExporterUrl(String path) {
        return !path.startsWith("/")
                ? getExporterBaseUrl() + "/" + path
                : getExporterBaseUrl() + path;
    }

    /**
     * Method to get the exporter base URL
     *
     * @return the exporter base URL
     */
    private String getExporterBaseUrl() {
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

    /**
     * Method to get a Prometheus URL
     *
     * @param path path
     * @return a Prometheus URL
     */
    public String getPrometheusUrl(String path) {
        return !path.startsWith("/")
                ? getPrometheusBaseUrl() + "/" + path
                : getPrometheusBaseUrl() + path;
    }

    /**
     * Method to get the Prometheus base URL
     *
     * @return the Prometheus base URL
     */
    private String getPrometheusBaseUrl() {
        return baseUrl + ":" + prometheusContainer.getMappedPort(9090);
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

        if (prometheusContainer != null) {
            prometheusContainer.stop();
            prometheusContainer = null;
        }
    }

    /**
     * Method to create a Prometheus container
     *
     * @return the return value
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

        String webYml =
                "/" + testClass.getName().replace(".", "/") + "/" + jmxExporterMode + "/web.yaml";

        boolean hasWebYaml = hasResource(webYml);

        if (hasWebYaml) {
            commands.add("--web.config.file=/etc/prometheus/web.yaml");
        }

        GenericContainer<?> genericContainer =
                new GenericContainer<>(prometheusDockerImage)
                        .withClasspathResourceMapping(
                                testClass.getName().replace(".", "/")
                                        + "/"
                                        + jmxExporterMode
                                        + "/prometheus.yaml",
                                "/etc/prometheus/prometheus.yaml",
                                BindMode.READ_ONLY)
                        .withWorkingDirectory("/prometheus")
                        .withCommand(commands.toArray(new String[0]))
                        .withCreateContainerCmdModifier(TestContainerConfigureCmd.getInstance())
                        .withExposedPorts(9090)
                        .withLogConsumer(
                                outputFrame -> {
                                    String string =
                                            outputFrame.getUtf8StringWithoutLineEnding().trim();
                                    if (!string.isBlank()) {
                                        System.out.println("> " + string);
                                    }
                                })
                        .withNetwork(network)
                        .withNetworkAliases("prometheus")
                        .withStartupTimeout(Duration.ofMillis(60000))
                        .waitingFor(
                                Wait.forLogMessage(
                                        ".*Server is ready to receive web requests.*", 1));

        if (hasWebYaml) {
            genericContainer.withClasspathResourceMapping(
                    webYml, "/etc/prometheus/web.yaml", BindMode.READ_ONLY);
        }

        return genericContainer;
    }

    /**
     * Method to create an application container
     *
     * @return the return value
     */
    private GenericContainer<?> createJavaAgentApplicationContainer() {
        return new GenericContainer<>(javaDockerImage)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/JavaAgent",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(TestContainerConfigureCmd.getInstance())
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(8888)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("application")
                .waitingFor(Wait.forLogMessage(".*JmxExampleApplication \\| Running.*", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
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
                .withCreateContainerCmdModifier(TestContainerConfigureCmd.getInstance())
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
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testClass.getName().replace(".", "/") + "/Standalone",
                        "/temp",
                        BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(TestContainerConfigureCmd.getInstance())
                .withCommand("/bin/sh exporter.sh")
                .withExposedPorts(8888)
                .withLogConsumer(TestContainerLogger.getInstance())
                .withNetwork(network)
                .withNetworkAliases("exporter")
                .waitingFor(Wait.forLogMessage(".*Standalone \\| Running.*", 1))
                .withStartupTimeout(Duration.ofMillis(60000))
                .withWorkingDirectory("/temp")
                .waitingFor(Wait.forLogMessage(".*Running.*", 1));
    }

    /**
     * Method to determine if a resource exists
     *
     * @param resource resource
     * @return true if the resource exists, else false
     */
    private static boolean hasResource(String resource) {
        boolean hasResource = false;

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        InputStream inputStream = ResourceSupport.class.getResourceAsStream(resource);
        if (inputStream != null) {
            try (BufferedReader bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    hasResource = true;
                }
            } catch (Throwable t) {
                // INTENTIONALLY BLANK
            }
        }

        return hasResource;
    }

    /**
     * Create the OpenTelemetryTestEnvironments
     *
     * @return a Stream of OpenTelemetryTestEnvironments
     */
    public static Stream<OpenTelemetryTestEnvironment> createOpenTelemetryTestEnvironments() {
        Collection<OpenTelemetryTestEnvironment> openTelemetryTestEnvironments = new ArrayList<>();

        PrometheusDockerImages.names()
                .forEach(
                        prometheusDockerImage ->
                                JavaDockerImages.names()
                                        .forEach(
                                                javaDockerImageName -> {
                                                    for (JmxExporterMode jmxExporterMode :
                                                            JmxExporterMode.values()) {
                                                        openTelemetryTestEnvironments.add(
                                                                new OpenTelemetryTestEnvironment(
                                                                        prometheusDockerImage,
                                                                        javaDockerImageName,
                                                                        jmxExporterMode));
                                                    }
                                                }));

        return openTelemetryTestEnvironments.stream();
    }
}
