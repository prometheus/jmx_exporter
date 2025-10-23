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

/** Class to implement HttpResponseBody */
public class HttpResponseBody {

    private final byte[] bytes;

    /**
     * Constructor
     *
     * @param bytes bytes
     */
    public HttpResponseBody(byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Get the body byte array
     *
     * @return the body byte array
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Get the body as a String
     *
     * @return the body as a String
     */
    public String string() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Get the body as a String using a specific Charset
     *
     * @param charset charset
     * @return the body as a String using a specific Charset
     */
    public String string(Charset charset) {
        return new String(bytes, charset);
    }
}
