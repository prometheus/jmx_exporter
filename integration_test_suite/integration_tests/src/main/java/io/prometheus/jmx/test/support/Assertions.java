/*
 * Copyright (C) 2024-present The Prometheus jmx_exporter Authors
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

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseBody;
import io.prometheus.jmx.test.support.metrics.MetricsContentType;

/** Class to implement Assertions */
public class Assertions {

    /** Constructor */
    private Assertions() {
        // INTENTIONALLY BLANK
    }

    /**
     * Assert a health response
     *
     * @param httpResponse httpResponse
     */
    public static void assertHealthyResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0))
                .contains("text/plain");
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().string()).isNotBlank();
        assertThat(httpResponse.body().string()).contains("Exporter is healthy.");
    }

    /**
     * Assert common metrics response
     *
     * @param httpResponse httpResponse
     * @param metricsContentType metricsContentType
     */
    public static void assertCommonMetricsResponse(
            HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody body = httpResponse.body();
            if (body != null) {
                throw new AssertionError(
                        format(
                                "Expected statusCode [%d] but was [%d] body [%s]",
                                200, statusCode, body.string()));
            } else {
                throw new AssertionError(
                        format("Expected statusCode [%d] but was [%d] no body", 200, statusCode));
            }
        }

        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0))
                .isEqualTo(metricsContentType.toString());
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }
}
