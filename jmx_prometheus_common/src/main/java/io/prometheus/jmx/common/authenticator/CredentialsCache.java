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

import io.prometheus.jmx.common.util.Precondition;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * LRU cache for credentials.
 *
 * <p>Provides caching for both valid and invalid credentials to improve authentication
 * performance.
 *
 * <p>Entries larger than the configured maximum value size are not cached. Once the cache reaches
 * the configured maximum number of entries, the least recently used entry is evicted.
 *
 * <p>Thread-safety: This class is thread-safe. All public methods are synchronized.
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

    private static final Byte PRESENT = (byte) 1;

    /**
     * Maximum cacheable credential size in bytes.
     */
    private final int maxValueSizeBytes;

    /**
     * Maximum number of cached credentials.
     */
    private final int maxEntries;

    /**
     * LRU cache for credential lookups.
     *
     * <p>Uses access-order to implement classic LRU semantics.
     */
    private final LinkedHashMap<Credentials, Byte> cache;

    /**
     * Constructs a credentials cache with the specified limits.
     *
     * @param maxValueSizeBytes maximum size of a single cached credential value in bytes, must be
     *     positive
     * @param maxEntries maximum number of cached credentials, must be positive
     */
    public CredentialsCache(int maxValueSizeBytes, int maxEntries) {
        Precondition.isGreaterThanOrEqualTo(maxValueSizeBytes, 1);
        Precondition.isGreaterThanOrEqualTo(maxEntries, 1);

        this.maxValueSizeBytes = maxValueSizeBytes;
        this.maxEntries = maxEntries;
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    /**
     * Adds credentials to the cache.
     *
     * <p>If the credentials size exceeds the maximum value size, they are not cached. If the
     * credentials are already present, they are refreshed as the most recently used entry.
     * Otherwise, the least recently used entries are evicted until there is room for the new
     * entry.
     *
     * @param credentials credentials to add, must not be {@code null}
     */
    public synchronized void add(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");

        if (calculateSizeBytes(credentials) > maxValueSizeBytes) {
            return;
        }

        if (cache.get(credentials) != null) {
            return;
        }

        while (cache.size() >= maxEntries) {
            evictLeastRecentlyUsed();
        }

        cache.put(credentials, PRESENT);
    }

    /**
     * Checks if the cache contains the specified credentials.
     *
     * <p>A successful lookup refreshes the credentials as the most recently used entry.
     *
     * @param credentials credentials to check, must not be {@code null}
     * @return {@code true} if the cache contains the credentials, otherwise {@code false}
     */
    public synchronized boolean contains(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return cache.get(credentials) != null;
    }

    /**
     * Removes credentials from the cache.
     *
     * @param credentials credentials to remove, must not be {@code null}
     * @return {@code true} if the credentials were found and removed, otherwise {@code false}
     */
    public synchronized boolean remove(Credentials credentials) {
        Precondition.notNull(credentials, "credentials is null");
        return cache.remove(credentials) != null;
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
     * Returns the maximum number of cached credentials.
     *
     * @return the maximum number of cached credentials
     */
    public int getMaxEntries() {
        return maxEntries;
    }

    /**
     * Returns the current number of cached credentials.
     *
     * @return the current number of cached credentials
     */
    public synchronized int getCurrentEntries() {
        return cache.size();
    }

    /**
     * Calculates the UTF-8 encoded byte size of the credentials' string representation.
     *
     * @param credentials the credentials to measure, must not be {@code null}
     * @return the byte size of the credentials when encoded as UTF-8
     */
    private static int calculateSizeBytes(Credentials credentials) {
        return credentials.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    /**
     * Evicts the least recently used entry from the cache.
     *
     * <p>Relies on the access-order iteration of {@link LinkedHashMap} to remove the oldest entry.
     */
    private void evictLeastRecentlyUsed() {
        Iterator<Credentials> iterator = cache.keySet().iterator();
        if (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }
}
