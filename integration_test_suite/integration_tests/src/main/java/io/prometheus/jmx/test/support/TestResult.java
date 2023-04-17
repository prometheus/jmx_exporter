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

import okhttp3.Headers;

import java.util.List;
import java.util.Objects;

/**
 * Class to implement a TestResult
 */
public class TestResult {

    private Integer code;

    private Headers headers;

    private boolean hasContent;
    private String content;

    private Headers.Builder headersBuilder;

    /**
     * Constructor
     */
    public TestResult() {
        headersBuilder = new Headers.Builder();
    }

    /**
     * Method to set the test result code
     *
     * @param code
     * @return
     */
    public TestResult withCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * Method to set the test result Headers
     *
     * @param headers
     * @return
     */
    public TestResult withHeaders(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    /**
     * Method to set the test result Content-Type
     *
     * @param contentType
     * @return
     */
    public TestResult withContentType(String contentType) {
        if (contentType != null) {
            headersBuilder.add("Content-Type", contentType);
        }
        return this;
    }

    /**
     * Method to set the test result content
     *
     * @param content
     * @return
     */
    public TestResult withContent(String content) {
        this.hasContent = true;
        this.content = content;
        return this;
    }

    /**
     * Method to get the test result code
     *
     * @return
     */
    public int code() {
        return code;
    }

    /**
     * Method to get the test result Headers
     *
     * @return
     */
    public Headers headers() {
        if (headers == null) {
            headers = headersBuilder.build();
        }
        return headers;
    }

    /**
     * Method to get the test result content
     *
     * @return
     */
    public String content() {
        return content;
    }

    /**
     * Method to compare whether a TestResult is equal to this test result
     *
     * @param testResult
     * @return
     */
    public TestResult isEqualTo(TestResult testResult) {
        equals(testResult);
        return this;
    }

    /**
     * Method to set the test result CodeConsumer
     *
     * @param consume
     * @return
     */
    public TestResult dispatch(CodeConsumer consume) {
        consume.accept(code);
        return this;
    }

    /**
     * Method to set the test result HeadersConsumer
     *
     * @param consumer
     * @return
     */
    public TestResult dispatch(HeadersConsumer consumer) {
        consumer.accept(headers);
        return this;
    }

    /**
     * Method to set the test result ContentConsumer
     *
     * @param consumer
     * @return
     */
    public TestResult dispatch(ContentConsumer consumer) {
        consumer.accept(content);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TestResult that = (TestResult) o;

        if (code != that.code) {
            return false;
        }

        if ((this.headers != null) && (that.headers == null)) {
            return false;
        } else if ((this.headers == null) && (that.headers != null)) {
            return false;
        } else if ((this.headers != null) && (that.headers != null)) {
            Headers thatHeaders = that.headers;
            for (String name : thatHeaders.names()) {
                List<String> values = headers.values(name);
                List<String> thatValues = thatHeaders.values(name);
                for (String thatValue : thatValues) {
                    if (!values.contains(thatValue)) {
                        return false;
                    }
                }
            }
        }

        if (that.content != null) {
            return Objects.equals(content, that.content);
        } else {
            return true;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, headers, content);
    }
}
