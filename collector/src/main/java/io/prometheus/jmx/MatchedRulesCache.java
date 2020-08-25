package io.prometheus.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See JmxCollector.Receiver).
 * The cache also retains unmatched entries (a bean name not matching a rule pattern) to avoid
 * matching against the same pattern in later bean collections.
 */
public class MatchedRulesCache {
    private final Map<JmxCollector.Rule, Map<String, MatchedRule>> cachedRules;

    public MatchedRulesCache(Collection<JmxCollector.Rule> rules) {
        this.cachedRules = new HashMap<JmxCollector.Rule, Map<String, MatchedRule>>(rules.size());
        for (JmxCollector.Rule rule : rules) {
            this.cachedRules.put(rule, new ConcurrentHashMap<String, MatchedRule>());
        }
    }

    public void put(final JmxCollector.Rule rule, final String cacheKey, final MatchedRule matchedRule) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(rule);
        cachedRulesForRule.put(cacheKey, matchedRule);
    }

    public MatchedRule get(final JmxCollector.Rule rule, final String cacheKey) {
        return cachedRules.get(rule).get(cacheKey);
    }

    // Remove stale rules (in the cache but not collected in the last run of the collector)
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (Map.Entry<JmxCollector.Rule, Map<String, MatchedRule>> entry : cachedRules.entrySet()) {
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
        private final Map<JmxCollector.Rule, Set<String>> lastCachedEntries = new HashMap<JmxCollector.Rule, Set<String>>();

        public void add(final JmxCollector.Rule rule, final String cacheKey) {
            Set<String> lastCachedEntriesForRule = lastCachedEntries.get(rule);
            if (lastCachedEntriesForRule == null) {
                lastCachedEntriesForRule = new HashSet<String>();
                lastCachedEntries.put(rule, lastCachedEntriesForRule);
            }

            lastCachedEntriesForRule.add(cacheKey);
        }

        public boolean contains(final JmxCollector.Rule rule, final String cacheKey) {
            Set<String> lastCachedEntriesForRule = lastCachedEntries.get(rule);
            return (lastCachedEntriesForRule != null) && lastCachedEntriesForRule.contains(cacheKey);
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