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

package io.prometheus.jmx.common.authenticator;

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.util.Precondition;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Basic authenticator that validates credentials using PBKDF2 password hashing.
 *
 * <p>Supports PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA256, and PBKDF2WithHmacSHA512 algorithms.
 * This is the most secure authentication method available, recommended for production use.
 *
 * <p>Each non-null authentication attempt derives a candidate PBKDF2 hash and compares both the
 * presented username and derived password hash using constant-time equality checks.
 *
 * <p>Thread-safety: This class is thread-safe. All configuration state is immutable after
 * construction, and credential verification uses constant-time comparisons.
 *
 * @see PlaintextAuthenticator
 * @see MessageDigestAuthenticator
 */
public class PBKDF2Authenticator extends BasicAuthenticator {

    /**
     * Hexadecimal characters for converting bytes to hex strings.
     */
    private static final char[] HEXADECIMAL_CHARACTERS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * The expected username for authentication.
     */
    private final String username;

    /**
     * The expected password hash for authentication (stored as bytes for constant-time comparison).
     */
    private final byte[] passwordHashBytes;

    /**
     * The PBKDF2 algorithm (PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA256, or PBKDF2WithHmacSHA512).
     */
    private final String algorithm;

    /**
     * The salt used in password hashing.
     */
    private final String salt;

    /**
     * The number of iterations for key derivation.
     */
    private final int iterations;

    /**
     * The key length in bits (note: constructor parameter is in bytes, converted to bits internally).
     */
    private final int keyLength;

    /**
     * Constructs a PBKDF2 authenticator with the specified parameters.
     *
     * @param realm the HTTP authentication realm, must not be {@code null} or blank
     * @param username the expected username, must not be {@code null} or blank
     * @param passwordHash the expected password hash, must not be {@code null} or blank
     * @param algorithm the PBKDF2 algorithm, must not be {@code null} or blank
     * @param salt the salt used in hashing, must not be {@code null} or blank
     * @param iterations the number of iterations, must be at least 1
     * @param keyLength the key length in bits, must be at least 1
     * @throws GeneralSecurityException if the algorithm is not supported
     * @throws NullPointerException if any string parameter is {@code null}
     * @throws IllegalArgumentException if any string parameter is blank or if iterations/keyLength
     *     is invalid
     */
    public PBKDF2Authenticator(
            String realm,
            String username,
            String passwordHash,
            String algorithm,
            String salt,
            int iterations,
            int keyLength)
            throws GeneralSecurityException {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(passwordHash);
        Precondition.notNullOrEmpty(algorithm);
        Precondition.notNullOrEmpty(salt);
        Precondition.isGreaterThanOrEqualTo(iterations, 1);
        Precondition.isGreaterThanOrEqualTo(keyLength, 1);

        SecretKeyFactory.getInstance(algorithm);

        this.username = username;
        this.passwordHashBytes = hexStringToByteArray(passwordHash.toLowerCase().replace(":", ""));
        this.algorithm = algorithm;
        this.salt = salt;
        this.iterations = iterations;
        this.keyLength = keyLength;
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        byte[] candidateHashBytes = generatePasswordHashBytes(algorithm, salt, iterations, keyLength, password);
        return MessageDigest.isEqual(
                        this.username.getBytes(StandardCharsets.UTF_8), username.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(this.passwordHashBytes, candidateHashBytes);
    }

    /**
     * Generates a password hash using PBKDF2 key derivation.
     *
     * @param algorithm the PBKDF2 algorithm
     * @param salt the salt
     * @param iterations the number of iterations
     * @param keyLength the key length in bits
     * @param password the password to hash
     * @return the lowercase hexadecimal hash
     * @throws RuntimeException if key derivation fails
     */
    private static String generatePasswordHash(
            String algorithm, String salt, int iterations, int keyLength, String password) {
        byte[] hashBytes = generatePasswordHashBytes(algorithm, salt, iterations, keyLength, password);
        return toLowerCaseHexadecimal(hashBytes);
    }

    /**
     * Generates a password hash as bytes for constant-time comparison.
     *
     * @param algorithm the PBKDF2 algorithm
     * @param salt the salt
     * @param iterations the number of iterations
     * @param keyLength the key length in bits
     * @param password the password to hash
     * @return the hash as a byte array
     * @throws RuntimeException if key derivation fails
     */
    private static byte[] generatePasswordHashBytes(
            String algorithm, String salt, int iterations, int keyLength, String password) {
        try {
            // Note: keyLength parameter is in bytes (for historical reasons), convert to bits for PBEKeySpec
            PBEKeySpec pbeKeySpec = new PBEKeySpec(
                    password.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, keyLength * 8);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            return secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Converts a byte array to a lowercase hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the lowercase hexadecimal string representation
     */
    private static String toLowerCaseHexadecimal(byte[] bytes) {
        int len = bytes.length;
        char[] result = new char[len * 2];

        for (int i = 0, j = 0; i < len; i++) {
            int v = bytes[i] & 0xFF;
            result[j++] = HEXADECIMAL_CHARACTERS[v >>> 4];
            result[j++] = HEXADECIMAL_CHARACTERS[v & 0x0F];
        }

        return new String(result);
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex the hexadecimal string to convert
     * @return the byte array representation
     * @throws IllegalArgumentException if the hex string is invalid
     */
    private static byte[] hexStringToByteArray(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }

        int len = hex.length();
        byte[] bytes = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }

        return bytes;
    }
}
