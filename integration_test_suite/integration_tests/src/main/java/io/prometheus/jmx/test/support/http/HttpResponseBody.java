/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.support.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents an HTTP response body that can be accessed as a byte array or decoded string.
 */
public class HttpResponseBody {

    private final byte[] bytes;

    /**
     * Creates an HTTP response body from the raw byte content.
     *
     * @param bytes the raw bytes of the response body
     */
    public HttpResponseBody(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Returns the raw bytes of the response body.
     *
     * @return the response body as a byte array
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Decodes the response body as a UTF-8 string.
     *
     * @return the response body decoded as a UTF-8 string
     */
    public String string() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Decodes the response body as a string using the specified charset.
     *
     * @param charset the charset to use for decoding the response body bytes
     * @return the response body decoded as a string using the specified charset
     */
    public String string(Charset charset) {
        return new String(bytes, charset);
    }
}
