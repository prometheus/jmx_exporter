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

/*
 * This product includes software based on Stackoverflow
 * Code : toHex() method
 * Author : maybeWeCouldStealAVan
 * Reference: https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
 */

package io.prometheus.jmx.common.http.authenticator;

public class HexString {

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    /** Constructor */
    private HexString() {
        // DO NOTHING
    }

    /**
     * Method to convert a byte array to a lowercase hexadecimal String
     *
     * @param bytes bytes
     * @return the return value
     */
    public static String toHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            hexChars[j++] = HEX_ARRAY[(0xF0 & bytes[i]) >>> 4];
            hexChars[j++] = HEX_ARRAY[0x0F & bytes[i]];
        }
        return new String(hexChars).toLowerCase();
    }
}
