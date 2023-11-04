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

package io.prometheus.jmx.test.support;

import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.credentials.Credentials;
import io.prometheus.jmx.test.support.legacy.BaseResponseLegacy;
import okhttp3.Headers;

/** Base class for all tests */
public abstract class BaseRequest implements Request {

    public static final BaseResponseLegacy RESULT_401 = new BaseResponseLegacy().withCode(401);

    protected HttpClient httpClient;
    protected Headers.Builder headersBuilder;
    protected String path;
    protected Credentials credentials;

    public BaseRequest() {
        headersBuilder = new Headers.Builder();
    }

    /**
     * Constructor
     *
     * @param httpClient httpClient
     */
    public BaseRequest(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.headersBuilder = new Headers.Builder();
    }

    /**
     * Method to set the path
     *
     * @param path path
     * @return this
     */
    public BaseRequest path(String path) {
        this.path = path;
        return this;
    }

    /**
     * Method to add Headers
     *
     * @param headers headers
     * @return this
     */
    public BaseRequest headers(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    public BaseRequest header(String name, String value) {
        headersBuilder.add(name, value);
        return this;
    }

    /**
     * Method to set the Content-Type
     *
     * @param contentType contentType
     * @return this
     */
    public BaseRequest contentType(String contentType) {
        if (contentType != null) {
            headersBuilder.add(Header.CONTENT_TYPE, contentType);
        }
        return this;
    }

    /**
     * Method to set the Credentials
     *
     * @param credentials credentials
     * @return this
     */
    public BaseRequest credentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    @Override
    public Response exchange() {
        return exchange(httpClient);
    }

    @Override
    public Response exchange(HttpClient httpClient) {
        Response response;

        try {
            okhttp3.Request.Builder requestBuilder = httpClient.createRequest(path);

            requestBuilder.headers(headersBuilder.build());

            if (credentials != null) {
                credentials.apply(requestBuilder);
            }

            try (okhttp3.Response okhttp3Response = httpClient.execute(requestBuilder)) {
                response = new Response(okhttp3Response);
            }

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
