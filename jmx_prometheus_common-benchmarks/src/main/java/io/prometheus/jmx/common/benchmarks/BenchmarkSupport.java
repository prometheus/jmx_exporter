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

package io.prometheus.jmx.common.benchmarks;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Shared fixtures and helpers for the jmx_prometheus_common benchmarks.
 */
final class BenchmarkSupport {

    private static final char[] HEXADECIMAL_CHARACTERS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private BenchmarkSupport() {
        // Intentionally empty
    }

    static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    static String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            result[j++] = HEXADECIMAL_CHARACTERS[value >>> 4];
            result[j++] = HEXADECIMAL_CHARACTERS[value & 0x0F];
        }
        return new String(result);
    }

    static String messageDigestHash(String algorithm, String salt, String password) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return toHex(digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8)));
    }

    static String pbkdf2Hash(String algorithm, String salt, int iterations, int keyLengthBits, String password)
            throws GeneralSecurityException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
        PBEKeySpec keySpec = new PBEKeySpec(
                password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, keyLengthBits);
        try {
            return toHex(factory.generateSecret(keySpec).getEncoded());
        } finally {
            keySpec.clearPassword();
        }
    }
}
