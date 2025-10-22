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

/** Class to implement HttpRequest */
public class HttpRequest {

    /** Enum to represent HTTP methods */
    public enum Method {
        /** GET method */
        GET,
        /** POST method */
        POST,
        /** PUT method */
        PUT
    }

    private final String url;
    private final Method method;
    private final Map<String, List<String>> headers;
    private final String body;

    /**
     * Constructor
     *
     * @param builder builder
     */
    private HttpRequest(Builder builder) {
        this.url = builder.url;
        this.method = builder.method;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    /**
     * Get the URL
     *
     * @return the URL
     */
    public String url() {
        return url;
    }

    /**
     * Get the Method
     *
     * @return the Method
     */
    public Method method() {
        return method;
    }

    /**
     * Get the Map of headers
     *
     * @return the Map of headers
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Get the body
     *
     * @return the body
     */
    public String body() {
        return body;
    }

    /**
     * Get a Builder
     *
     * @return a Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Class to implement Builder */
    public static class Builder {

        private String url;
        private Method method = Method.GET;
        private final Map<String, List<String>> headers = new HashMap<>();
        private String body;

        /** Constructor */
        private Builder() {
            // INTENTIONALLY BLANK
        }

        /**
         * Set the URL
         *
         * @param url url
         * @return the Builder
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Set the Method
         *
         * @param method method
         * @return the Builder
         */
        public Builder method(Method method) {
            this.method = method;
            return this;
        }

        /**
         * Set a header
         *
         * @param name name
         * @param value value
         * @return the Builder
         */
        public Builder header(String name, String value) {
            headers.computeIfAbsent(name.toUpperCase(Locale.US), k -> new ArrayList<>()).add(value);
            return this;
        }

        /**
         * Set a Collection of headers
         *
         * @param name name
         * @param values values
         * @return the Builder
         */
        public Builder headers(String name, Collection<String> values) {
            for (String value : values) {
                header(name, value);
            }
            return this;
        }

        /**
         * Set a Map of headers
         *
         * @param headers headers
         * @return the Builder
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
         * Set the BASIC authentication header
         *
         * @param principal principal
         * @param credential credential
         * @return the Builder
         */
        public Builder basicAuthentication(String principal, String credential) {
            return header(
                    "AUTHORIZATION",
                    format(
                            "Basic %s",
                            Base64.getEncoder()
                                    .encodeToString(
                                            (principal + ":" + credential)
                                                    .getBytes(StandardCharsets.UTF_8))));
        }

        /**
         * Set the body
         *
         * @param body body
         * @return the Builder
         */
        public Builder body(String body) {
            this.body = body;
            return this;
        }

        /**
         * Build the HttpRequest
         *
         * @return an HttpRequest
         */
        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
