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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.jmx.common.util.Precondition;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * A thread-safe bounded cache for credentials backed by Caffeine.
 *
 * <p>The cache evicts entries based on a maximum weight in bytes, using the UTF-8 encoded byte size
 * of the credential's string representation as the weight. Entries larger than the configured
 * maximum value size are not cached.
 *
 * <p>The cache is keyed by a lightweight {@link CredentialsKey} that holds only the username and
 * password strings. A cache lookup therefore does not need to allocate a full {@link Credentials}
 * (with its UTF-8 {@code byte[]} copies) just to determine whether the presented credentials are
 * already known to be valid. The key comparison is constant-time and does not allocate.
 *
 * <p>Thread-safety: This class is thread-safe. All public methods delegate to the underlying
 * Caffeine cache, which provides concurrent access.
 */
public class CredentialsCache {

    /**
     * Default maximum size for a single cached credential value (5 KiB).
     */
    public static final int DEFAULT_MAX_VALUE_SIZE_BYTES = 5 * 1024;

    /**
     * Default maximum number of cached credentials.
     */
    public static final int DEFAULT_MAX_ENTRIES = 100;

    /**
     * Default maximum cache weight in bytes ({@value #DEFAULT_MAX_VALUE_SIZE_BYTES} * {@value #DEFAULT_MAX_ENTRIES}).
     */
    public static final long DEFAULT_MAX_WEIGHT_BYTES = (long) DEFAULT_MAX_VALUE_SIZE_BYTES * DEFAULT_MAX_ENTRIES;

    private static final Boolean VALID = Boolean.TRUE;

    /**
     * Maximum cacheable credential size in bytes.
     */
    private final int maxValueSizeBytes;

    /**
     * Maximum cache weight in bytes.
     */
    private final long maxWeightBytes;

    /**
     * The backing Caffeine cache.
     */
    private final Cache<CredentialsKey, Boolean> cache;

    /**
     * Constructs a credentials cache with the specified per-entry and weight limits.
     *
     * @param maxValueSizeBytes maximum size of a single cached credential value in bytes, must be
     *     positive
     * @param maxWeightBytes maximum total cache weight in bytes, must be positive
     */
    public CredentialsCache(int maxValueSizeBytes, long maxWeightBytes) {
        this(maxValueSizeBytes, maxWeightBytes, Duration.ZERO);
    }

    /**
     * Constructs a credentials cache with the specified per-entry, weight, and TTL limits.
     *
     * <p>If TTL is {@link Duration#ZERO}, entries have no time-based expiry.
     *
     * @param maxValueSizeBytes maximum size of a single cached credential value in bytes, must be
     *     positive
     * @param maxWeightBytes maximum total cache weight in bytes, must be positive
     * @param ttl time-to-live for cached entries, {@link Duration#ZERO} for no expiry
     */
    public CredentialsCache(int maxValueSizeBytes, long maxWeightBytes, Duration ttl) {
        Precondition.isGreaterThanOrEqualTo(maxValueSizeBytes, 1);
        Precondition.isGreaterThanOrEqualTo((int) Math.min(maxWeightBytes, Integer.MAX_VALUE), 1);

        this.maxValueSizeBytes = maxValueSizeBytes;
        this.maxWeightBytes = maxWeightBytes;

        Caffeine<CredentialsKey, Boolean> builder = Caffeine.newBuilder()
                .maximumWeight(maxWeightBytes)
                .weigher((CredentialsKey key, Boolean ignored) -> key.byteSize());

        if (!ttl.isZero() && !ttl.isNegative()) {
            builder.expireAfterWrite(ttl);
        }

        this.cache = builder.build();
    }

    /**
     * Constructs a credentials cache with the specified per-entry and entry-count limits.
     *
     * <p>The weight limit is computed as {@code maxValueSizeBytes * maxEntries}.
     *
     * @param maxValueSizeBytes maximum size of a single cached credential value in bytes, must be
     *     positive
     * @param maxEntries maximum number of cached credentials, must be positive
     */
    public CredentialsCache(int maxValueSizeBytes, int maxEntries) {
        this(maxValueSizeBytes, (long) maxValueSizeBytes * maxEntries);
    }

    /**
     * Adds credentials to the cache as valid.
     *
     * <p>If the credentials size exceeds the maximum value size, they are not cached. If the
     * credentials are already present, they are refreshed as a recently used entry.
     *
     * @param credentials credentials to add, must not be {@code null}
     */
    public void add(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        add(credentials.username(), credentials.password());
    }

    /**
     * Adds credentials to the cache as valid without constructing a {@link Credentials} value.
     *
     * @param username the username, must not be {@code null}
     * @param password the password, must not be {@code null}
     */
    public void add(String username, String password) {
        Precondition.notNull(username, "username is null");
        Precondition.notNull(password, "password is null");

        CredentialsKey key = new CredentialsKey(username, password);
        if (key.byteSize() > maxValueSizeBytes) {
            return;
        }

        cache.put(key, VALID);
    }

    /**
     * Checks if the cache contains the specified credentials.
     *
     * <p>A successful lookup refreshes the credentials' recency and frequency metadata.
     *
     * @param credentials credentials to check, must not be {@code null}
     * @return {@code true} if the cache contains the credentials, otherwise {@code false}
     */
    public boolean contains(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return contains(credentials.username(), credentials.password());
    }

    /**
     * Checks if the cache contains the specified credentials without constructing a
     * {@link Credentials} value. The lookup key does not allocate UTF-8 byte arrays.
     *
     * @param username the username, must not be {@code null}
     * @param password the password, must not be {@code null}
     * @return {@code true} if the cache contains the credentials, otherwise {@code false}
     */
    public boolean contains(String username, String password) {
        Precondition.notNull(username, "username is null");
        Precondition.notNull(password, "password is null");
        return cache.getIfPresent(new CredentialsKey(username, password)) != null;
    }

    /**
     * Removes credentials from the cache.
     *
     * @param credentials credentials to remove, must not be {@code null}
     * @return {@code true} if the credentials were found and removed, otherwise {@code false}
     */
    public boolean remove(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return cache.asMap().remove(new CredentialsKey(credentials.username(), credentials.password())) != null;
    }

    /**
     * Returns the maximum cacheable credential size in bytes.
     *
     * @return the maximum cacheable credential size in bytes
     */
    public int getMaxValueSizeBytes() {
        return maxValueSizeBytes;
    }

    /**
     * Returns the maximum cache weight in bytes.
     *
     * @return the maximum cache weight in bytes
     */
    public long getMaxWeightBytes() {
        return maxWeightBytes;
    }

    /**
     * Returns the approximate maximum number of cached credentials.
     *
     * <p>This is a nominal value computed as {@code maxWeightBytes / maxValueSizeBytes}, rounded
     * down.
     *
     * @return the approximate maximum number of cached credentials
     */
    public int getMaxEntries() {
        return (int) (maxWeightBytes / maxValueSizeBytes);
    }

    /**
     * Returns an approximation of the current number of cached credentials.
     *
     * @return an approximate count of cached credentials
     */
    public int getCurrentEntries() {
        cache.cleanUp();
        return (int) cache.estimatedSize();
    }

    /**
     * Lightweight, allocation-light cache key for credentials.
     *
     * <p>It holds only the username and password strings. Equality is constant-time and does not
     * allocate: the comparison loop always runs for the longer of the two strings and never returns
     * early on a content mismatch. The UTF-8 byte size used by the Caffeine weigher is computed
     * lazily and is only needed when an entry is written.
     */
    private static final class CredentialsKey {

        private final String username;
        private final String password;
        private final int hash;
        private int byteSize = -1;

        CredentialsKey(String username, String password) {
            this.username = username;
            this.password = password;
            this.hash = Objects.hash(username, password);
        }

        int byteSize() {
            int size = byteSize;
            if (size < 0) {
                size = username.getBytes(StandardCharsets.UTF_8).length
                        + password.getBytes(StandardCharsets.UTF_8).length;
                byteSize = size;
            }
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CredentialsKey other = (CredentialsKey) o;
            return constantTimeEquals(username, other.username) & constantTimeEquals(password, other.password);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        private static boolean constantTimeEquals(String a, String b) {
            if (a == b) {
                return true;
            }
            int aLength = a.length();
            int bLength = b.length();
            int result = aLength ^ bLength;
            int max = Math.max(aLength, bLength);
            for (int i = 0; i < max; i++) {
                char aChar = i < aLength ? a.charAt(i) : 0;
                char bChar = i < bLength ? b.charAt(i) : 0;
                result |= aChar ^ bChar;
            }
            return result == 0;
        }
    }
}
