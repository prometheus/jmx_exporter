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

public class TestResult {

    private int code;
    private Headers headers;
    private String content;

    public TestResult(int code, String contentType, String content) {
        Headers.Builder headersBuilder = new Headers.Builder();
        if (contentType != null) {
            System.out.println(String.format("Content-Type [%s]", contentType));
            headersBuilder.add("Content-Type", contentType);
        }
        Headers headers = headersBuilder.build();
        initialize(code, headers, content);
    }

    public TestResult(int code, Headers headers, String content) {
        initialize(code, headers, content);
    }

    private void initialize(int code, Headers headers, String content) {
        this.code = code;
        this.headers = headers;
        this.content = content;
    }

    public int code() {
        return code;
    }

    public Headers headers() {
        return headers;
    }

    public String content() {
        return content;
    }

    public TestResult isEqualTo(TestResult testResult) {
        equals(testResult);
        return this;
    }

    public TestResult accept(CodeConsumer consume) {
        consume.accept(code);
        return this;
    }

    public TestResult accept(HeadersConsumer consumer) {
        consumer.accept(headers);
        return this;
    }

    public TestResult accept(ContentConsumer consumer) {
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
                System.out.println(String.format("header name [%s]", name));
                List<String> values = headers.values(name);
                for (String value : values) {
                    System.out.println(String.format("value [%s]", value));
                }
                List<String> thatValues = thatHeaders.values(name);
                for (String thatValue : thatValues) {
                    System.out.println(String.format("thatValue [%s]", thatValue));
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
