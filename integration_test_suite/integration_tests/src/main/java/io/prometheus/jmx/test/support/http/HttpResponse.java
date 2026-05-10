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

package io.prometheus.jmx.test.support.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an immutable HTTP response with status code, headers, and body.
 */
public class HttpResponse {

    private final int statusCode;
    private final String statusMessage;
    private final HttpResponseBody body;
    private final Map<String, List<String>> headers;

    /**
     * Creates an HTTP response with the specified status, headers, and body.
     *
     * @param statusCode the HTTP status code
     * @param statusMessage the HTTP status message
     * @param headers the response headers as a map of uppercased header names to their values
     * @param body the raw bytes of the response body
     */
    public HttpResponse(int statusCode, String statusMessage, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.body = new HttpResponseBody(body);
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP status message.
     *
     * @return the HTTP status message
     */
    public String statusMessage() {
        return statusMessage;
    }

    /**
     * Returns the response headers as a map of uppercased header names to their values.
     *
     * @return the response headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns the values for the specified header name using case-insensitive lookup.
     *
     * @param name the header name to look up (case-insensitive)
     * @return the header values, or {@code null} if the header is not present
     */
    public List<String> header(String name) {
        return headers.get(name.toUpperCase(Locale.US));
    }

    /**
     * Returns the response body.
     *
     * @return the response body
     */
    public HttpResponseBody body() {
        return body;
    }

    /**
     * Asserts that the response is a healthy jmx_exporter response with status 200
     * and a text/plain content type containing the expected health message.
     *
     * @param httpResponse the HTTP response to validate
     * @throws AssertionError if the response does not meet health criteria
     */
    public static void assertHealthyResponse(HttpResponse httpResponse) {
        assertThat(httpResponse).isNotNull();
        assertThat(httpResponse.statusCode()).isEqualTo(200);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0)).contains("text/plain");
        assertThat(httpResponse.body()).isNotNull();
        String body = httpResponse.body().string();
        assertThat(body).isNotBlank();
        assertThat(body).contains("Exporter is healthy.");
    }
}
