/*
 * Copyright (C) 2020-2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx;

import java.nio.charset.StandardCharsets;

/** Class to implement a Murmur3Hash */
public class Murmur3Hash {

    private static final int SEED = 0;

    /** Constructor */
    private Murmur3Hash() {
        // DO NOTHING
    }

    /**
     * Method to hash a String to a Murmur3 hash String
     *
     * @param string string
     * @return a Murmur3 hash String
     */
    public static String hash(String string) {
        if (string == null) {
            return null;
        }
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        int hashValue = hashBytes(data);
        return Integer.toHexString(hashValue);
    }

    private static int hashBytes(byte[] bytes) {
        int length = bytes.length;
        int h = SEED;
        int currentIndex = 0;

        if (length > 0) {
            int nBlocks = length / 4;

            for (int i = 0; i < nBlocks; i++) {
                int k = bytes[currentIndex++] & 0xFF;
                k |= (bytes[currentIndex++] & 0xFF) << 8;
                k |= (bytes[currentIndex++] & 0xFF) << 16;
                k |= (bytes[currentIndex++] & 0xFF) << 24;

                k *= 0xcc9e2d51;
                k = Integer.rotateLeft(k, 15);
                k *= 0x1b873593;

                h ^= k;
                h = Integer.rotateLeft(h, 13);
                h = h * 5 + 0xe6546b64;
            }

            int k1 = 0;
            switch (length & 3) {
                case 3:
                    k1 ^= (bytes[currentIndex + 2] & 0xFF) << 16;
                case 2:
                    k1 ^= (bytes[currentIndex + 1] & 0xFF) << 8;
                case 1:
                    k1 ^= (bytes[currentIndex] & 0xFF);
                    k1 *= 0xcc9e2d51;
                    k1 = Integer.rotateLeft(k1, 15);
                    k1 *= 0x1b873593;
                    h ^= k1;
            }

            h ^= length;
            h ^= h >>> 16;
            h *= 0x85ebca6b;
            h ^= h >>> 13;
            h *= 0xc2b2ae35;
            h ^= h >>> 16;
        }

        return h;
    }
}
