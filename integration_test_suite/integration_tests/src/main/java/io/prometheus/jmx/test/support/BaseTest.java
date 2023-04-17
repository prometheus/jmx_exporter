/*
 * Copyright 2023 Douglas Hoard
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
import io.prometheus.jmx.test.util.ThrowableUtils;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for all tests
 */
public abstract class BaseTest implements Test {

    public static final TestResult RESULT_401 = new TestResult().withCode(401);

    protected final HttpClient httpClient;
    protected final Headers.Builder headersBuilder;
    protected String path;
    protected Credentials credentials;

    /**
     * Constructor
     *
     * @param httpClient
     */
    public BaseTest(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.headersBuilder = new Headers.Builder();
    }

    /**
     * Method to set the path
     *
     * @param path
     * @return
     */
    public BaseTest withPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Method to add Headers
     *
     * @param headers
     * @return
     */
    public BaseTest withHeaders(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    /**
     * Method to set the Content-Type
     *
     * @param contentType
     * @return
     */
    public BaseTest withContentType(String contentType) {
        if (contentType != null) {
            headersBuilder.add("Content-Type", contentType);
        }
        return this;
    }

    /**
     * Method to set the Credentials
     *
     * @param credentials
     * @return
     */
    public BaseTest withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    /**
     * Method to execute the test
     *
     * @return the TestResult
     */
    public TestResult execute() {
        TestResult actualTestResult = null;

        try {
            Request.Builder requestBuilder = httpClient.createRequest(path);

            if (credentials != null) {
                credentials.apply(requestBuilder);
            }

            try (Response response = httpClient.execute(requestBuilder)) {
                assertThat(response).isNotNull();
                int code = response.code();
                Headers headers = response.headers();
                ResponseBody body = response.body();
                assertThat(body).isNotNull();
                String content = body.string();
                assertThat(content).isNotNull();
                actualTestResult = new TestResult().withCode(code).withHeaders(headers).withContent(content);
            }
        } catch (Throwable t) {
            ThrowableUtils.throwUnchecked(t);
        }

        return actualTestResult;
    }
}
