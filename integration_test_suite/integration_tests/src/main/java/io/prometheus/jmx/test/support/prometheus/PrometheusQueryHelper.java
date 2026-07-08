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

package io.prometheus.jmx.test.support.prometheus;

import static java.lang.String.format;

import io.prometheus.jmx.test.support.environment.PrometheusTestEnvironment;
import io.prometheus.jmx.test.support.http.HttpClient;
import io.prometheus.jmx.test.support.http.HttpRequest;
import io.prometheus.jmx.test.support.http.HttpResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import org.paramixel.api.support.Retry;

/**
 * Shared helper for querying Prometheus metrics in OpenTelemetry integration tests.
 *
 * <p>Replaces the per-test-class {@code getPrometheusMetric()}/{@code sendPrometheusQuery()} code
 * with a single implementation that:
 *
 * <ul>
 *   <li>Queries {@code /api/v1/query?query=<metric>} via the {@link PrometheusTestEnvironment}.
 *   <li>Requires HTTP 200.
 *   <li>Parses the Prometheus JSON response to check for {@code "status":"success"} and a
 *       non-empty {@code "result"} array.
 *   <li>Waits up to 60 seconds with exponential backoff for OTLP ingestion.
 *   <li>Includes the last response body in the timeout failure message for diagnostics.
 * </ul>
 */
public class PrometheusQueryHelper {

    private static final Duration MAX_WAIT = Duration.ofSeconds(60);

    private final PrometheusTestEnvironment prometheusTestEnvironment;
    private final String username;
    private final String password;

    private PrometheusQueryHelper(Builder builder) {
        this.prometheusTestEnvironment = Objects.requireNonNull(builder.prometheusTestEnvironment);
        this.username = builder.username;
        this.password = builder.password;
    }

    /**
     * Creates a new {@link Builder} for constructing a {@link PrometheusQueryHelper}.
     *
     * @param prometheusTestEnvironment the Prometheus test environment; must not be {@code null}
     * @return a new builder instance
     */
    public static Builder builder(PrometheusTestEnvironment prometheusTestEnvironment) {
        return new Builder(prometheusTestEnvironment);
    }

    /**
     * Waits until the specified metric is queryable from Prometheus and returns its value.
     *
     * <p>Polls the Prometheus query API with exponential backoff (100ms initial, 60s max). Parses
     * the JSON response to verify {@code "status":"success"} and a non-empty {@code "result"}
     * array. On timeout, the failure message includes the last Prometheus response body for
     * diagnostics.
     *
     * @param metricName the metric name to query
     * @return {@code 1.0} if the metric was found within the timeout; {@code null} otherwise
     * @throws Throwable if an unexpected error occurs during retry execution
     */
    public Double waitForMetric(String metricName) throws Throwable {
        final String[] lastResponseBody = {null};

        Retry.Result result = Retry.of(Retry.Policy.exponential(Duration.ofMillis(100), MAX_WAIT))
                .retryOn(t -> t instanceof RuntimeException)
                .run(() -> {
                    HttpResponse httpResponse = sendPrometheusQuery(metricName);

                    if (httpResponse.statusCode() != 200) {
                        throw new RuntimeException(format(
                                "Prometheus query for [%s] returned status %d", metricName, httpResponse.statusCode()));
                    }

                    if (httpResponse.body() == null) {
                        throw new RuntimeException(format("Prometheus query for [%s] returned null body", metricName));
                    }

                    String body = httpResponse.body().string();
                    lastResponseBody[0] = body;

                    if (body == null) {
                        throw new RuntimeException(
                                format("Prometheus query for [%s] returned null body string", metricName));
                    }

                    if (!isPrometheusSuccessWithResult(body)) {
                        throw new RuntimeException(format(
                                "Prometheus query for [%s] did not return a successful result. " + "Response body: %s",
                                metricName, body));
                    }
                });

        if (!result.isSuccessful()) {
            throw new RuntimeException(format(
                    "Metric [%s] not found in Prometheus within %d seconds. Last response body: %s",
                    metricName, MAX_WAIT.getSeconds(), lastResponseBody[0]));
        }

        return 1.0;
    }

    private HttpResponse sendPrometheusQuery(String query) throws IOException {
        String path = "/api/v1/query?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = prometheusTestEnvironment.getPrometheusUrl(path);

        HttpRequest.Builder requestBuilder = HttpRequest.builder().url(url);
        if (username != null && password != null) {
            requestBuilder.basicAuthentication(username, password);
        }

        return HttpClient.sendRequest(requestBuilder.build());
    }

    /**
     * Parses the Prometheus JSON response to check for a successful query with results.
     *
     * <p>Looks for {@code "status":"success"} and a non-empty {@code "result"} array using string
     * parsing. This avoids adding a JSON library dependency while being more reliable than a raw
     * substring check on the metric name.
     *
     * @param body the Prometheus API response body
     * @return {@code true} if the response indicates success with a non-empty result
     */
    private static boolean isPrometheusSuccessWithResult(String body) {
        if (!body.contains("\"status\":\"success\"")) {
            return false;
        }

        int resultIndex = body.indexOf("\"result\":");
        if (resultIndex < 0) {
            return false;
        }

        String afterResult =
                body.substring(resultIndex + "\"result\":".length()).trim();
        // Check that result is not an empty array "[]"
        return !afterResult.startsWith("[]");
    }

    /**
     * Builder for {@link PrometheusQueryHelper}.
     */
    public static class Builder {

        private final PrometheusTestEnvironment prometheusTestEnvironment;
        private String username;
        private String password;

        private Builder(PrometheusTestEnvironment prometheusTestEnvironment) {
            this.prometheusTestEnvironment = prometheusTestEnvironment;
        }

        /**
         * Sets basic authentication credentials for Prometheus queries.
         *
         * @param username the basic auth username; must not be {@code null}
         * @param password the basic auth password; must not be {@code null}
         * @return this builder for method chaining
         */
        public Builder basicAuthentication(String username, String password) {
            this.username = Objects.requireNonNull(username);
            this.password = Objects.requireNonNull(password);
            return this;
        }

        /**
         * Builds the {@link PrometheusQueryHelper}.
         *
         * @return a new helper instance
         */
        public PrometheusQueryHelper build() {
            return new PrometheusQueryHelper(this);
        }
    }
}
