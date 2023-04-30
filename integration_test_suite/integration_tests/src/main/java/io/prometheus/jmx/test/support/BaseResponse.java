/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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
import org.opentest4j.AssertionFailedError;

import java.util.List;
import java.util.Objects;

/**
 * Class to implement a Response
 */
public class BaseResponse implements Response {

    private Integer code;

    private Headers headers;

    private boolean hasContent;
    private String content;

    private Headers.Builder headersBuilder;

    /**
     * Constructor
     */
    public BaseResponse() {
        headersBuilder = new Headers.Builder();
    }

    /**
     * Method to set the response code
     *
     * @param code
     * @return
     */
    public BaseResponse withCode(int code) {
        this.code = code;
        return this;
    }

    /**
     * Method to set the response Headers
     *
     * @param headers
     * @return
     */
    public BaseResponse withHeaders(Headers headers) {
        if (headers != null) {
            headersBuilder.addAll(headers);
        }
        return this;
    }

    /**
     * Method to set the response Content-Type
     *
     * @param contentType
     * @return
     */
    public BaseResponse withContentType(String contentType) {
        if (contentType != null) {
            headersBuilder.add("Content-Type", contentType);
        }
        return this;
    }

    /**
     * Method to set the response content
     *
     * @param content
     * @return
     */
    public BaseResponse withContent(String content) {
        this.hasContent = true;
        this.content = content;
        return this;
    }

    /**
     * Method to get the response code
     *
     * @return
     */
    @Override
    public int code() {
        return code;
    }

    /**
     * Method to get the response Headers
     *
     * @return
     */
    @Override
    public Headers headers() {
        if (headers == null) {
            headers = headersBuilder.build();
        }
        return headers;
    }

    /**
     * Method to get the response content
     *
     * @return
     */
    @Override
    public String content() {
        return content;
    }

    /**
     * Method to check if this Response is a superset of another Response
     *
     * @param response
     * @return
     */
    @Override
    public Response isSuperset(Response response) {
        if (!checkSuperset(response)) {
            throw new AssertionFailedError("Actual response isn't a superset of the expected response");
        }
        return this;
    }

    /**
     * Method to dispatch the response code to a CodeConsumer
     *
     * @param consume
     * @return
     */
    @Override
    public Response dispatch(CodeConsumer consume) {
        consume.accept(code);
        return this;
    }

    /**
     * Method to dispatch the response Headers to a HeadersConsumer
     *
     * @param consumer
     * @return
     */
    @Override
    public Response dispatch(HeadersConsumer consumer) {
        consumer.accept(headers);
        return this;
    }

    /**
     * Method to dispatch the response content to a ContentConsumer
     *
     * @param consumer
     * @return
     */
    @Override
    public Response dispatch(ContentConsumer consumer) {
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

        BaseResponse that = (BaseResponse) o;

        return hasContent == that.hasContent
                && Objects.equals(code, that.code)
                && Objects.equals(headers, that.headers)
                && Objects.equals(content, that.content)
                && Objects.equals(headersBuilder, that.headersBuilder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, headers, content);
    }

    /**
     * Method to check if this Object is a superset of another object
     *
     * @param o o
     * @return the return value
     */
    private boolean checkSuperset(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BaseResponse that = (BaseResponse) o;

        if (!Objects.equals(code, that.code)) {
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
}
