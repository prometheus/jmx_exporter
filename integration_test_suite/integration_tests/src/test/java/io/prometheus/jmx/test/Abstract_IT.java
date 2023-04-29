/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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
import org.antublue.test.engine.api.Parameter;
import org.antublue.test.engine.api.ParameterMap;
import org.antublue.test.engine.api.TestEngine;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Abstract_IT {

    public static final String DOCKER_IMAGE_NAME = "dockerImageName";
    public static final String IS_JAVA_6 = "isJava6";
    public static final String MODE = "mode";

    /**
     * Method to get the list of Docker image names
     *
     * @return the return value
     */
    @TestEngine.ParameterSupplier
    protected static Stream<Parameter> parameters() {
        List<Parameter> parameters = new ArrayList<>();

        DockerImageNames
                .names()
                .forEach(dockerImageName -> {
                    for (Mode mode : Mode.VALUES) {
                        parameters.add(
                                ParameterMap
                                        .named(dockerImageName + " / " + mode)
                                        .put("dockerImageName", dockerImageName)
                                        .put("mode", mode)
                                        .put("isJava6", dockerImageName.contains(":6"))
                                        .parameter());
                    }
                });

        return parameters.stream();
    }

    /**
     * Method to create a Network
     *
     * @return the return value
     */
    protected static Network createNetwork() {
        Network network = Network.newNetwork();

        // Get the id to force the network creation
        network.getId();

        return network;
    }

    /**
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    protected static GenericContainer<?> createStandaloneApplicationContainer(
            Network network, String dockerImageName, String testName) {
        GenericContainer<?> applicationContainer =
                new GenericContainer<>(dockerImageName)
                        .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                        .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                        .withClasspathResourceMapping(testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(c -> c.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                        .withCommand("/bin/sh application.sh")
                        .withExposedPorts(9999)
                        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                        .withNetwork(network)
                        .withNetworkAliases("application")
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withStartupTimeout(Duration.ofMillis(30000))
                        .withWorkingDirectory("/temp");

        if (DockerImageNames.isJava6(dockerImageName)) {
            applicationContainer.withCommand("/bin/sh application_java6.sh");
        }

        return applicationContainer;
    }

    /**
     * Method to create an exporter container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    protected static GenericContainer<?> createStandaloneExporterContainer(
            Network network, String dockerImageName, String testName) {
        // Exporter container
        GenericContainer<?> exporterContainer =
                new GenericContainer<>(dockerImageName)
                        .waitingFor(Wait.forHttp("/"))
                        .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                        .withClasspathResourceMapping(testName.replace(".", "/") + "/Standalone", "/temp", BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(c -> c.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                        .withCommand("/bin/sh exporter.sh")
                        .withExposedPorts(8888)
                        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                        .withNetwork(network)
                        .withNetworkAliases("exporter")
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withStartupTimeout(Duration.ofMillis(30000))
                        .withWorkingDirectory("/temp");

        if (DockerImageNames.isJava6(dockerImageName)) {
            exporterContainer.withCommand("/bin/sh exporter_java6.sh");
        }

        return exporterContainer;
    }

    /**
     * Method to create an application container
     *
     * @param network network
     * @param dockerImageName dockerImageName
     * @param testName testName
     * @return the return value
     */
    protected static GenericContainer<?> createJavaAgentApplicationContainer(
            Network network, String dockerImageName, String testName) {
        GenericContainer<?> applicationContainer =
                new GenericContainer<>(dockerImageName)
                        .waitingFor(Wait.forHttp("/"))
                        .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                        .withClasspathResourceMapping(testName.replace(".", "/") + "/JavaAgent", "/temp", BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(c -> c.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                        .withCommand("/bin/sh application.sh")
                        .withExposedPorts(8888)
                        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                        .withNetwork(network)
                        .withNetworkAliases("application")
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withStartupTimeout(Duration.ofMillis(30000))
                        .withWorkingDirectory("/temp");

        if (DockerImageNames.isJava6(dockerImageName)) {
            applicationContainer.withCommand("/bin/sh application_java6.sh");
        }

        return applicationContainer;
    }

    /**
     * Method to create an HttpClient
     *
     * @param genericContainer genericContainer
     * @param baseUrl baseUrl
     * @return the return value
     */
    protected static HttpClient createHttpClient(GenericContainer<?> genericContainer, String baseUrl) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(8888));
    }

    /**
     * Method to destroy a GenericContainer (null safe)
     *
     * @param genericContainer genericContainer
     */
    protected static void destroy(GenericContainer<?> genericContainer) {
        if (genericContainer != null) {
            genericContainer.close();
        }
    }

    /**
     * Method to destroy a Network (null safe)
     *
     * @param network network
     */
    protected static void destroy(Network network) {
        if (network != null) {
            network.close();
        }
    }

    /**
     * Method to derive the build name based on the mode and Java version
     *
     * @param mode mode
     * @param isJava6 isJava6
     * @return the return value
     */
    public static String deriveBuildName(Mode mode, boolean isJava6) {
        String buildInfoName;
        if (mode == Mode.JavaAgent) {
            buildInfoName = "jmx_prometheus_javaagent";
        } else {
            buildInfoName = "jmx_prometheus_httpserver";
        }

        if (isJava6) {
            buildInfoName += "_java6";
        }

        return buildInfoName;
    }
}
