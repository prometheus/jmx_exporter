package io.prometheus.jmx.test.httpserver;

import com.github.dockerjava.api.model.Ulimit;
import io.prometheus.jmx.test.DockerImageNameParameters;
import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.TestUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class Base_IT {

    public static Network createNetwork() {
        Network network = Network.newNetwork();

        // Get the id to force the network creation
        network.getId();

        return network;
    }

    public GenericContainer<?> createApplicationContainer(
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

    public GenericContainer<?> createExporterContainer(Object testInstance,
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

    public HttpClient createHttpClient(GenericContainer<?> genericContainer, String baseUrl) {
        return new HttpClient(baseUrl + ":" + genericContainer.getMappedPort(8888));
    }

    public void destroy(GenericContainer<?> genericContainer) {
        TestUtils.close(genericContainer);
    }

    public static void destroy(Network network) {
        TestUtils.close(network);
    }

}
