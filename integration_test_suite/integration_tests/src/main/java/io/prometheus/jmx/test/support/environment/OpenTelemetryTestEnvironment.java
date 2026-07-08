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
import io.prometheus.jmx.test.support.PrometheusDockerImages;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.altcontainers.api.Network;
import org.paramixel.api.support.Retry;

/**
 * Composite test environment combining a {@link PrometheusTestEnvironment} and a
 * {@link JmxExporterTestEnvironment} for OpenTelemetry integration testing.
 *
 * <p>The environment initializes both sub-environments on a shared Docker network passed
 * via {@link #initialize(Network)}. Resource mapping is delegated to the composed
 * Prometheus and JMX exporter environments. The caller is responsible for network
 * creation and teardown; this class only manages the sub-environment lifecycles.
 */
public class OpenTelemetryTestEnvironment implements AutoCloseable {

    private final String id;
    private final Class<?> testClass;
    private final PrometheusTestEnvironment prometheusTestEnvironment;
    private final JmxExporterTestEnvironment jmxExporterTestEnvironment;

    private String prometheusReadyUsername;
    private String prometheusReadyPassword;
    private String exporterReadyUsername;
    private String exporterReadyPassword;
    private Network network;

    /**
     * Creates an OpenTelemetry test environment combining a Prometheus and JMX exporter environment.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @param prometheusTestEnvironment the Prometheus test environment for scrape target verification
     * @param jmxExporterTestEnvironment the JMX exporter test environment providing the exporter
     * @throws NullPointerException if any argument is {@code null}
     */
    public OpenTelemetryTestEnvironment(
            Class<?> testClass,
            PrometheusTestEnvironment prometheusTestEnvironment,
            JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        this.id = UUID.randomUUID().toString();
        this.testClass = Objects.requireNonNull(testClass);
        this.prometheusTestEnvironment = Objects.requireNonNull(prometheusTestEnvironment);
        this.jmxExporterTestEnvironment = Objects.requireNonNull(jmxExporterTestEnvironment);
    }

    /**
     * Returns a human-readable name identifying this environment, its mode, and Docker images.
     *
     * @return the display name of this environment
     */
    public String name() {
        return jmxExporterTestEnvironment.getJmxExporterMode()
                + " ("
                + jmxExporterTestEnvironment.getJavaDockerImage()
                + ","
                + prometheusTestEnvironment.getPrometheusDockerImage()
                + ")";
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
     * Returns the Prometheus test environment used for scrape target verification.
     *
     * @return the Prometheus test environment
     */
    public PrometheusTestEnvironment prometheusTestEnvironment() {
        return prometheusTestEnvironment;
    }

    /**
     * Returns the JMX exporter test environment providing the exporter instance.
     *
     * @return the JMX exporter test environment
     */
    public JmxExporterTestEnvironment exporterTestEnvironment() {
        return jmxExporterTestEnvironment;
    }

    /**
     * Configures basic authentication credentials to use when waiting for Prometheus to become ready.
     *
     * @param username the basic authentication username; must not be {@code null}
     * @param password the basic authentication password; must not be {@code null}
     * @return this environment instance for method chaining
     */
    public OpenTelemetryTestEnvironment withPrometheusReadyBasicAuthentication(String username, String password) {
        this.prometheusReadyUsername = username;
        this.prometheusReadyPassword = password;
        return this;
    }

    /**
     * Configures basic authentication credentials to use when waiting for the JMX exporter
     * to become ready.
     *
     * @param username the basic authentication username; must not be {@code null}
     * @param password the basic authentication password; must not be {@code null}
     * @return this environment instance for method chaining
     */
    public OpenTelemetryTestEnvironment withExporterReadyBasicAuthentication(String username, String password) {
        this.exporterReadyUsername = username;
        this.exporterReadyPassword = password;
        return this;
    }

    /**
     * Initializes this environment on the specified Docker network, starting the Prometheus
     * and JMX exporter sub-environments, and waiting for Prometheus to become ready.
     *
     * @param network the shared Docker network; must not be {@code null}
     * @throws Throwable if initialization of either sub-environment fails; suppressed
     *                   exceptions are attached for partial cleanup failures
     */
    public void initialize(Network network) throws Throwable {
        this.network = Objects.requireNonNull(network);

        try {
            prometheusTestEnvironment.initialize(network);
            if (prometheusReadyUsername != null && prometheusReadyPassword != null) {
                prometheusTestEnvironment.waitForReady(prometheusReadyUsername, prometheusReadyPassword);
            } else {
                prometheusTestEnvironment.waitForReady();
            }
            jmxExporterTestEnvironment.initialize(network);
            waitForExporterReady();
        } catch (Throwable t) {
            Throwable failure = t;

            try {
                jmxExporterTestEnvironment.close();
            } catch (Throwable closeFailure) {
                failure.addSuppressed(closeFailure);
            }

            try {
                prometheusTestEnvironment.close();
            } catch (Throwable closeFailure) {
                failure.addSuppressed(closeFailure);
            }

            throw failure;
        }
    }

    /**
     * Waits for the JMX exporter's {@code /healthy} endpoint to return HTTP 200.
     *
     * <p>This ensures the exporter is fully ready before tests query Prometheus for metrics.
     *
     * @throws EnvironmentException if the exporter does not become ready within the retry policy
     */
    private void waitForExporterReady() {
        String url = jmxExporterTestEnvironment.getUrl(JmxExporterPath.HEALTHY);

        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(30)))
                .retryOn(t -> t instanceof Exception)
                .run(() -> {
                    HttpRequest.Builder httpRequestBuilder =
                            HttpRequest.builder().url(url);
                    if (exporterReadyUsername != null && exporterReadyPassword != null) {
                        httpRequestBuilder.basicAuthentication(exporterReadyUsername, exporterReadyPassword);
                    }

                    HttpResponse httpResponse = HttpClient.sendRequest(httpRequestBuilder.build());

                    if (httpResponse.statusCode() == 200) {
                        return;
                    }

                    throw new EnvironmentException("Exporter not ready, status: " + httpResponse.statusCode());
                });

        if (!result.isSuccessful()) {
            throw new EnvironmentException("Exporter not ready after retry");
        }
    }

    /**
     * Returns whether both the Prometheus and JMX exporter environments are currently running.
     *
     * @return {@code true} if both sub-environments are running; {@code false} otherwise
     */
    public boolean isRunning() {
        return prometheusTestEnvironment.isRunning() && jmxExporterTestEnvironment.isRunning();
    }

    @Override
    public void close() {
        RuntimeException firstException = null;
        try {
            jmxExporterTestEnvironment.close();
        } catch (RuntimeException e) {
            firstException = e;
        }
        try {
            prometheusTestEnvironment.close();
        } catch (RuntimeException e) {
            if (firstException == null) {
                firstException = e;
            } else {
                firstException.addSuppressed(e);
            }
        }
        if (firstException != null) {
            throw firstException;
        }
    }

    /**
     * Returns the Docker network shared by the sub-environments.
     *
     * @return the Docker network; may be {@code null} if not yet initialized
     */
    public Network getNetwork() {
        return network;
    }

    /**
     * Creates one test environment per combination of configured Prometheus Docker image,
     * Java Docker image, and JMX exporter mode for the given test class.
     *
     * @param testClass the test class used to resolve classpath resource mappings
     * @return an unmodifiable list of test environments covering all image and mode combinations
     */
    public static List<OpenTelemetryTestEnvironment> createTestEnvironments(Class<?> testClass) {
        return PrometheusDockerImages.names().stream()
                .flatMap(prometheusDockerImage -> JavaDockerImages.names().stream()
                        .flatMap(javaDockerImage -> Arrays.stream(JmxExporterMode.values())
                                .map(jmxExporterMode -> new OpenTelemetryTestEnvironment(
                                        testClass,
                                        new PrometheusTestEnvironment(testClass, prometheusDockerImage),
                                        new JmxExporterTestEnvironment(testClass, javaDockerImage, jmxExporterMode)))))
                .toList();
    }
}
