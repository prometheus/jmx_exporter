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
 * <p>Thread-safety: This class is thread-safe. Password comparison is constant-time.
 *
 * @see MessageDigestAuthenticator
 * @see PBKDF2Authenticator
 */
public class PlaintextAuthenticator extends BasicAuthenticator {

    /**
     * The expected username for authentication, encoded as UTF-8 bytes for constant-time comparison.
     */
    private final byte[] usernameBytes;

    /**
     * The expected password for authentication, encoded as UTF-8 bytes for constant-time comparison.
     */
    private final byte[] passwordBytes;

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
    }

    /**
     * Validates the presented credentials against the configured plaintext username and password.
     *
     * <p>Both the username and password are compared using constant-time equality checks via
     * {@link MessageDigest#isEqual(byte[], byte[])} to prevent timing side-channel attacks.
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

        boolean usernameMatches = MessageDigest.isEqual(this.usernameBytes, username.getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = MessageDigest.isEqual(this.passwordBytes, password.getBytes(StandardCharsets.UTF_8));
        return usernameMatches & passwordMatches;
    }
}
