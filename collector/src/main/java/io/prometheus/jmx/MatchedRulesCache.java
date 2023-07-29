/*
 * Copyright (C) 2020-2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See
 * JmxCollector.Receiver). The cache also retains unmatched entries (a bean name not matching a rule
 * pattern) to avoid matching against the same pattern in later bean collections.
 */
public class MatchedRulesCache {
    private final Map<JmxCollector.Rule, Map<String, MatchedRule>> cachedRules;

    public MatchedRulesCache(Collection<JmxCollector.Rule> rules) {
        this.cachedRules = new HashMap<>(rules.size());
        for (JmxCollector.Rule rule : rules) {
            this.cachedRules.put(rule, new ConcurrentHashMap<>());
        }
    }

    public void put(
            final JmxCollector.Rule rule, final String cacheKey, final MatchedRule matchedRule) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(rule);
        cachedRulesForRule.put(cacheKey, matchedRule);
    }

    public MatchedRule get(final JmxCollector.Rule rule, final String cacheKey) {
        return cachedRules.get(rule).get(cacheKey);
    }

    // Remove stale rules (in the cache but not collected in the last run of the collector)
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (Map.Entry<JmxCollector.Rule, Map<String, MatchedRule>> entry :
                cachedRules.entrySet()) {
            JmxCollector.Rule rule = entry.getKey();
            Map<String, MatchedRule> cachedRulesForRule = entry.getValue();

            for (String cacheKey : cachedRulesForRule.keySet()) {
                if (!stalenessTracker.contains(rule, cacheKey)) {
                    cachedRulesForRule.remove(cacheKey);
                }
            }
        }
    }

    public static class StalenessTracker {
        private final Map<JmxCollector.Rule, Set<String>> lastCachedEntries = new HashMap<>();

        public void add(final JmxCollector.Rule rule, final String cacheKey) {
            Set<String> lastCachedEntriesForRule =
                    lastCachedEntries.computeIfAbsent(rule, k -> new HashSet<>());
            lastCachedEntriesForRule.add(cacheKey);
        }

        public boolean contains(final JmxCollector.Rule rule, final String cacheKey) {
            Set<String> lastCachedEntriesForRule = lastCachedEntries.get(rule);
            return (lastCachedEntriesForRule != null)
                    && lastCachedEntriesForRule.contains(cacheKey);
        }

        public long cachedCount() {
            long count = 0;
            for (Set<String> cacheKeys : lastCachedEntries.values()) {
                count += cacheKeys.size();
            }
            return count;
        }
    }
}
