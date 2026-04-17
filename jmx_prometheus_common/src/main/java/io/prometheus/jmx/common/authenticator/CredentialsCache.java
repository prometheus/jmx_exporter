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
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Size-constrained LRU cache for credentials.
 *
 * <p>Provides caching for both valid and invalid credentials to improve authentication performance.
 * When the cache reaches its maximum size, the oldest entries are evicted to make room for new
 * ones.
 *
 * <p>Credentials that exceed the maximum cache size are not cached. This prevents a single large
 * credential from evicting multiple smaller entries.
 *
 * <p>Thread-safety: This class is thread-safe. All public methods are synchronized.
 */
public class CredentialsCache {

    /**
     * Maximum cache size in bytes.
     */
    private final int maximumCacheSizeBytes;

    /**
     * LRU cache for credential lookups.
     *
     * <p>Uses LinkedHashMap with access-order to implement LRU eviction.
     */
    private final LinkedHashMap<Credentials, Byte> linkedHashMap;

    /**
     * LinkedList for maintaining LRU order.
     *
     * <p>The most recently used credentials are at the head, least recently used at the tail.
     */
    private final LinkedList<Credentials> linkedList;

    /**
     * Current cache size in bytes.
     */
    private int currentCacheSizeBytes;

    /**
     * Constructs a credentials cache with the specified maximum size.
     *
     * @param maximumCacheSizeBytes maximum cache size in bytes, must be positive
     */
    public CredentialsCache(int maximumCacheSizeBytes) {
        this.maximumCacheSizeBytes = maximumCacheSizeBytes;
        linkedHashMap = new LinkedHashMap<>();
        linkedList = new LinkedList<>();
    }

    /**
     * Adds credentials to the cache.
     *
     * <p>If the credentials size exceeds the maximum cache size, they are not cached. If adding
     * the credentials would exceed the cache size, older entries are evicted until there is
     * room.
     *
     * <p>This operation is synchronized to ensure thread-safe access to the underlying
     * collections.
     *
     * @param credentials credentials to add, must not be {@code null}
     */
    public synchronized void add(Credentials credentials) {
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;

        // Don't cache the entry since it's bigger than the maximum cache size
        // Don't invalidate other entries
        if (credentialSizeBytes > maximumCacheSizeBytes) {
            return;
        }

        // Purge old cache entries until we have space or the cache is empty
        while (((currentCacheSizeBytes + credentialSizeBytes) > maximumCacheSizeBytes) && (currentCacheSizeBytes > 0)) {
            Credentials c = linkedList.removeLast();
            linkedHashMap.remove(c);
            currentCacheSizeBytes -= c.toString().getBytes(StandardCharsets.UTF_8).length;
        }

        linkedHashMap.put(credentials, (byte) 1);
        linkedList.addFirst(credentials);
        currentCacheSizeBytes += credentialSizeBytes;
    }

    /**
     * Checks if the cache contains the specified credentials.
     *
     * <p>This operation is synchronized to ensure thread-safe access to the underlying
     * collection.
     *
     * @param credentials credentials to check, must not be {@code null}
     * @return {@code true} if the cache contains the credentials, {@code false} otherwise
     */
    public synchronized boolean contains(Credentials credentials) {
        return linkedHashMap.containsKey(credentials);
    }

    /**
     * Removes credentials from the cache.
     *
     * <p>This operation is synchronized to ensure thread-safe access to the underlying
     * collections.
     *
     * @param credentials credentials to remove, must not be {@code null}
     * @return {@code true} if the credentials were found and removed, {@code false} if they
     *     were not found
     */
    public synchronized boolean remove(Credentials credentials) {
        if (linkedHashMap.remove(credentials) != null) {
            linkedList.remove(credentials);
            currentCacheSizeBytes -= credentials.toString().getBytes(StandardCharsets.UTF_8).length;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the maximum cache size in bytes.
     *
     * @return the maximum cache size
     */
    public int getMaximumCacheSizeBytes() {
        return maximumCacheSizeBytes;
    }

    /**
     * Returns the current cache size in bytes.
     *
     * <p>This operation is synchronized to ensure thread-safe access to the cache size counter.
     *
     * @return the current cache size
     */
    public synchronized int getCurrentCacheSizeBytes() {
        return currentCacheSizeBytes;
    }
}
