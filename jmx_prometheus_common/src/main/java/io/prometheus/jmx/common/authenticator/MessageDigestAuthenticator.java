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

/**
 * Basic authenticator that validates credentials using salted message digest password hashing.
 *
 * <p>Supports SHA-1, SHA-256, and SHA-512 algorithms. Passwords are hashed using the formula:
 * {@code hash(algorithm, salt + ":" + password)}.
 *
 * <p>This authenticator caches both valid and invalid credentials to improve authentication
 * performance, using a maximum value size of 5 KiB and a maximum of 100 entries per
 * cache.
 *
 * <p>Thread-safety: This class is thread-safe. Credential cache operations are synchronized.
 * Password hash comparison is constant-time.
 *
 * @see PlaintextAuthenticator
 * @see PBKDF2Authenticator
 */
public class MessageDigestAuthenticator extends BasicAuthenticator {

    /** Maximum size for a single cached credential value in bytes (5 KiB). */
    private static final int MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES = CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES;

    /** Maximum number of entries per credential cache. */
    private static final int MAXIMUM_CREDENTIAL_CACHE_ENTRIES = CredentialsCache.DEFAULT_MAX_ENTRIES;

    /**
     * The expected username for authentication.
     */
    private final String username;

    /**
     * The expected password hash for authentication (stored as bytes for constant-time comparison).
     */
    private final byte[] passwordHashBytes;

    /**
     * The hashing algorithm (SHA-1, SHA-256, or SHA-512).
     */
    private final String algorithm;

    /**
     * The salt used in password hashing.
     */
    private final String salt;

    /**
     * Cache for valid credentials.
     */
    private final CredentialsCache validCredentialsCache;

    /**
     * Cache for invalid credentials.
     */
    private final CredentialsCache invalidCredentialsCache;

    /**
     * Constructs a message digest authenticator with the specified parameters.
     *
     * @param realm the HTTP authentication realm, must not be {@code null} or blank
     * @param username the expected username, must not be {@code null} or blank
     * @param passwordHash the expected password hash, must not be {@code null} or blank
     * @param algorithm the hashing algorithm (SHA-1, SHA-256, or SHA-512), must not be {@code null}
     *     or blank
     * @param salt the salt used in hashing, must not be {@code null} or blank
     * @throws GeneralSecurityException if the algorithm is not supported
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public MessageDigestAuthenticator(String realm, String username, String passwordHash, String algorithm, String salt)
            throws GeneralSecurityException {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(passwordHash);
        Precondition.notNullOrEmpty(algorithm);
        Precondition.notNullOrEmpty(salt);

        MessageDigest.getInstance(algorithm);

        this.username = username;
        this.passwordHashBytes = hexStringToByteArray(passwordHash.toLowerCase().replace(":", ""));
        this.algorithm = algorithm;
        this.salt = salt;
        this.validCredentialsCache =
                new CredentialsCache(MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES, MAXIMUM_CREDENTIAL_CACHE_ENTRIES);
        this.invalidCredentialsCache =
                new CredentialsCache(MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES, MAXIMUM_CREDENTIAL_CACHE_ENTRIES);
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        Credentials credentials = new Credentials(username, password);
        if (validCredentialsCache.contains(credentials)) {
            return true;
        } else if (invalidCredentialsCache.contains(credentials)) {
            return false;
        }

        byte[] candidateHashBytes = generatePasswordHashBytes(algorithm, salt, password);
        boolean isValid = MessageDigest.isEqual(
                        this.username.getBytes(StandardCharsets.UTF_8), username.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(this.passwordHashBytes, candidateHashBytes);

        if (isValid) {
            validCredentialsCache.add(credentials);
        } else {
            invalidCredentialsCache.add(credentials);
        }

        return isValid;
    }

    /**
     * Generates a password hash using the configured message digest algorithm.
     *
     * @param algorithm the hashing algorithm
     * @param salt the salt
     * @param password the password to hash
     * @return the lowercase hexadecimal hash
     * @throws RuntimeException if the algorithm is not supported
     */
    private static String generatePasswordHash(String algorithm, String salt, String password) {
        byte[] hashBytes = generatePasswordHashBytes(algorithm, salt, password);
        return toLowerCaseHexadecimal(hashBytes);
    }

    /**
     * Generates a password hash as bytes for constant-time comparison.
     *
     * @param algorithm the hashing algorithm
     * @param salt the salt
     * @param password the password to hash
     * @return the hash as a byte array
     * @throws RuntimeException if the algorithm is not supported
     */
    private static byte[] generatePasswordHashBytes(String algorithm, String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
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
            result[j++] = Integer.toHexString(v >>> 4).charAt(0);
            result[j++] = Integer.toHexString(v & 0x0F).charAt(0);
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
