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

package io.prometheus.jmx;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See
 * JmxCollector.Receiver). The cache also retains unmatched entries (a bean name not matching a rule
 * pattern) to avoid matching against the same pattern in later bean collections.
 */
public class MatchedRulesCache {

    private final Map<CacheKey, Entry> cache;

    /**
     * Constructs an empty cache
     */
    public MatchedRulesCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Adds a rule match to the cache
     *
     * @param key the cache key
     * @param matchedRule the matched rule
     */
    public void put(final CacheKey key, final MatchedRule matchedRule) {
        cache.put(key, new Entry(key, matchedRule));
    }

    /**
     * Retrieves the cached MatchedRule
     *
     * @param key the cache key
     * @return a MatchedRule from cache or null
     */
    public MatchedRule get(final CacheKey key) {
        Entry entry = cache.get(key);
        return entry == null ? null : entry.rule;
    }

    /**
     * Retrieves the cache entry for a lookup key. The returned entry exposes the canonical stored
     * key, allowing a cache hit to be marked fresh without allocating a defensive copy of the
     * caller's bean metadata.
     *
     * @param key the lookup key
     * @return the cache entry or null
     */
    Entry getEntry(final CacheKey key) {
        return cache.get(key);
    }

    /**
     * Method to remove stale rules (in the cache but not collected in the last run of the
     * collector)
     *
     * @param stalenessTracker stalenessTracker
     */
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (CacheKey key : cache.keySet()) {
            if (!stalenessTracker.isFresh(key)) {
                cache.remove(key);
            }
        }
    }

    /**
     * Tracks which cache entries were touched during current scrape, so that all that were not can
     * be evicted from the cache
     */
    public static class StalenessTracker {

        private final Set<CacheKey> freshEntries;

        /**
         * Constructor
         */
        public StalenessTracker() {
            this.freshEntries = new HashSet<>();
        }

        /**
         * Marks a cache key as fresh (not stale)
         *
         * @param key the cache key
         */
        public void markAsFresh(final CacheKey key) {
            freshEntries.add(key);
        }

        /**
         * Returns true if {@link #markAsFresh(CacheKey)) was called for that key
         *
         * @param key the cache key
         */
        boolean isFresh(final CacheKey key) {
            return freshEntries.contains(key);
        }

        /**
         * Returns the number of fresh rules
         *
         * @return the number of fresh rules
         */
        public long freshCount() {
            return freshEntries.size();
        }
    }

    /**
     * A cache entry. It keeps the canonical stored key together with the cached rule so a cache hit
     * can be marked fresh using the already-stored key instead of a freshly allocated copy.
     */
    static final class Entry {
        final CacheKey key;
        final MatchedRule rule;

        Entry(final CacheKey key, final MatchedRule rule) {
            this.key = key;
            this.rule = rule;
        }
    }

    /**
     * CacheKey is a key for the cache. It contains the domain, bean properties, attribute keys and
     * attribute name.
     */
    public static class CacheKey {
        private final String domain;
        private final LinkedHashMap<String, String> beanProperties;
        private final List<String> attrKeys;
        private final String attrName;
        private final int cachedHashCode;

        /**
         * Constructor
         *
         * @param domain the domain
         * @param beanProperties the bean properties
         * @param attrKeys the attribute keys
         * @param attrName the attribute name
         */
        public CacheKey(
                String domain, LinkedHashMap<String, String> beanProperties, List<String> attrKeys, String attrName) {
            this(domain, beanProperties, attrKeys, attrName, true);
        }

        private CacheKey(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                List<String> attrKeys,
                String attrName,
                boolean defensiveCopy) {
            this.domain = domain;
            this.beanProperties = defensiveCopy ? new LinkedHashMap<>(beanProperties) : beanProperties;
            this.attrKeys = defensiveCopy ? new ArrayList<>(attrKeys) : attrKeys;
            this.attrName = attrName;
            this.cachedHashCode = Objects.hash(domain, this.beanProperties, this.attrKeys, attrName);
        }

        /**
         * Creates a non-copying lookup key. It references the caller's bean metadata and must only
         * be used transiently (for example, as a cache probe). It must never be stored in the cache
         * or the staleness tracker, because the referenced collections may be mutated by the caller
         * after the lookup.
         *
         * @param domain the domain
         * @param beanProperties the bean properties
         * @param attrKeys the attribute keys
         * @param attrName the attribute name
         * @return a lookup key
         */
        static CacheKey lookup(
                String domain, LinkedHashMap<String, String> beanProperties, List<String> attrKeys, String attrName) {
            return new CacheKey(domain, beanProperties, attrKeys, attrName, false);
        }

        /**
         * Creates a defensive copy of this key suitable for storing in the cache. When this key is
         * already a stored key this is a no-op copy, so mutation of caller-owned collections cannot
         * affect the cache.
         *
         * @return a key with defensively copied bean metadata
         */
        CacheKey storedCopy() {
            return new CacheKey(domain, beanProperties, attrKeys, attrName, true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return Objects.equals(domain, cacheKey.domain)
                    && Objects.equals(beanProperties, cacheKey.beanProperties)
                    && Objects.equals(attrKeys, cacheKey.attrKeys)
                    && Objects.equals(attrName, cacheKey.attrName);
        }

        @Override
        public int hashCode() {
            return cachedHashCode;
        }
    }
}
