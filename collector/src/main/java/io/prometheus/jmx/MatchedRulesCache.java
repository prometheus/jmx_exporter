package io.prometheus.jmx;

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

    public MatchedRulesCache() {
        this.cachedRules = new ConcurrentHashMap<JmxCollector.Rule, Map<String, MatchedRule>>();
    }

    public void clear() {
        cachedRules.clear();
    }

    public void put(final JmxCollector.Rule rule, final String cacheKey, final MatchedRule matchedRule) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(rule);
        if (cachedRulesForRule == null) {
            synchronized (cachedRules) {
                cachedRulesForRule = cachedRules.get(rule);
                if (cachedRulesForRule == null) {
                    cachedRulesForRule = new ConcurrentHashMap<String, MatchedRule>();
                    cachedRules.put(rule, cachedRulesForRule);
                }
            }
        }

        cachedRulesForRule.put(cacheKey, matchedRule);
    }

    public MatchedRule get(final JmxCollector.Rule rule, final String cacheKey) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(rule);
        return (cachedRulesForRule == null) ? null : cachedRulesForRule.get(cacheKey);
    }

    // Remove stale rules (in the cache but not collected in the last run of the collector)
    public void evictStaleEntries(final LastEntries lastEntries) {
        for (Map.Entry<JmxCollector.Rule, Map<String, MatchedRule>> entry : cachedRules.entrySet()) {
            JmxCollector.Rule rule = entry.getKey();
            Map<String, MatchedRule> cachedRulesForRule = entry.getValue();

            for (String cacheKey : cachedRulesForRule.keySet()) {
                if (!lastEntries.contains(rule, cacheKey)) {
                    cachedRulesForRule.remove(cacheKey);
                }
            }

            if (cachedRulesForRule.size() == 0) {
                cachedRules.remove(rule);
            }
        }
    }

    public long matchedCount() {
        long count = 0;
        for (Map<String, MatchedRule> matchedRules : cachedRules.values()) {
            for (MatchedRule matchedRule : matchedRules.values()) {
                if (matchedRule.isUnmatched()) {
                    count++;
                }
            }
        }
        return count;
    }

    public long unmatchedCount() {
        long count = 0;
        for (Map<String, MatchedRule> matchedRules : cachedRules.values()) {
            for (MatchedRule matchedRule : matchedRules.values()) {
                if (matchedRule.isUnmatched()) {
                    count++;
                }
            }
        }
        return count;
    }

    public static class LastEntries {
        final Map<JmxCollector.Rule, Set<String>> lastCachedEntries = new HashMap<JmxCollector.Rule, Set<String>>();

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
    }
}