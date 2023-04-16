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
import io.prometheus.jmx.test.HttpHeader;
import io.prometheus.jmx.test.util.ThrowableUtils;
import io.prometheus.jmx.test.credentials.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.assertj.core.api.Assertions.assertThat;

public class HealthyTest {

    public static final TestResult RESULT_200 = new TestResult(200, null, "Exporter is Healthy.");
    public static final TestResult RESULT_401 = new TestResult(401, null, null);

    private final HttpClient httpClient;
    private Credentials credentials;

    public HealthyTest(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HealthyTest withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }

    public void expect(TestResult testResult) {
        try {
            Request.Builder requestBuilder = httpClient.createRequest("/-/healthy");

            if (credentials != null) {
                credentials.apply(requestBuilder);
            }

            if (testResult.contentType() != null) {
                requestBuilder.addHeader(HttpHeader.ACCEPT, testResult.contentType());
            }

            try (Response response = httpClient.execute(requestBuilder)) {
                assertThat(response).isNotNull();
                int code = response.code();
                String contentType = response.header(HttpHeader.CONTENT_TYPE);
                ResponseBody body = response.body();
                assertThat(body).isNotNull();
                String content = body.string();
                assertThat(content).isNotNull();
                testResult.expect(code, contentType, content);
            }
        } catch (Throwable t) {
            ThrowableUtils.throwUnchecked(t);
        }
    }
}
