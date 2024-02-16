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

package io.prometheus.jmx.test.support.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
     * Method to get the body as bytes
     *
     * @return the body as bytes
     */
    public byte[] bytes() {
        return bytes;
    }

    /**
     * Method to get the body as a String using UTF-8
     *
     * @return the body as a String using UTF-8
     */
    public String string() {
        return string(StandardCharsets.UTF_8);
    }

    /**
     * Method to get the body as a String using a specific character set
     *
     * @param charset charset
     * @return the body as a String using the specified character set
     */
    public String string(Charset charset) {
        String string = null;
        if (bytes != null) {
            string = new String(bytes, charset);
        }
        return string;
    }
}
