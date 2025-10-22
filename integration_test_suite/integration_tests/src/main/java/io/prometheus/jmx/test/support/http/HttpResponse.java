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

/** Class to implement HttpResponse */
public class HttpResponse {

    private final int statusCode;
    private final String statusMessage;
    private final HttpResponseBody body;
    private final Map<String, List<String>> headers;

    /**
     * Constructor
     *
     * @param statusCode statusCode
     * @param statusMessage statusMessage
     * @param headers headers
     * @param body body
     */
    public HttpResponse(
            int statusCode, String statusMessage, Map<String, List<String>> headers, byte[] body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.headers = headers;
        this.body = new HttpResponseBody(body);
    }

    /**
     * Get the status code
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Get the status message
     *
     * @return the status message
     */
    public String statusMessage() {
        return statusMessage;
    }

    /**
     * Get the Map of headers
     *
     * @return a Map of headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Get a List of header values
     *
     * @param name name
     * @return a List of header values
     */
    public List<String> header(String name) {
        return headers.get(name.toUpperCase(Locale.US));
    }

    /**
     * Get the body
     *
     * @return the body
     */
    public HttpResponseBody body() {
        return body;
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
}
