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
import java.time.Duration;

/**
 * A thread-safe bounded cache for credentials backed by Caffeine.
 *
 * <p>The cache evicts entries based on a maximum weight in bytes, using the UTF-8 encoded byte size
 * of the credential's string representation as the weight. Entries larger than the configured
 * maximum value size are not cached.
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
    private static final Boolean INVALID = Boolean.FALSE;

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
    private final Cache<Credentials, Boolean> cache;

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
    @SuppressWarnings("unchecked")
    public CredentialsCache(int maxValueSizeBytes, long maxWeightBytes, Duration ttl) {
        Precondition.isGreaterThanOrEqualTo(maxValueSizeBytes, 1);
        Precondition.isGreaterThanOrEqualTo((int) Math.min(maxWeightBytes, Integer.MAX_VALUE), 1);

        this.maxValueSizeBytes = maxValueSizeBytes;
        this.maxWeightBytes = maxWeightBytes;

        Caffeine<Credentials, Boolean> builder = Caffeine.newBuilder()
                .maximumWeight(maxWeightBytes)
                .weigher((Credentials credentials, Boolean ignored) -> credentials.byteSize());

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

        if (credentials.byteSize() > maxValueSizeBytes) {
            return;
        }

        cache.put(credentials, VALID);
    }

    /**
     * Adds credentials to the cache as invalid.
     *
     * <p>If the credentials size exceeds the maximum value size, they are not cached. If the
     * credentials are already present, they are refreshed as a recently used entry.
     *
     * @param credentials credentials to add as invalid, must not be {@code null}
     */
    public void addInvalid(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");

        if (credentials.byteSize() > maxValueSizeBytes) {
            return;
        }

        cache.put(credentials, INVALID);
    }

    /**
     * Returns the cached status of the specified credentials.
     *
     * @param credentials credentials to look up, must not be {@code null}
     * @return {@code Boolean.TRUE} if valid, {@code Boolean.FALSE} if invalid, or {@code null} if
     *     not cached
     */
    public Boolean get(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return cache.getIfPresent(credentials);
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
        return cache.getIfPresent(credentials) != null;
    }

    /**
     * Removes credentials from the cache.
     *
     * @param credentials credentials to remove, must not be {@code null}
     * @return {@code true} if the credentials were found and removed, otherwise {@code false}
     */
    public boolean remove(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return cache.asMap().remove(credentials) != null;
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
}
