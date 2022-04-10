package io.prometheus.jmx;

import io.prometheus.jmx.Config.RuleConfig;

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
public class MatchedRulesCache implements Consumer<Config> {

    private final Map<RuleConfig, Map<String, MatchedRule>> cachedRules = new ConcurrentHashMap<RuleConfig, Map<String, MatchedRule>>();

    public void put(RuleConfig ruleConfig, String cacheKey, MatchedRule matchedRule) {
        Map<String, MatchedRule> cachedRulesForRule = cachedRules.get(ruleConfig);
        cachedRulesForRule.put(cacheKey, matchedRule);
    }

    public MatchedRule get(RuleConfig ruleConfig, final String cacheKey) {
        return cachedRules.get(ruleConfig).get(cacheKey);
    }

    // Remove stale rules (in the cache but not collected in the last run of the collector)
    public void evictStaleEntries(final StalenessTracker stalenessTracker) {
        for (Map.Entry<RuleConfig, Map<String, MatchedRule>> entry : cachedRules.entrySet()) {
            RuleConfig ruleConfig = entry.getKey();
            Map<String, MatchedRule> cachedRulesForRule = entry.getValue();

            for (String cacheKey : cachedRulesForRule.keySet()) {
                if (!stalenessTracker.contains(ruleConfig, cacheKey)) {
                    cachedRulesForRule.remove(cacheKey);
                }
            }
        }
    }

    @Override
    public void accept(Config config) {
        cachedRules.clear();
        for (RuleConfig ruleConfig : config.getRulesOrDefault()) {
            cachedRules.put(ruleConfig, new ConcurrentHashMap<String, MatchedRule>());
        }
    }

    public static class StalenessTracker {
        private final Map<RuleConfig, Set<String>> lastCachedEntries = new HashMap<RuleConfig, Set<String>>();

        public void add(RuleConfig ruleConfig, String cacheKey) {
            Set<String> lastCachedEntriesForRule = lastCachedEntries.get(ruleConfig);
            if (lastCachedEntriesForRule == null) {
                lastCachedEntriesForRule = new HashSet<String>();
                lastCachedEntries.put(ruleConfig, lastCachedEntriesForRule);
            }

            lastCachedEntriesForRule.add(cacheKey);
        }

        public boolean contains(RuleConfig ruleConfig, String cacheKey) {
            Set<String> lastCachedEntriesForRule = lastCachedEntries.get(ruleConfig);
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