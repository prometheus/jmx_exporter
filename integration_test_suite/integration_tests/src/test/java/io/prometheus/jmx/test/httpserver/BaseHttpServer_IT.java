/*
 * Copyright 2022-2023 Douglas Hoard
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

package io.prometheus.jmx.test.httpserver;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.DockerImageNameParameters;
import io.prometheus.jmx.test.HttpClient;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class BaseHttpServer_IT {

    /**
     * Method to create a Network
     *
     * @return
     */
    public static Network createNetwork() {
        Network network = Network.newNetwork();

        // Get the id to force the network creation
        network.getId();

        return network;
    }

    /**
     * Method to create an application container
     *
     * @param testInstance
     * @param dockerImageName
     * @param network
     * @return
     */
    public static GenericContainer<?> createApplicationContainer(
            Object testInstance,
            String dockerImageName,
            Network network) {
        GenericContainer<?> applicationContainer =
                new GenericContainer<>(dockerImageName)
                        .waitingFor(Wait.forLogMessage(".*Running.*", 1))
                        .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                        .withClasspathResourceMapping(testInstance.getClass().getName().replace(".", "/"), "/temp", BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(c -> c.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                        .withCommand("/bin/sh application.sh")
                        .withExposedPorts(9999)
                        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                        .withNetwork(network)
                        .withNetworkAliases("application")
                        .withStartupTimeout(Duration.ofMillis(30000))
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withWorkingDirectory("/temp");

        if (DockerImageNameParameters.isJava6(dockerImageName)) {
            applicationContainer.withCommand("/bin/sh application_java6.sh");
        }

        return applicationContainer;
    }

    /**
     * Method to create an exporter container
     *
     * @param testInstance
     * @param dockerImageName
     * @param network
     * @return
     */
    public static GenericContainer<?> createExporterContainer(Object testInstance,
                                                       String dockerImageName,
                                                       Network network) {
        // Exporter container
        GenericContainer<?> exporterContainer =
                new GenericContainer<>(dockerImageName)
                        .waitingFor(Wait.forHttp("/"))
                        .withClasspathResourceMapping("common", "/temp", BindMode.READ_ONLY)
                        .withClasspathResourceMapping(testInstance.getClass().getName().replace(".", "/"), "/temp", BindMode.READ_ONLY)
                        .withCreateContainerCmdModifier(c -> c.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)}))
                        .withCommand("/bin/sh exporter.sh")
                        .withExposedPorts(8888)
                        .withLogConsumer(outputFrame -> System.out.print(outputFrame.getUtf8String()))
                        .withNetwork(network)
                        .withNetworkAliases("exporter")
                        .withStartupCheckStrategy(new IsRunningStartupCheckStrategy())
                        .withWorkingDirectory("/temp");

        if (DockerImageNameParameters.isJava6(dockerImageName)) {
            exporterContainer.withCommand("/bin/sh exporter_java6.sh");
        }

        return exporterContainer;
    }

    /**
     * Method to create an HttpClient
     *
     * @param genericContainer
     * @param baseUrl
     * @return
     */
    public static HttpClient createHttpClient(GenericContainer<?> genericContainer, String baseUrl) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(8888));
    }

    /**
     * Method to destroy a GenericContainer
     *
     * @param genericContainer
     */
    public static void destroy(GenericContainer<?> genericContainer) {
        if (genericContainer != null) {
            genericContainer.close();
        }
    }

    /**
     * Method to destroy a Network
     *
     * @param network
     */
    public static void destroy(Network network) {
        if (network != null) {
            network.close();
        }
    }
}
