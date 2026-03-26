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
import java.math.BigInteger;
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
 * performance. Credentials are cached up to 1 MB for valid credentials and 10 MB for invalid
 * credentials.
 *
 * <p>Thread-safety: This class is thread-safe. Credential cache operations are synchronized.
 *
 * @see PlaintextAuthenticator
 * @see PBKDF2Authenticator
 */
public class MessageDigestAuthenticator extends BasicAuthenticator {

    /**
     * Maximum cache size for valid credentials in bytes (1 MB).
     */
    private static final int MAXIMUM_VALID_CACHE_SIZE_BYTES = 1000000; // 1 MB

    /**
     * Maximum cache size for invalid credentials in bytes (10 MB).
     */
    private static final int MAXIMUM_INVALID_CACHE_SIZE_BYTES = 10000000; // 10 MB

    /**
     * The expected username for authentication.
     */
    private final String username;

    /**
     * The expected password hash for authentication.
     */
    private final String passwordHash;

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
        this.passwordHash = passwordHash.toLowerCase().replace(":", "");
        this.algorithm = algorithm;
        this.salt = salt;
        this.validCredentialsCache = new CredentialsCache(MAXIMUM_VALID_CACHE_SIZE_BYTES);
        this.invalidCredentialsCache = new CredentialsCache(MAXIMUM_INVALID_CACHE_SIZE_BYTES);
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

        boolean isValid = this.username.equals(username)
                && this.passwordHash.equals(generatePasswordHash(algorithm, salt, password));

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
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            return number.toString(16).toLowerCase();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
