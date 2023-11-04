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

import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.HttpClient;
import io.prometheus.jmx.test.credentials.Credentials;
import io.prometheus.jmx.test.support.legacy.BaseResponseLegacy;
import io.prometheus.jmx.test.util.ThrowableUtils;
import okhttp3.Headers;

/** Base class for all tests */
public abstract class BaseRequest implements Request {

    public static final BaseResponseLegacy RESULT_401 = new BaseResponseLegacy().withCode(401);

    protected final HttpClient httpClient;
    protected final Headers.Builder headersBuilder;
    protected String path;
    protected Credentials credentials;

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
    public BaseRequest withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Method to add Headers
     *
     * @param headers headers
     * @return this
     */
    public BaseRequest withHeaders(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    public BaseRequest withHeader(String name, String value) {
        headersBuilder.add(name, value);
        return this;
    }

    /**
     * Method to set the Content-Type
     *
     * @param contentType contentType
     * @return this
     */
    public BaseRequest withContentType(String contentType) {
        if (contentType != null) {
            headersBuilder.add("Content-Type", contentType);
        }
        return this;
    }

    /**
     * Method to set the Credentials
     *
     * @param credentials credentials
     * @return this
     */
    public BaseRequest withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    @Override
    public ResponseCallback execute() {
        try {
            okhttp3.Request.Builder requestBuilder = httpClient.createRequest(path);

            requestBuilder.headers(headersBuilder.build());

            if (credentials != null) {
                credentials.apply(requestBuilder);
            }

            try (okhttp3.Response response = httpClient.execute(requestBuilder)) {
                assertThat(response).isNotNull();
                return new ResponseCallback(new Response(response));
            }
        } catch (Throwable t) {
            ThrowableUtils.throwUnchecked(t);
        }

        return null;
    }
}
