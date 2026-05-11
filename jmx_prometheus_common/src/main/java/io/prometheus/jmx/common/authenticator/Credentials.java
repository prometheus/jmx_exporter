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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Immutable credentials container for username and password.
 *
 * <p>Used by authenticator implementations to cache credentials for fast lookup.
 *
 * <p>Password comparison is constant-time to prevent timing side-channel attacks.
 *
 * <p>This class is immutable and thread-safe.
 */
public class Credentials {

    /**
     * The username.
     */
    private final String username;

    /**
     * The password.
     */
    private final String password;

    /**
     * Constructs credentials with the specified username and password.
     *
     * @param username the username, must not be {@code null}
     * @param password the password, must not be {@code null}
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the total size of the credentials in characters.
     *
     * <p>This is the sum of the username and password lengths, used for cache size accounting.
     *
     * @return the total size in characters
     */
    public int size() {
        return username.length() + password.length();
    }

    /**
     * Returns the concatenated username and password without a separator.
     *
     * <p>Used as a cache key by {@link CredentialsCache}. The lack of a separator is intentional
     * because cache size accounting uses the combined length.
     *
     * @return the concatenated username and password
     */
    @Override
    public String toString() {
        return username + password;
    }

    /**
     * Compares credentials for equality using constant-time comparison.
     *
     * <p>Uses {@link MessageDigest#isEqual(byte[], byte[])} on UTF-8 encoded username and password
     * to prevent timing side-channel attacks.
     *
     * @param o the object to compare against
     * @return {@code true} if the object is a {@code Credentials} with matching username and
     *     password
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Credentials credentials = (Credentials) o;
        return MessageDigest.isEqual(
                        username.getBytes(StandardCharsets.UTF_8),
                        credentials.username.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(
                        password.getBytes(StandardCharsets.UTF_8),
                        credentials.password.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
