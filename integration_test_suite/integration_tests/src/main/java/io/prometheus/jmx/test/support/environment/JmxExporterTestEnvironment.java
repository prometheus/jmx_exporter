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
import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.TestSupport;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.altcontainers.api.Container;
import org.altcontainers.api.ContainerSpec;
import org.altcontainers.api.Network;
import org.altcontainers.api.OutputFrame;

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
    private static final String MODE_DIRECTORY = "mode";
    private static final long MEMORY_BYTES = 1073741824L;
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;
    private static final long NOFILE_SOFT_LIMIT = 65536L;
    private static final long NOFILE_HARD_LIMIT = 65536L;

    private final String id;
    private final Class<?> testClass;
    private final String javaDockerImage;
    private final JmxExporterMode jmxExporterMode;

    private String baseUrl;
    private Network network;
    private Container standaloneApplicationContainer;
    private Container javaAgentApplicationContainer;
    private Container standaloneExporterContainer;

    private static Consumer<OutputFrame> prefixedLogConsumer(String prefix, String image) {
        return frame -> System.out.println("[" + prefix + "] " + image + " | " + frame.utf8StringWithoutLineEnding());
    }

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
            }
            case Standalone -> {
                standaloneApplicationContainer = createStandaloneApplicationContainer();
                standaloneExporterContainer = createStandaloneExporterContainer();
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
                    case JavaAgent -> javaAgentApplicationContainer.hostPort(8888);
                    case Standalone -> standaloneExporterContainer.hostPort(8888);
                };

        return baseUrl + ":" + port;
    }

    @Override
    public void close() {
        RuntimeException firstException = null;
        firstException = closeContainer(javaAgentApplicationContainer, firstException);
        javaAgentApplicationContainer = null;
        firstException = closeContainer(standaloneExporterContainer, firstException);
        standaloneExporterContainer = null;
        firstException = closeContainer(standaloneApplicationContainer, firstException);
        standaloneApplicationContainer = null;
        if (firstException != null) {
            throw firstException;
        }
    }

    /**
     * Destroys a container, collecting the first failure for later rethrow so that
     * remaining containers are still cleaned up.
     *
     * @param container the container to destroy, or {@code null}
     * @param firstException the first exception encountered so far, or {@code null}
     * @return {@code firstException} if already set, otherwise the first exception from this destroy
     */
    private static RuntimeException closeContainer(Container container, RuntimeException firstException) {
        if (container == null) {
            return firstException;
        }
        try {
            container.close();
        } catch (RuntimeException e) {
            if (firstException == null) {
                return e;
            }
            firstException.addSuppressed(e);
        }
        return firstException;
    }

    /**
     * Returns the Docker network used by the containers.
     *
     * @return the Docker network; may be {@code null} if not yet initialized
     */
    public Network getNetwork() {
        return network;
    }

    private Container createJavaAgentApplicationContainer() {
        ContainerSpec containerSpec = ContainerSpec.builder(javaDockerImage)
                .bindDirectory(
                        TestSupport.resolveClasspathDirectory(testClass, "common")
                                .toString(),
                        "/common")
                .bindDirectory(
                        TestSupport.copyClasspathDirectoryToTemp(
                                        testClass, resolveModeResourceDirectory(JmxExporterMode.JavaAgent))
                                .toString(),
                        "/temp")
                .command("/bin/sh", "application.sh")
                .exposePorts(8888)
                .network(network, "application")
                .waitForContainerPort(8888)
                .waitForLogMessage(".*JmxExampleApplication \\| Running.*")
                .workingDirectory("/temp")
                .onOutput(prefixedLogConsumer("JMX_EXPORTER_JAVAAGENT", javaDockerImage))
                .startupAttempts(3)
                .memory(MEMORY_BYTES)
                .memorySwap(MEMORY_SWAP_BYTES)
                .ulimit("nofile", NOFILE_SOFT_LIMIT, NOFILE_HARD_LIMIT)
                .build();

        return Container.create(containerSpec);
    }

    private Container createStandaloneApplicationContainer() {
        ContainerSpec containerSpec = ContainerSpec.builder(javaDockerImage)
                .bindDirectory(
                        TestSupport.resolveClasspathDirectory(testClass, "common")
                                .toString(),
                        "/common")
                .bindDirectory(
                        TestSupport.copyClasspathDirectoryToTemp(
                                        testClass, resolveModeResourceDirectory(JmxExporterMode.Standalone))
                                .toString(),
                        "/temp")
                .command("/bin/sh", "application.sh")
                .exposePorts(9999)
                .network(network, "application")
                .waitForLogMessage(".*JmxExampleApplication \\| Running.*")
                .workingDirectory("/temp")
                .onOutput(prefixedLogConsumer("EXAMPLE_APPLICATION", javaDockerImage))
                .startupAttempts(3)
                .memory(MEMORY_BYTES)
                .memorySwap(MEMORY_SWAP_BYTES)
                .ulimit("nofile", NOFILE_SOFT_LIMIT, NOFILE_HARD_LIMIT)
                .build();

        return Container.create(containerSpec);
    }

    private Container createStandaloneExporterContainer() {
        ContainerSpec containerSpec = ContainerSpec.builder(javaDockerImage)
                .bindDirectory(
                        TestSupport.resolveClasspathDirectory(testClass, "common")
                                .toString(),
                        "/common")
                .bindDirectory(
                        TestSupport.copyClasspathDirectoryToTemp(
                                        testClass, resolveModeResourceDirectory(JmxExporterMode.Standalone))
                                .toString(),
                        "/temp")
                .command("/bin/sh", "exporter.sh")
                .exposePorts(8888)
                .network(network, "exporter")
                .waitForLogMessage(".*Standalone \\| Running.*")
                .workingDirectory("/temp")
                .onOutput(prefixedLogConsumer("JMX_EXPORTER_STANDALONE", javaDockerImage))
                .startupAttempts(3)
                .memory(MEMORY_BYTES)
                .memorySwap(MEMORY_SWAP_BYTES)
                .ulimit("nofile", NOFILE_SOFT_LIMIT, NOFILE_HARD_LIMIT)
                .build();

        return Container.create(containerSpec);
    }

    private String resolveModeResourceDirectory(JmxExporterMode mode) {
        String modeResourceDirectory = testResourceBasePath() + "/" + MODE_DIRECTORY + "/" + mode;
        if (ResourceSupport.exists(modeResourceDirectory + "/application.sh")) {
            return modeResourceDirectory;
        }

        String legacyResourceDirectory = testResourceBasePath() + "/" + mode;
        if (ResourceSupport.exists(legacyResourceDirectory + "/application.sh")) {
            return legacyResourceDirectory;
        }

        throw new EnvironmentException(
                format("Resource directory [%s] or [%s] not found", modeResourceDirectory, legacyResourceDirectory));
    }

    private String testResourceBasePath() {
        return testClass.getName().replace(".", "/");
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
