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

package io.prometheus.jmx.test.opentelemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.paramixel.api.Context.withInstance;

import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.OpenTelemetryTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.paramixel.api.Paramixel;
import org.paramixel.api.Runner;
import org.paramixel.api.action.Action;
import org.paramixel.api.action.Each;
import org.paramixel.api.action.Instance;
import org.paramixel.api.action.Scope;
import org.paramixel.api.action.Sequence;
import org.paramixel.api.action.Step;
import org.paramixel.api.support.Retry;

public class BasicAuthenticationTest {

    private static final String VALID_USER = "Prometheus";
    private static final String VALUE_PASSWORD = "secret";

    private final OpenTelemetryTestEnvironment environment;

    private BasicAuthenticationTest(OpenTelemetryTestEnvironment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) throws Throwable {
        Runner.defaultRunner().runAndExit(factory());
    }

    @Paramixel.Factory
    public static Action factory() throws Throwable {
        return Each.parallel(
                        BasicAuthenticationTest.class.getName(),
                        OpenTelemetryTestEnvironment.createTestEnvironments(BasicAuthenticationTest.class),
                        env -> {
                            env.withPrometheusReadyBasicAuthentication(VALID_USER, VALUE_PASSWORD);
                            return Instance.builder(env.name(), () -> new BasicAuthenticationTest(env))
                                    .body(Scope.builder("scenario")
                                            .before(Step.of(
                                                    "setUp()",
                                                    withInstance(
                                                            BasicAuthenticationTest.class,
                                                            BasicAuthenticationTest::setUp)))
                                            .body(Sequence.builder("tests")
                                                    .child(Step.of(
                                                            "testPrometheusHasMetrics()",
                                                            withInstance(
                                                                    BasicAuthenticationTest.class,
                                                                    BasicAuthenticationTest
                                                                            ::testPrometheusHasMetrics))))
                                            .after(Step.of(
                                                    "tearDown()",
                                                    withInstance(
                                                            BasicAuthenticationTest.class,
                                                            BasicAuthenticationTest::tearDown))))
                                    .build();
                        })
                .build();
    }

    public void setUp() throws Throwable {
        environment.initialize();
    }

    public void testPrometheusHasMetrics() throws Throwable {
        boolean isJmxExporterModeJavaStandalone =
                environment.exporterTestEnvironment().getJmxExporterMode() == JmxExporterMode.Standalone;

        boolean isKonaJdk8 =
                environment.exporterTestEnvironment().getJavaDockerImage().startsWith("konajdk/konajdk:8");

        for (String metricName : ExpectedMetricsNames.getMetricsNames().stream()
                .filter(name ->
                        !isJmxExporterModeJavaStandalone || (!name.startsWith("jvm_") && !name.startsWith("process_")))
                .filter(name -> !isKonaJdk8 || !name.equals("jvm_memory_pool_allocated_bytes_total"))
                .toList()) {
            Double value = getPrometheusMetric(metricName);

            assertThat(value).as("metricName [%s]", metricName).isNotNull();
            assertThat(value).as("metricName [%s]", metricName).isEqualTo(1);
        }
    }

    public void tearDown() throws Throwable {
        environment.close();
    }

    private Double getPrometheusMetric(String metricName) throws Throwable {
        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), Duration.ofSeconds(5)))
                .retryOn(t -> t instanceof RuntimeException)
                .run(() -> {
                    HttpResponse httpResponse = sendPrometheusQuery(metricName);

                    assertThat(httpResponse.statusCode()).isEqualTo(200);
                    assertThat(httpResponse.body()).isNotNull();
                    assertThat(httpResponse.body().string()).isNotNull();

                    if (httpResponse.body().string().contains(metricName)) {
                        return;
                    }

                    throw new RuntimeException("metric not found yet: " + metricName);
                });

        return result.isSuccessful() ? 1.0 : null;
    }

    private HttpResponse sendPrometheusQuery(String query) throws IOException {
        return sendRequest("/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    private HttpResponse sendRequest(String path) throws IOException {
        return HttpClient.sendRequest(HttpRequest.builder()
                .url(environment.prometheusTestEnvironment().getPrometheusUrl(path))
                .basicAuthentication(VALID_USER, VALUE_PASSWORD)
                .build());
    }
}
