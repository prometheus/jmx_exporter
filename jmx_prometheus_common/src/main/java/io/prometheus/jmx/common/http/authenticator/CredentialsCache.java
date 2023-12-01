/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.http.authenticator;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;

/**
 * Class to implement a Credentials cache that is size constrained
 *
 * <p>The cache will purge old entries to make size for a cacheable credential
 *
 * <p>A credential that exceeds maximumCacheSizeBytes is not cached
 */
public class CredentialsCache {

    private final int maximumCacheSizeBytes;
    private final LinkedHashMap<Credentials, Byte> linkedHashMap;
    private final LinkedList<Credentials> linkedList;

    private int currentCacheSizeBytes;

    /**
     * Constructor
     *
     * @param maximumCacheSizeBytes maximum cache size in bytes
     */
    public CredentialsCache(int maximumCacheSizeBytes) {
        this.maximumCacheSizeBytes = maximumCacheSizeBytes;
        linkedHashMap = new LinkedHashMap<>();
        linkedList = new LinkedList<>();
    }

    /**
     * Method to add a Credentials to the cache
     *
     * <p>A credential that exceeds maximumCacheSizeBytes is not cached
     *
     * @param credentials credential
     */
    public synchronized void add(Credentials credentials) {
        int credentialSizeBytes = credentials.toString().getBytes(StandardCharsets.UTF_8).length;

        // Don't cache the entry since it's bigger than the maximum cache size
        // Don't invalidate other entries
        if (credentialSizeBytes > maximumCacheSizeBytes) {
            return;
        }

        // Purge old cache entries until we have space or the cache is empty
        while (((currentCacheSizeBytes + credentialSizeBytes) > maximumCacheSizeBytes)
                && (currentCacheSizeBytes > 0)) {
            Credentials c = linkedList.removeLast();
            linkedHashMap.remove(c);
            currentCacheSizeBytes -= credentialSizeBytes;
            if (currentCacheSizeBytes < 0) {
                currentCacheSizeBytes = 0;
            }
        }

        linkedHashMap.put(credentials, (byte) 1);
        linkedList.addFirst(credentials);
        currentCacheSizeBytes += credentialSizeBytes;
    }

    /**
     * Method to return whether the cache contains the Credentials
     *
     * @param credentials credentials
     * @return true if the set contains the Credential, else false
     */
    public synchronized boolean contains(Credentials credentials) {
        return linkedHashMap.containsKey(credentials);
    }

    /**
     * Method to remove a Credentials from the cache
     *
     * @param credentials credentials
     * @return true if the Credentials existed and was removed, else false
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
     * Method to get the maximum cache size in bytes
     *
     * @return the maximum cache size in bytes
     */
    public int getMaximumCacheSizeBytes() {
        return maximumCacheSizeBytes;
    }

    /**
     * Method to get the current cache size in bytes
     *
     * @return the current cache size in bytes
     */
    public synchronized int getCurrentCacheSizeBytes() {
        return currentCacheSizeBytes;
    }
}
