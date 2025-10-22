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
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.throttle.ExponentialBackoffThrottle;
import io.prometheus.jmx.test.support.throttle.Throttle;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.verifyica.api.Argument;

/** Class to implement PrometheusTestEnvironment */
public class PrometheusTestEnvironment implements Argument<PrometheusTestEnvironment> {

    private static final String BASE_URL = "http://localhost";

    private final String id;
    private final String prometheusDockerImage;

    private Class<?> testClass;
    private Network network;
    private String baseUrl;
    private GenericContainer<?> prometheusContainer;

    /**
     * Constructor
     *
     * @param prometheusDockerImage prometheusDockerImage
     */
    public PrometheusTestEnvironment(String prometheusDockerImage) {
        this.id = UUID.randomUUID().toString();
        this.prometheusDockerImage = prometheusDockerImage;
        this.baseUrl = BASE_URL;
    }

    @Override
    public String getName() {
        return prometheusDockerImage;
    }

    @Override
    public PrometheusTestEnvironment getPayload() {
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
    public PrometheusTestEnvironment setBaseUrl(String baseUrl) {
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
     * Method to initialize the test environment
     *
     * @param network network
     */
    public void initialize(Class<?> testClass, Network network) {
        this.testClass = testClass;
        this.network = network;

        prometheusContainer = createPrometheusContainer();
        prometheusContainer.start();

        if (!prometheusContainer.isRunning()) {
            throw new IllegalStateException("Prometheus container is not running");
        }
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

    /** Method to wait for the Prometheus container to be ready */
    public void waitForReady() {
        waitForReady(null, null);
    }

    /**
     * Method to wait for the Prometheus container to be ready with credentials
     *
     * @param username username
     * @param password password
     */
    public void waitForReady(String username, String password) {
        long startMilliseconds = System.currentTimeMillis();
        int maximumRetryCount = 10;
        Throttle throttle = new ExponentialBackoffThrottle(100, 5000);

        for (int i = 0; i < maximumRetryCount; i++) {
            try {
                HttpRequest.Builder httpRequestBuilder =
                        HttpRequest.builder().url(getPrometheusBaseUrl() + "/-/ready");

                if (username != null && password != null) {
                    httpRequestBuilder.basicAuthentication(username, password);
                }

                HttpResponse httpResponse = HttpClient.sendRequest(httpRequestBuilder.build());

                if (httpResponse.statusCode() == 200) {
                    return;
                } else {
                    throttle.throttle();
                }
            } catch (Exception e) {
                throttle.throttle();
            }
        }

        throw new EnvironmentException(
                format(
                        "Prometheus [%s] not ready have after %d milliseconds",
                        prometheusContainer.getDockerImageName(),
                        System.currentTimeMillis() - startMilliseconds));
    }

    /** Method to destroy the test environment */
    public void destroy() {
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

        String webYml = "/" + testClass.getName().replace(".", "/") + "/web.yaml";

        boolean hasWebYaml = ResourceSupport.exists(webYml);

        if (hasWebYaml) {
            commands.add("--web.config.file=/etc/prometheus/web.yaml");
        }

        GenericContainer<?> genericContainer =
                new GenericContainer<>(prometheusDockerImage)
                        .withClasspathResourceMapping(
                                testClass.getName().replace(".", "/") + "/prometheus.yaml",
                                "/etc/prometheus/prometheus.yaml",
                                BindMode.READ_ONLY)
                        .withWorkingDirectory("/prometheus")
                        .withCommand(commands.toArray(new String[0]))
                        .withCreateContainerCmdModifier(ContainerCmdModifier.getInstance())
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
                        .waitingFor(
                                Wait.forLogMessage(
                                        ".*Server is ready to receive web requests.*", 1))
                        .withStartupTimeout(Duration.ofMillis(60000));

        if (hasWebYaml) {
            genericContainer.withClasspathResourceMapping(
                    webYml, "/etc/prometheus/web.yaml", BindMode.READ_ONLY);
        }

        return genericContainer;
    }
}
