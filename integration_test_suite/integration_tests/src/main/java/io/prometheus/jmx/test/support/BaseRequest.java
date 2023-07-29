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
import io.prometheus.jmx.test.util.ThrowableUtils;
import okhttp3.Headers;
import okhttp3.ResponseBody;

/** Base class for all tests */
public abstract class BaseRequest implements Request {

    public static final BaseResponse RESULT_401 = new BaseResponse().withCode(401);

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

    /**
     * Method to execute the test
     *
     * @return the TestResult
     */
    @Override
    public Response execute() {
        Response actualResponse = null;

        try {
            okhttp3.Request.Builder requestBuilder = httpClient.createRequest(path);

            if (credentials != null) {
                credentials.apply(requestBuilder);
            }

            try (okhttp3.Response response = httpClient.execute(requestBuilder)) {
                assertThat(response).isNotNull();
                int code = response.code();
                Headers headers = response.headers();
                ResponseBody body = response.body();
                assertThat(body).isNotNull();
                String content = body.string();
                assertThat(content).isNotNull();
                actualResponse =
                        new BaseResponse().withCode(code).withHeaders(headers).withContent(content);
            }
        } catch (Throwable t) {
            ThrowableUtils.throwUnchecked(t);
        }

        return actualResponse;
    }
}
