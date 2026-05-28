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
import java.security.MessageDigest;

/**
 * Basic authenticator that validates credentials against a plaintext username and password.
 *
 * <p>This authenticator is suitable for development and testing. For production environments,
 * consider using {@link MessageDigestAuthenticator} or {@link PBKDF2Authenticator} for more
 * secure password handling.
 *
 * <p>This authenticator caches both valid and invalid credentials in a single cache to improve
 * authentication performance, using a maximum credential size of 5 KiB and an approximately
 * 500 KiB maximum cache weight.
 *
 * <p>Thread-safety: This class is thread-safe. Credential cache operations are thread-safe,
 * backed by Caffeine. Password hash comparison is constant-time.
 *
 * @see MessageDigestAuthenticator
 * @see PBKDF2Authenticator
 */
public class PlaintextAuthenticator extends BasicAuthenticator {

    /**
     * Maximum size for a single cached credential value in bytes (5 KiB).
     */
    private static final int MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES = CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES;

    /**
     * Maximum number of entries per credential cache.
     */
    private static final int MAXIMUM_CREDENTIAL_CACHE_ENTRIES = CredentialsCache.DEFAULT_MAX_ENTRIES;

    /**
     * The expected username for authentication, encoded as UTF-8 bytes for constant-time comparison.
     */
    private final byte[] usernameBytes;

    /**
     * The expected password for authentication, encoded as UTF-8 bytes for constant-time comparison.
     */
    private final byte[] passwordBytes;

    /**
     * Single cache for valid and invalid credentials.
     */
    private final CredentialsCache credentialsCache;

    /**
     * Constructs a plaintext authenticator with the specified credentials.
     *
     * @param realm the HTTP authentication realm, must not be {@code null} or blank
     * @param username the expected username, must not be {@code null} or blank
     * @param password the expected password, must not be {@code null} or blank
     * @throws NullPointerException if any parameter is {@code null}
     * @throws IllegalArgumentException if any parameter is blank
     */
    public PlaintextAuthenticator(String realm, String username, String password) {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(password);

        this.usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        this.passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        this.credentialsCache =
                new CredentialsCache(MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES, MAXIMUM_CREDENTIAL_CACHE_ENTRIES);
    }

    /**
     * Validates the presented credentials using a single valid/invalid cache and constant-time
     * comparison.
     *
     * <p>The cache is checked first. If found, the cached result is returned. If not found,
     * both the username and password are compared using constant-time equality checks via
     * {@link MessageDigest#isEqual(byte[], byte[])} to prevent timing side-channel attacks.
     * The result is then stored in the cache.
     *
     * @param username the presented username, may be {@code null}
     * @param password the presented password, may be {@code null}
     * @return {@code true} if both username and password match, {@code false} if either is
     *     {@code null} or they do not match
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        Credentials credentials = new Credentials(username, password);
        Boolean cached = credentialsCache.get(credentials);
        if (cached != null) {
            return cached;
        }

        boolean usernameMatches = MessageDigest.isEqual(this.usernameBytes, username.getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = MessageDigest.isEqual(this.passwordBytes, password.getBytes(StandardCharsets.UTF_8));
        boolean isValid = usernameMatches & passwordMatches;

        if (isValid) {
            credentialsCache.add(credentials);
        } else {
            credentialsCache.addInvalid(credentials);
        }

        return isValid;
    }
}
