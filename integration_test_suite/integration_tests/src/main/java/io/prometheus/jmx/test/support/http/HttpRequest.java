/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.support.http;

import java.util.function.Consumer;
import okhttp3.Headers;

/** Class to implement HttpRequest */
public class HttpRequest {

    protected Headers.Builder headersBuilder;
    protected String path;
    protected HttpCredentials httpCredentials;

    /** Constructor */
    public HttpRequest() {
        headersBuilder = new Headers.Builder();
    }

    /**
     * Constructor
     *
     * @param path path
     */
    public HttpRequest(String path) {
        headersBuilder = new Headers.Builder();
        path(path);
    }

    /**
     * Method to set the path
     *
     * @param path path
     * @return this
     */
    public HttpRequest path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Method to add Headers
     *
     * @param headers headers
     * @return this
     */
    public HttpRequest headers(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    public HttpRequest header(String name, String value) {
        headersBuilder.add(name, value);
        return this;
    }

    /**
     * Method to set the Credentials
     *
     * @param httpCredentials credentials
     * @return this
     */
    public HttpRequest credentials(HttpCredentials httpCredentials) {
        this.httpCredentials = httpCredentials;
        return this;
    }

    /**
     * Method to send the request
     *
     * @param httpClient httpClient
     * @return the response
     */
    public HttpResponse send(HttpClient httpClient) {
        HttpResponse httpResponse;

        try {
            okhttp3.Request.Builder requestBuilder = httpClient.createRequest(path);

            requestBuilder.headers(headersBuilder.build());

            if (httpCredentials != null) {
                httpCredentials.apply(requestBuilder);
            }

            try (okhttp3.Response okhttp3Response = httpClient.execute(requestBuilder)) {
                httpResponse = new HttpResponse(okhttp3Response);
            }

            return httpResponse;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Method to send the request
     *
     * @param httpClient httpClient
     * @param httpResponseConsumer httpResponseConsumer
     */
    public void send(HttpClient httpClient, Consumer<HttpResponse> httpResponseConsumer) {
        httpResponseConsumer.accept(send(httpClient));
    }
}
