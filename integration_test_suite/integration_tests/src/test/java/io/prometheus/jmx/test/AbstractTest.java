/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.support.JavaDockerImages;
import io.prometheus.jmx.test.support.JmxExporterMode;
import io.prometheus.jmx.test.support.TestArguments;
import io.prometheus.jmx.test.support.TestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

public abstract class AbstractTest {

    private static final String BASE_URL = "http://localhost";
    private static final long MEMORY_BYTES = 1073741824; // 1GB
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;

    @TestEngine.Argument public TestArguments testArguments;

    protected TestEnvironment testEnvironment;

    /**
     * Method to get the Collection of TestArguments
     *
     * @return the return value
     */
    @TestEngine.ArgumentSupplier
    public static Stream<TestArguments> arguments() {
        List<TestArguments> testArguments = new ArrayList<>();

        JavaDockerImages.names()
                .forEach(
                        dockerImageName -> {
                            for (JmxExporterMode jmxExporterMode : JmxExporterMode.values()) {
                                testArguments.add(
                                        TestArguments.of(
                                                dockerImageName + " / " + jmxExporterMode,
                                                dockerImageName,
                                                jmxExporterMode));
                            }
                        });

        return testArguments.stream();
    }

    @TestEngine.Prepare
    public final void prepare() {
        // Create a Network and get the id to force the network creation
        Network network = Network.newNetwork();
        network.getId();

        testEnvironment = new TestEnvironment();
        testEnvironment.setNetwork(network);
        testEnvironment.setBaseUrl(BASE_URL);
    }

    @TestEngine.BeforeAll
    public final void beforeAll() {
        testEnvironment.reset();

        Network network = testEnvironment.getNetwork();
        String dockerImageName = testArguments.getDockerImageName();
        String testName = this.getClass().getName();
        String baseUrl = testEnvironment.getBaseUrl();

        switch (testArguments.getJmxExporterMode()) {
            case JavaAgent:
                {
                    GenericContainer<?> applicationContainer =
                            createJavaAgentApplicationContainer(network, dockerImageName, testName);
                    applicationContainer.start();
                    testEnvironment.setApplicationContainer(applicationContainer);

                    HttpClient httpClient = createHttpClient(applicationContainer, baseUrl);
                    testEnvironment.setHttpClient(httpClient);

                    break;
                }
            case Standalone:
                {
                    GenericContainer<?> applicationContainer =
                            createStandaloneApplicationContainer(
                                    network, dockerImageName, testName);
                    applicationContainer.start();
                    testEnvironment.setApplicationContainer(applicationContainer);

                    GenericContainer<?> exporterContainer =
                            createStandaloneExporterContainer(network, dockerImageName, testName);
                    exporterContainer.start();
                    testEnvironment.setExporterContainer(exporterContainer);

                    HttpClient httpClient = createHttpClient(exporterContainer, baseUrl);
                    testEnvironment.setHttpClient(httpClient);

                    break;
                }
        }
    }

    @TestEngine.AfterAll
    public final void afterAll() {
        testEnvironment.reset();
    }

    @TestEngine.Conclude
    public final void conclude() {
        testEnvironment.destroy();
        testEnvironment = null;
    }

    /**
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createStandaloneApplicationContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(9999)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("application")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an exporter container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createStandaloneExporterContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forListeningPort())
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh exporter.sh")
                .withExposedPorts(8888)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("exporter")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    private static GenericContainer<?> createJavaAgentApplicationContainer(
            Network network, String dockerImageName, String testName) {
        return new GenericContainer<>(dockerImageName)
                .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                .withClasspathResourceMapping(
                        testName.replace(".", "/") + "/JavaAgent", "/temp", BindMode.READ_ONLY)
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withMemory(MEMORY_BYTES)
                                        .withMemorySwap(MEMORY_SWAP_BYTES))
                .withCreateContainerCmdModifier(
                        c ->
                                c.getHostConfig()
                                        .withUlimits(
                                                new Ulimit[] {
                                                    new Ulimit("nofile", 65536L, 65536L)
                                                }))
                .withCommand("/bin/sh application.sh")
                .withExposedPorts(8888)
                .withLogConsumer(
                        outputFrame -> {
                            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
                            if (!string.isBlank()) {
                                System.out.println(string);
                            }
                        })
                .withNetwork(network)
                .withNetworkAliases("application")
                .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                .withStartupTimeout(Duration.ofMillis(30000))
                .withWorkingDirectory("/temp");
    }

    /**
     * Method to create an HttpClient
     *
     * @param genericContainer genericContainer
     * @param baseUrl baseUrl
     * @return the return value
     */
    private static HttpClient createHttpClient(
            GenericContainer<?> genericContainer, String baseUrl) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(8888));
    }
}
