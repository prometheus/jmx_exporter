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

import static java.lang.String.format;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an immutable HTTP request with method, URL, headers, and body, built via the Builder pattern.
 */
public class HttpRequest {

    /**
     * Defines the supported HTTP methods for integration test requests.
     */
    public enum Method {

        /**
         * The HTTP {@code GET} method for retrieving resources.
         */
        GET,

        /**
         * The HTTP {@code POST} method for submitting data.
         */
        POST,

        /**
         * The HTTP {@code PUT} method for replacing resources.
         */
        PUT
    }

    private final String url;
    private final Method method;
    private final Map<String, List<String>> headers;
    private final String body;

    /**
     * Creates an {@link HttpRequest} from the builder state.
     *
     * @param builder the builder containing the request configuration
     */
    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    /**
     * Returns the request URL.
     *
     * @return the request URL
     */
    public String url() {
        return url;
    }

    /**
     * Returns the HTTP method.
     *
     * @return the HTTP method
     */
    public Method method() {
        return method;
    }

    /**
     * Returns the request headers as a map of header names to their values.
     *
     * @return the request headers, where keys are uppercased header names
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns the request body, or {@code null} if no body is set.
     *
     * @return the request body, or {@code null} if unset
     */
    public String body() {
        return body;
    }

    /**
     * Creates a new {@link Builder} for constructing an {@link HttpRequest}.
     *
     * @return a new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds {@link HttpRequest} instances with a fluent API.
     */
    public static class Builder {

        private String url;
        private Method method = Method.GET;
        private final Map<String, List<String>> headers = new HashMap<>();
        private String body;

        /**
         * Private constructor to enforce use of {@link HttpRequest#builder()}.
         */
        private Builder() {
            // INTENTIONALLY BLANK
        }

        /**
         * Sets the request URL.
         *
         * @param url the target URL for the request
         * @return this builder for method chaining
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the HTTP method, defaulting to {@code GET} if not called.
         *
         * @param method the HTTP method to use
         * @return this builder for method chaining
         */
        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        /**
         * Adds a single header name-value pair. Header names are stored in uppercase.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder for method chaining
         */
        public Builder header(String name, String value) {
            headers.computeIfAbsent(name.toUpperCase(Locale.US), k -> new ArrayList<>())
                    .add(value);
            return this;
        }

        /**
         * Adds multiple values for a single header name.
         *
         * @param name the header name
         * @param values the collection of header values to add
         * @return this builder for method chaining
         */
        public Builder headers(String name, Collection<String> values) {
            for (String value : values) {
                header(name, value);
            }
            return this;
        }

        /**
         * Adds all headers from a map of header names to their value collections.
         *
         * @param headers the map of header names to their values
         * @return this builder for method chaining
         */
        public Builder headers(Map<String, Collection<String>> headers) {
            for (Map.Entry<String, ? extends Collection<String>> entry : headers.entrySet()) {
                for (String value : entry.getValue()) {
                    header(entry.getKey(), value);
                }
            }
            return this;
        }

        /**
         * Sets the HTTP Authorization header with Basic authentication credentials.
         *
         * @param principal the username for Basic authentication
         * @param credential the password for Basic authentication
         * @return this builder for method chaining
         */
        public Builder basicAuthentication(String principal, String credential) {
            return header(
                    "AUTHORIZATION",
                    format(
                            "Basic %s",
                            Base64.getEncoder()
                                    .encodeToString((principal + ":" + credential).getBytes(StandardCharsets.UTF_8))));
        }

        /**
         * Sets the request body.
         *
         * @param body the request body content
         * @return this builder for method chaining
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Builds an immutable {@link HttpRequest} from the configured state.
         *
         * @return a new {@link HttpRequest} instance
         */
        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
