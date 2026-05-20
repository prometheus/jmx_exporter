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
     * Pre-computed UTF-8 encoded username bytes for constant-time comparison.
     */
    private final byte[] usernameBytes;

    /**
     * Pre-computed UTF-8 encoded password bytes for constant-time comparison.
     */
    private final byte[] passwordBytes;

    /**
     * Pre-computed hash code.
     */
    private final int hash;

    /**
     * Pre-computed UTF-8 byte size of username + password.
     */
    private final int byteSize;

    /**
     * Constructs credentials with the specified username and password.
     *
     * @param username the username, must not be {@code null}
     * @param password the password, must not be {@code null}
     */
    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
        this.usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        this.passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        this.hash = Objects.hash(username, password);
        this.byteSize = usernameBytes.length + passwordBytes.length;
    }

    /**
     * Returns the UTF-8 byte size of the concatenated username and password.
     *
     * @return the total size in bytes
     */
    public int byteSize() {
        return byteSize;
    }

    /**
     * Returns the concatenated username and password separated by a null character.
     *
     * <p>The null separator prevents edge-case collisions where boundaries between username and
     * password could be ambiguous (e.g., {@code ("us", "erpass")} vs {@code ("use", "rpass")}).
     *
     * @return the concatenated username and password with a null separator
     */
    @Override
    public String toString() {
        return username + "\0" + password;
    }

    /**
     * Compares credentials for equality using constant-time comparison.
     *
     * <p>Uses {@link MessageDigest#isEqual(byte[], byte[])} on pre-computed UTF-8 encoded username
     * and password byte arrays to prevent timing side-channel attacks.
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
        return MessageDigest.isEqual(this.usernameBytes, credentials.usernameBytes)
                && MessageDigest.isEqual(this.passwordBytes, credentials.passwordBytes);
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
