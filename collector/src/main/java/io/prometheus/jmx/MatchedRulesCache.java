package io.prometheus.jmx;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchedRulesCache is a cache for bean name to configured rule mapping (See JmxCollector.Receiver).
 * The cache also retains unmatched entries (a bean name not matching a rule pattern) to avoid
 * matching against the same pattern in later bean collections.
 */
public class MatchedRulesCache {
    private final Map<Entry, MatchedRule> cachedRules;

    public MatchedRulesCache() {
        this.cachedRules = new ConcurrentHashMap<Entry, MatchedRule>();
    }

    public void clear() {
        cachedRules.clear();
    }

    public void put(final JmxCollector.Rule rule, final String name, final MatchedRule matchedRule) {
        final Entry entry = new Entry(rule, name);
        cachedRules.put(entry, matchedRule);
    }

    public MatchedRule get(final JmxCollector.Rule rule, final String name) {
        final Entry entry = new Entry(rule, name);
        return cachedRules.get(entry);
    }

    // Remove stale rules (in the cache but not collected in the last run of the collector)
    public void evictStaleEntries(Collection<Entry> lastCachedEntries) {
        for (Entry entry : cachedRules.keySet()) {
            if (!lastCachedEntries.contains(entry)) {
                cachedRules.remove(entry);
            }
        }
    }

    public long matchedCount() {
        long count = 0;
        for (MatchedRule matchedRule : cachedRules.values()) {
            if (!matchedRule.isUnmatched()) {
                count++;
            }
        }
        return count;
    }

    public long unmatchedCount() {
        long count = 0;
        for (MatchedRule matchedRule : cachedRules.values()) {
            if (matchedRule.isUnmatched()) {
                count++;
            }
        }
        return count;
    }

    public static class Entry {
        public final JmxCollector.Rule rule;
        public final String name;

        public Entry(final JmxCollector.Rule rule, final String name) {
            this.rule = rule;
            this.name = name;
        }

        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + rule.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof Entry)) {
                return false;
            }

            Entry entry = (Entry) obj;
            return rule.equals(entry.rule) && name.equals(entry.name);
        }
    }
}