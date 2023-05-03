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

    public static final Response RESULT_401 = new BaseResponse().withCode(401);

    private enum Status {
        OBJECT_NULL,
        OBJECT_CLASS_MISMATCH,
        STATUS_CODE_MISMATCH,
        HEADERS_MISMATCH_1,
        HEADERS_MISMATCH_2,
        HEADERS_MISMATCH_3,
        CONTENT_MISMATCH_1,
        MATCH }

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
        Status status = checkSuperset(response);
        switch (status) {
            case MATCH: {
                break;
            } default: {
                throw new AssertionFailedError(
                        String.format(
                                "Actual response is not a superset of the expected response, error [%s]",
                                status));
            }
        }
        return this;
    }

    /**
     * Method to dispatch the response code to a CodeConsumer
     *
     * @param consumer
     * @return
     */
    @Override
    public Response dispatch(CodeConsumer consumer) {
        consumer.accept(code);
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
    private Status checkSuperset(Object o) {
        if (this == o) {
            return Status.MATCH;
        }

        if (o == null) {
            return Status.OBJECT_NULL;
        }

        if (getClass() != o.getClass()) {
            return Status.OBJECT_CLASS_MISMATCH;
        }

        BaseResponse that = (BaseResponse) o;

        if (!Objects.equals(this.code, that.code)) {
            return Status.STATUS_CODE_MISMATCH;
        }

        if (this.headers != null && that.headers == null) {
            return Status.HEADERS_MISMATCH_1;
        } else if (this.headers == null && that.headers != null) {
            return Status.HEADERS_MISMATCH_2;
        } else if (this.headers != null && that.headers != null) {
            Headers thatHeaders = that.headers;
            for (String name : thatHeaders.names()) {
                List<String> values = this.headers.values(name);
                List<String> thatValues = thatHeaders.values(name);
                for (String thatValue : thatValues) {
                    if (!values.contains(thatValue)) {
                        return Status.HEADERS_MISMATCH_3;
                    }
                }
            }
        }

        if (that.content != null && !Objects.equals(this.content, that.content)) {
            return Status.CONTENT_MISMATCH_1;
        }

        return Status.MATCH;
    }
}
