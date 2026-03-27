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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MatchedRulesCacheTest {

    private List<String> of(String... strings) {
        ArrayList<String> list = new ArrayList<>();
        Collections.addAll(list, strings);
        return list;
    }

    private MatchedRule createMatchedRule(String name) {
        return new MatchedRule(name, "matchName", "GAUGE", "help", of("l1"), of("v1"), 1.0, 1.0);
    }

    private MatchedRulesCache.CacheKey createCacheKey(String domain) {
        LinkedHashMap<String, String> props = new LinkedHashMap<>();
        props.put("type", "test");
        LinkedList<String> keys = new LinkedList<>();
        keys.add("key1");
        return new MatchedRulesCache.CacheKey(domain, props, keys, "attr");
    }

    @Nested
    class CacheKeyTests {

        @Test
        void cacheKeyEqualsReflexive() {
            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            assertThat(key1).isEqualTo(key1);
        }

        @Test
        void cacheKeyEqualsSymmetric() {
            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain1");
            assertThat(key1).isEqualTo(key2);
            assertThat(key2).isEqualTo(key1);
        }

        @Test
        void cacheKeyEqualsWithDifferentDomain() {
            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain2");
            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        void cacheKeyEqualsWithDifferentAttrName() {
            LinkedHashMap<String, String> props = new LinkedHashMap<>();
            props.put("type", "test");
            LinkedList<String> keys = new LinkedList<>();
            keys.add("key1");

            MatchedRulesCache.CacheKey key1 = new MatchedRulesCache.CacheKey("domain", props, keys, "attr1");
            MatchedRulesCache.CacheKey key2 = new MatchedRulesCache.CacheKey("domain", props, keys, "attr2");

            assertThat(key1).isNotEqualTo(key2);
        }

        @Test
        void cacheKeyHashCodeConsistentWithEquals() {
            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain1");
            assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
        }

        @Test
        void cacheKeyHashCodeDifferentForDifferentObjects() {
            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain2");
            assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
        }

        @Test
        void cacheKeyEqualsWithNull() {
            MatchedRulesCache.CacheKey key = createCacheKey("domain");
            assertThat(key).isNotEqualTo(null);
        }

        @Test
        void cacheKeyEqualsWithDifferentClass() {
            MatchedRulesCache.CacheKey key = createCacheKey("domain");
            assertThat(key).isNotEqualTo("not a CacheKey");
        }

        @Test
        void cacheKeyWithEmptyBeanProperties() {
            LinkedHashMap<String, String> props = new LinkedHashMap<>();
            LinkedList<String> keys = new LinkedList<>();
            keys.add("key1");

            MatchedRulesCache.CacheKey key = new MatchedRulesCache.CacheKey("domain", props, keys, "attr");

            assertThat(key).isNotNull();
        }

        @Test
        void cacheKeyWithEmptyAttrKeys() {
            LinkedHashMap<String, String> props = new LinkedHashMap<>();
            props.put("type", "test");
            LinkedList<String> keys = new LinkedList<>();

            MatchedRulesCache.CacheKey key = new MatchedRulesCache.CacheKey("domain", props, keys, "attr");

            assertThat(key).isNotNull();
        }
    }

    @Nested
    class StalenessTrackerTests {

        @Test
        void markAsFreshMakesEntryFresh() {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();
            MatchedRulesCache.CacheKey key = createCacheKey("domain");

            tracker.markAsFresh(key);

            assertThat(tracker.isFresh(key)).isTrue();
        }

        @Test
        void isFreshReturnsFalseForUnmarkedEntry() {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();
            MatchedRulesCache.CacheKey key = createCacheKey("domain");

            assertThat(tracker.isFresh(key)).isFalse();
        }

        @Test
        void freshCountReturnsCorrectCount() {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            tracker.markAsFresh(createCacheKey("domain1"));
            tracker.markAsFresh(createCacheKey("domain2"));
            tracker.markAsFresh(createCacheKey("domain3"));

            assertThat(tracker.freshCount()).isEqualTo(3);
        }

        @Test
        void freshCountReturnsZeroForEmptyTracker() {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            assertThat(tracker.freshCount()).isEqualTo(0);
        }

        @Test
        void sameKeyMarkedTwiceCountsOnce() {
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();
            MatchedRulesCache.CacheKey key = createCacheKey("domain1");

            tracker.markAsFresh(key);
            tracker.markAsFresh(key);

            assertThat(tracker.freshCount()).isEqualTo(1);
        }
    }

    @Nested
    class MatchedRulesCacheTests {

        @Test
        void putAndGetReturnsSameRule() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.CacheKey key = createCacheKey("domain");
            MatchedRule rule = createMatchedRule("test_name");

            cache.put(key, rule);
            MatchedRule retrieved = cache.get(key);

            assertThat(retrieved).isSameAs(rule);
        }

        @Test
        void getReturnsNullForMissingKey() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.CacheKey key = createCacheKey("nonexistent");

            MatchedRule result = cache.get(key);

            assertThat(result).isNull();
        }

        @Test
        void getWithNullKeyThrowsNPE() {
            MatchedRulesCache cache = new MatchedRulesCache();

            assertThatThrownBy(() -> cache.get(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void putWithNullKeyThrowsNPE() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRule rule = createMatchedRule("test_name");

            assertThatThrownBy(() -> cache.put(null, rule)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void evictStaleEntriesRemovesStaleEntries() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain2");

            MatchedRule rule1 = createMatchedRule("name1");
            MatchedRule rule2 = createMatchedRule("name2");

            cache.put(key1, rule1);
            cache.put(key2, rule2);

            tracker.markAsFresh(key1);

            cache.evictStaleEntries(tracker);

            assertThat(cache.get(key1)).isSameAs(rule1);
            assertThat(cache.get(key2)).isNull();
        }

        @Test
        void evictStaleEntriesKeepsFreshEntries() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            MatchedRulesCache.CacheKey key1 = createCacheKey("domain1");
            MatchedRulesCache.CacheKey key2 = createCacheKey("domain2");

            MatchedRule rule1 = createMatchedRule("name1");
            MatchedRule rule2 = createMatchedRule("name2");

            cache.put(key1, rule1);
            cache.put(key2, rule2);

            tracker.markAsFresh(key1);
            tracker.markAsFresh(key2);

            cache.evictStaleEntries(tracker);

            assertThat(cache.get(key1)).isSameAs(rule1);
            assertThat(cache.get(key2)).isSameAs(rule2);
        }

        @Test
        void evictStaleEntriesWithEmptyCache() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            tracker.markAsFresh(createCacheKey("domain"));

            cache.evictStaleEntries(tracker);
        }

        @Test
        void evictStaleEntriesWithEmptyTracker() {
            MatchedRulesCache cache = new MatchedRulesCache();

            MatchedRulesCache.CacheKey key = createCacheKey("domain1");
            MatchedRule rule = createMatchedRule("name1");

            cache.put(key, rule);

            MatchedRulesCache.StalenessTracker tracker = new MatchedRulesCache.StalenessTracker();

            cache.evictStaleEntries(tracker);

            assertThat(cache.get(key)).isNull();
        }

        @Test
        void multiplePutsOverwritePreviousValue() {
            MatchedRulesCache cache = new MatchedRulesCache();
            MatchedRulesCache.CacheKey key = createCacheKey("domain");

            MatchedRule rule1 = createMatchedRule("name1");
            MatchedRule rule2 = createMatchedRule("name2");

            cache.put(key, rule1);
            cache.put(key, rule2);

            assertThat(cache.get(key)).isSameAs(rule2);
        }
    }
}
