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

import static java.lang.String.format;

import io.prometheus.jmx.MatchedRulesCache.CacheKey;
import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import io.prometheus.jmx.variable.VariableResolver;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.Unit;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.yaml.snakeyaml.Yaml;

/** Class to implement JmxCollector */
@SuppressWarnings("unchecked")
public class JmxCollector implements MultiCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxCollector.class);

    /** Enum to implement Mode */
    public enum Mode {
        /** Agent mode */
        AGENT,
        /** Standalone mode */
        STANDALONE
    }

    private final Mode mode;

    /** Class to implement ExtraMetric */
    static class ExtraMetric {
        String name;
        Object value;
        String description;
    }

    /** Class to implement Rule */
    static class Rule {
        Pattern pattern;
        String name;
        String value;
        Double valueFactor = 1.0;
        String help;
        boolean attrNameSnakeCase;
        boolean cache = false;
        String type = "UNKNOWN";
        ArrayList<String> labelNames;
        ArrayList<String> labelValues;
    }

    /** Class to implement MetricCustomizer */
    public static class MetricCustomizer {
        MBeanFilter mbeanFilter;
        List<String> attributesAsLabels;
        List<ExtraMetric> extraMetrics;

        /** Constructor */
        public MetricCustomizer() {
            // INTENTIONALLY BLANK
        }
    }

    /** Class to implement MBeanFilter */
    public static class MBeanFilter {
        String domain;
        Map<String, String> properties;

        /** Constructor */
        public MBeanFilter() {
            // INTENTIONALLY BLANK
        }
    }

    /** Class to implement Config */
    private static class Config {
        Integer startDelaySeconds = 0;
        String jmxUrl = "";
        String username = "";
        String password = "";
        boolean ssl = false;
        boolean lowercaseOutputName;
        boolean lowercaseOutputLabelNames;
        boolean inferCounterTypeFromName;
        final List<ObjectName> includeObjectNames = new ArrayList<>();
        final List<ObjectName> excludeObjectNames = new ArrayList<>();
        ObjectNameAttributeFilter objectNameAttributeFilter;
        final List<Rule> rules = new ArrayList<>();
        long lastUpdate = 0L;
        List<MetricCustomizer> metricCustomizers = new ArrayList<>();
        MatchedRulesCache rulesCache;
    }

    private Config config;
    private File configFile;
    private final long createTimeNanoSecs = System.nanoTime();

    private Counter configReloadSuccess;
    private Counter configReloadFailure;
    private Gauge jmxScrapeDurationSeconds;
    private Gauge jmxScrapeError;
    private Gauge jmxScrapeCachedBeans;

    private final JmxMBeanPropertyCache jmxMBeanPropertyCache = new JmxMBeanPropertyCache();

    /**
     * Constructor
     *
     * @param in in
     * @throws IOException IOException
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(File in) throws IOException, MalformedObjectNameException {
        this(in, null);
    }

    /**
     * Constructor
     *
     * @param in in
     * @param mode mode
     * @throws IOException IOException
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(File in, Mode mode) throws IOException, MalformedObjectNameException {
        configFile = in;
        this.mode = mode;
        config = loadConfig(new Yaml().load(new FileReader(in)));
        config.lastUpdate = configFile.lastModified();
        exitOnConfigError();
    }

    /**
     * Constructor
     *
     * @param yamlConfig yamlConfig
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(String yamlConfig) throws MalformedObjectNameException {
        config = loadConfig(new Yaml().load(yamlConfig));
        mode = null;
    }

    /**
     * Constructor
     *
     * @param inputStream inputStream
     * @throws MalformedObjectNameException MalformedObjectNameException
     */
    public JmxCollector(InputStream inputStream) throws MalformedObjectNameException {
        config = loadConfig(new Yaml().load(inputStream));
        mode = null;
    }

    /**
     * Method to register the JmxCollector
     *
     * @return the JmxCollector
     */
    public JmxCollector register() {
        return register(PrometheusRegistry.defaultRegistry);
    }

    /**
     * Method to register the JmxCollector
     *
     * @param prometheusRegistry prometheusRegistry
     * @return the JmxCollector
     */
    public JmxCollector register(PrometheusRegistry prometheusRegistry) {
        configReloadSuccess =
                Counter.builder()
                        .name("jmx_config_reload_success_total")
                        .help("Number of times configuration have successfully been reloaded.")
                        .register(prometheusRegistry);

        configReloadFailure =
                Counter.builder()
                        .name("jmx_config_reload_failure_total")
                        .help("Number of times configuration have failed to be reloaded.")
                        .register(prometheusRegistry);

        jmxScrapeDurationSeconds =
                Gauge.builder()
                        .name("jmx_scrape_duration_seconds")
                        .help("Time this JMX scrape took, in seconds.")
                        .unit(Unit.SECONDS)
                        .register(prometheusRegistry);

        jmxScrapeError =
                Gauge.builder()
                        .name("jmx_scrape_error")
                        .help("Non-zero if this scrape failed.")
                        .register(prometheusRegistry);

        jmxScrapeCachedBeans =
                Gauge.builder()
                        .name("jmx_scrape_cached_beans")
                        .help("Number of beans with their matching rule cached")
                        .register(prometheusRegistry);

        prometheusRegistry.register(this);

        return this;
    }

    private void exitOnConfigError() {
        if (mode == Mode.AGENT && !config.jmxUrl.isEmpty()) {
            LOGGER.error(
                    "Configuration error: When running jmx_exporter as a Java agent, you must not"
                        + " configure 'jmxUrl' or 'hostPort' because you don't want to monitor a"
                        + " remote JVM.");
            System.exit(-1);
        }
        if (mode == Mode.STANDALONE && config.jmxUrl.isEmpty()) {
            LOGGER.error(
                    "Configuration error: When running jmx_exporter in standalone mode (using"
                            + " jmx_prometheus_standalone-*.jar) you must configure 'jmxUrl' or"
                            + " 'hostPort'.");
            System.exit(-1);
        }
    }

    private void reloadConfig() {
        try (FileReader fr = new FileReader(configFile)) {
            Map<String, Object> newYamlConfig = new Yaml().load(fr);
            Config newConfig = loadConfig(newYamlConfig);
            newConfig.lastUpdate = configFile.lastModified();
            config = newConfig;
            configReloadSuccess.inc();
        } catch (Exception e) {
            LOGGER.error("Configuration reload failed: %s: ", e);
            configReloadFailure.inc();
        }
    }

    private synchronized Config getLatestConfig() {
        if (configFile != null) {
            long lastModified = configFile.lastModified();
            if (lastModified > config.lastUpdate) {
                LOGGER.trace("Configuration file changed, reloading...");
                reloadConfig();
            }
        }
        exitOnConfigError();
        return config;
    }

    private Config loadConfig(Map<String, Object> yamlConfig) throws MalformedObjectNameException {
        Config cfg = new Config();

        if (yamlConfig == null) { // Yaml config empty, set config to empty map.
            yamlConfig = new HashMap<>();
        }

        if (yamlConfig.containsKey("startDelaySeconds")) {
            try {
                cfg.startDelaySeconds = (Integer) yamlConfig.get("startDelaySeconds");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Invalid number provided for startDelaySeconds", e);
            }
        }
        if (yamlConfig.containsKey("hostPort")) {
            if (yamlConfig.containsKey("jmxUrl")) {
                throw new IllegalArgumentException(
                        "At most one of hostPort and jmxUrl must be provided");
            }
            cfg.jmxUrl = "service:jmx:rmi:///jndi/rmi://" + yamlConfig.get("hostPort") + "/jmxrmi";
        } else if (yamlConfig.containsKey("jmxUrl")) {
            cfg.jmxUrl = (String) yamlConfig.get("jmxUrl");
        }

        if (yamlConfig.containsKey("username")) {
            String username = (String) yamlConfig.get("username");
            cfg.username = VariableResolver.resolveVariable(username);
        }

        if (yamlConfig.containsKey("password")) {
            String password = (String) yamlConfig.get("password");
            cfg.password = VariableResolver.resolveVariable(password);
        }

        if (yamlConfig.containsKey("ssl")) {
            cfg.ssl = (Boolean) yamlConfig.get("ssl");
        }

        if (yamlConfig.containsKey("lowercaseOutputName")) {
            cfg.lowercaseOutputName = (Boolean) yamlConfig.get("lowercaseOutputName");
        }

        if (yamlConfig.containsKey("lowercaseOutputLabelNames")) {
            cfg.lowercaseOutputLabelNames = (Boolean) yamlConfig.get("lowercaseOutputLabelNames");
        }

        if (yamlConfig.containsKey("inferCounterTypeFromName")) {
            cfg.inferCounterTypeFromName = (Boolean) yamlConfig.get("inferCounterTypeFromName");
        }

        // Default to includeObjectNames, but fall back to whitelistObjectNames for backward
        // compatibility
        if (yamlConfig.containsKey("includeObjectNames")) {
            List<Object> names = (List<Object>) yamlConfig.get("includeObjectNames");
            for (Object name : names) {
                cfg.includeObjectNames.add(new ObjectName((String) name));
            }
        } else if (yamlConfig.containsKey("whitelistObjectNames")) {
            List<Object> names = (List<Object>) yamlConfig.get("whitelistObjectNames");
            for (Object name : names) {
                cfg.includeObjectNames.add(new ObjectName((String) name));
            }
        } else {
            cfg.includeObjectNames.add(null);
        }

        // Default to excludeObjectNames, but fall back to blacklistObjectNames for backward
        // compatibility
        if (yamlConfig.containsKey("excludeObjectNames")) {
            List<Object> names = (List<Object>) yamlConfig.get("excludeObjectNames");
            for (Object name : names) {
                cfg.excludeObjectNames.add(new ObjectName((String) name));
            }
        } else if (yamlConfig.containsKey("blacklistObjectNames")) {
            List<Object> names = (List<Object>) yamlConfig.get("blacklistObjectNames");
            for (Object name : names) {
                cfg.excludeObjectNames.add(new ObjectName((String) name));
            }
        }

        // Default ObjectNames to exclude if excludeJvmMetrics is true
        if (yamlConfig.containsKey("excludeJvmMetrics")) {
            Boolean excludeJvmMetrics = (Boolean) yamlConfig.get("excludeJvmMetrics");
            if (excludeJvmMetrics != null && excludeJvmMetrics) {
                cfg.excludeObjectNames.add(new ObjectName("com.sun.management:*"));
                cfg.excludeObjectNames.add(new ObjectName("com.sun.management.jmxremote:*"));
                cfg.excludeObjectNames.add(new ObjectName("java.lang:*"));
                cfg.excludeObjectNames.add(new ObjectName("java.nio:*"));
                cfg.excludeObjectNames.add(new ObjectName("java.util.logging:*"));
                cfg.excludeObjectNames.add(new ObjectName("javax.management:*"));
                cfg.excludeObjectNames.add(new ObjectName("javax.management.remote:*"));
                cfg.excludeObjectNames.add(new ObjectName("jdk.internal:*"));
                cfg.excludeObjectNames.add(new ObjectName("jdk.management:*"));
                cfg.excludeObjectNames.add(new ObjectName("jdk.management.jfr:*"));
                cfg.excludeObjectNames.add(new ObjectName("sun.management:*"));
            }
        }

        if (yamlConfig.containsKey("metricCustomizers")) {
            List<Map<String, Object>> metricCustomizersYaml =
                    (List<Map<String, Object>>) yamlConfig.get("metricCustomizers");
            if (metricCustomizersYaml != null) {
                for (Map<String, Object> metricCustomizerYaml : metricCustomizersYaml) {
                    Map<String, Object> mbeanFilterYaml =
                            (Map<String, Object>) metricCustomizerYaml.get("mbeanFilter");
                    if (mbeanFilterYaml == null) {
                        throw new IllegalArgumentException(
                                "Must provide mbeanFilter, if metricCustomizers is given: "
                                        + metricCustomizersYaml);
                    }
                    MBeanFilter mbeanFilter = new MBeanFilter();
                    mbeanFilter.domain = (String) mbeanFilterYaml.get("domain");
                    if (mbeanFilter.domain == null) {
                        throw new IllegalArgumentException(
                                "Must provide domain, if metricCustomizers is given: "
                                        + metricCustomizersYaml);
                    }
                    mbeanFilter.properties =
                            (Map<String, String>)
                                    mbeanFilterYaml.getOrDefault("properties", new HashMap<>());

                    List<String> attributesAsLabelsYaml =
                            (List<String>) metricCustomizerYaml.get("attributesAsLabels");
                    List<Map<String, Object>> extraMetricsYaml =
                            (List<Map<String, Object>>) metricCustomizerYaml.get("extraMetrics");
                    if (attributesAsLabelsYaml == null && extraMetricsYaml == null) {
                        throw new IllegalArgumentException(
                                "Must provide attributesAsLabels or extraMetrics, if"
                                        + " metricCustomizers is given: "
                                        + metricCustomizersYaml);
                    }
                    MetricCustomizer metricCustomizer = new MetricCustomizer();
                    metricCustomizer.mbeanFilter = mbeanFilter;
                    metricCustomizer.attributesAsLabels = attributesAsLabelsYaml;

                    if (extraMetricsYaml != null) {
                        List<ExtraMetric> extraMetrics = new ArrayList<>();
                        for (Map<String, Object> extraMetricYaml : extraMetricsYaml) {
                            ExtraMetric extraMetric = new ExtraMetric();
                            extraMetric.name = (String) extraMetricYaml.get("name");
                            if (extraMetric.name == null) {
                                throw new IllegalArgumentException(
                                        "Must provide name, if extraMetric is given: "
                                                + extraMetricsYaml);
                            }
                            extraMetric.value = extraMetricYaml.get("value");
                            if (extraMetric.value == null) {
                                throw new IllegalArgumentException(
                                        "Must provide value, if extraMetric is given: "
                                                + extraMetricsYaml);
                            }
                            extraMetric.description = (String) extraMetricYaml.get("description");
                            extraMetrics.add(extraMetric);
                        }
                        metricCustomizer.extraMetrics = extraMetrics;
                    }
                    cfg.metricCustomizers.add(metricCustomizer);
                }
            } else {
                throw new IllegalArgumentException(
                        "Must provide mbeanFilter, if metricCustomizers is given ");
            }
        }

        if (yamlConfig.containsKey("rules")) {
            List<Map<String, Object>> configRules =
                    (List<Map<String, Object>>) yamlConfig.get("rules");
            for (Map<String, Object> yamlRule : configRules) {
                Rule rule = new Rule();
                cfg.rules.add(rule);
                if (yamlRule.containsKey("pattern")) {
                    rule.pattern = Pattern.compile("^.*(?:" + yamlRule.get("pattern") + ").*$");
                }
                if (yamlRule.containsKey("name")) {
                    rule.name = (String) yamlRule.get("name");
                }
                if (yamlRule.containsKey("value")) {
                    rule.value = String.valueOf(yamlRule.get("value"));
                }
                if (yamlRule.containsKey("valueFactor")) {
                    String valueFactor = String.valueOf(yamlRule.get("valueFactor"));
                    try {
                        rule.valueFactor = Double.valueOf(valueFactor);
                    } catch (NumberFormatException e) {
                        // use default value
                    }
                }
                if (yamlRule.containsKey("attrNameSnakeCase")) {
                    rule.attrNameSnakeCase = (Boolean) yamlRule.get("attrNameSnakeCase");
                }
                if (yamlRule.containsKey("cache")) {
                    rule.cache = (Boolean) yamlRule.get("cache");
                }
                if (yamlRule.containsKey("type")) {
                    String t = (String) yamlRule.get("type");
                    // Gracefully handle switch to OM data model.
                    if ("UNTYPED".equals(t)) {
                        t = "UNKNOWN";
                    }
                    rule.type = t;
                }
                if (yamlRule.containsKey("help")) {
                    rule.help = (String) yamlRule.get("help");
                }
                if (yamlRule.containsKey("labels")) {
                    TreeMap<String, Object> labels =
                            new TreeMap<>((Map<String, Object>) yamlRule.get("labels"));
                    rule.labelNames = new ArrayList<>();
                    rule.labelValues = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : labels.entrySet()) {
                        rule.labelNames.add(entry.getKey());
                        rule.labelValues.add((String) entry.getValue());
                    }
                }

                // Validation.
                if ((rule.labelNames != null || rule.help != null) && rule.name == null) {
                    throw new IllegalArgumentException(
                            "Must provide name, if help or labels are given: " + yamlRule);
                }
                if (rule.name != null && rule.pattern == null) {
                    throw new IllegalArgumentException(
                            "Must provide pattern, if name is given: " + yamlRule);
                }
            }
        } else {
            // Default to a single default rule.
            cfg.rules.add(new Rule());
        }

        boolean hasCachedRules = false;
        for (Rule rule : cfg.rules) {
            hasCachedRules |= rule.cache;
        }

        // Avoid all costs related to maintaining the cache if there are no cached rules
        if (hasCachedRules) {
            cfg.rulesCache = new MatchedRulesCache();
        }
        cfg.objectNameAttributeFilter = ObjectNameAttributeFilter.create(yamlConfig);

        return cfg;
    }

    /**
     * Convert name to snake case and lower case.
     *
     * @param name the name
     * @return the converted name
     */
    static String toSnakeAndLowerCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        char firstChar = name.charAt(0);

        boolean prevCharIsUpperCaseOrUnderscore =
                Character.isUpperCase(firstChar) || firstChar == '_';

        StringBuilder stringBuilder =
                new StringBuilder(name.length()).append(Character.toLowerCase(firstChar));

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean charIsUpperCase = Character.isUpperCase(c);

            if (!prevCharIsUpperCaseOrUnderscore && charIsUpperCase) {
                stringBuilder.append("_");
            }

            stringBuilder.append(Character.toLowerCase(c));
            prevCharIsUpperCaseOrUnderscore = charIsUpperCase || c == '_';
        }

        return stringBuilder.toString();
    }

    /**
     * Convert the name to a "safe" name by changing invalid chars to underscore, and merging
     * consecutive underscores.
     *
     * @param name the name
     * @return the safe name
     */
    static String toSafeName(String name) {
        if (name == null) {
            return null;
        }

        boolean prevCharIsUnderscore = false;
        StringBuilder stringBuilder = new StringBuilder(name.length());

        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            // prevent a numeric prefix.
            stringBuilder.append("_");
        }

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean isUnsafeChar = !JmxCollector.isLegalCharacter(c);
            if ((isUnsafeChar || c == '_')) {
                if (!prevCharIsUnderscore) {
                    stringBuilder.append("_");
                    prevCharIsUnderscore = true;
                }
            } else {
                stringBuilder.append(c);
                prevCharIsUnderscore = false;
            }
        }

        return stringBuilder.toString();
    }

    private static boolean isLegalCharacter(char input) {
        return ((input == ':')
                || (input == '_')
                || (input >= 'a' && input <= 'z')
                || (input >= 'A' && input <= 'Z')
                || (input >= '0' && input <= '9'));
    }

    static class Receiver implements JmxScraper.MBeanReceiver {

        final List<MatchedRule> matchedRules = new ArrayList<>();

        final Config config;
        final MatchedRulesCache.StalenessTracker stalenessTracker;

        private static final char SEP = '_';

        Receiver(Config config, MatchedRulesCache.StalenessTracker stalenessTracker) {
            this.config = config;
            this.stalenessTracker = stalenessTracker;
        }

        // [] and () are special in regexes, so switch to <>.
        private String angleBrackets(String s) {
            return "<" + s.substring(1, s.length() - 1) + ">";
        }

        // Add the matched rule to the cached rules and tag it as not stale
        private void addToCache(final CacheKey cacheKey, final MatchedRule matchedRule) {
            if (config.rulesCache != null && cacheKey != null) {
                config.rulesCache.put(cacheKey, matchedRule);
                stalenessTracker.markAsFresh(cacheKey);
            }
        }

        private MatchedRule defaultExport(
                String matchName,
                String domain,
                LinkedHashMap<String, String> beanProperties,
                LinkedList<String> attrKeys,
                String attrName,
                String help,
                Double value,
                double valueFactor,
                String type,
                Map<String, String> attributesAsLabelsWithValues) {
            StringBuilder name = new StringBuilder();
            name.append(domain);
            if (!beanProperties.isEmpty()) {
                name.append(SEP);
                name.append(beanProperties.values().iterator().next());
            }
            for (String k : attrKeys) {
                name.append(SEP);
                name.append(k);
            }
            name.append(SEP);
            name.append(attrName);
            String fullname = toSafeName(name.toString());

            if (config.lowercaseOutputName) {
                fullname = fullname.toLowerCase();
            }

            if (config.inferCounterTypeFromName && fullname.endsWith("_total")) {
                type = "COUNTER";
            }

            List<String> labelNames = new ArrayList<>();
            List<String> labelValues = new ArrayList<>();
            if (beanProperties.size() > 1) {
                Iterator<Map.Entry<String, String>> iter = beanProperties.entrySet().iterator();
                // Skip the first one, it's been used in the name.
                iter.next();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String labelName = toSafeName(entry.getKey());
                    if (config.lowercaseOutputLabelNames) {
                        labelName = labelName.toLowerCase();
                    }
                    labelNames.add(labelName);
                    labelValues.add(entry.getValue());
                }
            }
            addAttributesAsLabelsWithValuesToLabels(
                    config, attributesAsLabelsWithValues, labelNames, labelValues);

            return new MatchedRule(
                    fullname, matchName, type, help, labelNames, labelValues, value, valueFactor);
        }

        public void recordBean(
                String domain,
                LinkedHashMap<String, String> beanProperties,
                Map<String, String> attributesAsLabelsWithValues,
                LinkedList<String> attrKeys,
                String attrName,
                String attrType,
                String attrDescription,
                Object beanValue) {

            MatchedRule matchedRule = MatchedRule.unmatched();

            CacheKey cacheKey = null;
            MatchedRule cachedRule = null;

            if (config.rulesCache != null) {
                cacheKey = new CacheKey(domain, beanProperties, attrKeys, attrName);
                cachedRule = config.rulesCache.get(cacheKey);
                if (cachedRule != null) {
                    stalenessTracker.markAsFresh(cacheKey);
                    matchedRule = cachedRule;
                }
            }

            if (matchedRule.isUnmatched()) {
                String beanName =
                        domain
                                + angleBrackets(beanProperties.toString())
                                + angleBrackets(attrKeys.toString());

                // Build the HELP string from the bean metadata.
                String help =
                        domain
                                + ":name="
                                + beanProperties.get("name")
                                + ",type="
                                + beanProperties.get("type")
                                + ",attribute="
                                + attrName;
                // Add the attrDescription to the HELP if it exists and is useful.
                if (attrDescription != null && !attrDescription.equals(attrName)) {
                    help = attrDescription + " " + help;
                }

                for (Rule rule : config.rules) {
                    // If we cache that rule, and we found a cache entry for this bean/attribute,
                    // then what's left to do is to check all uncached rules
                    if (rule.cache && cachedRule != null) {
                        continue;
                    }

                    // Rules with bean values cannot be properly cached (only the value from the
                    // first
                    // scrape will be cached).
                    // If caching for the rule is enabled, replace the value with a dummy <cache> to
                    // avoid caching different values at different times.
                    Object matchBeanValue = rule.cache ? "<cache>" : beanValue;

                    String attributeName;
                    if (rule.attrNameSnakeCase) {
                        attributeName = toSnakeAndLowerCase(attrName);
                    } else {
                        attributeName = attrName;
                    }

                    String matchName = beanName + attributeName + ": " + matchBeanValue;

                    Matcher matcher = null;
                    if (rule.pattern != null) {
                        matcher = rule.pattern.matcher(matchName);
                        if (!matcher.matches()) {
                            continue;
                        }
                    }

                    Double value = null;
                    if (rule.value != null && !rule.value.isEmpty()) {
                        String val = matcher.replaceAll(rule.value);
                        try {
                            value = Double.valueOf(val);
                        } catch (NumberFormatException e) {
                            LOGGER.trace(
                                    "Unable to parse configured value '%s' to number for bean:"
                                            + " %s%s: %s",
                                    val, beanName, attrName, beanValue);
                            return;
                        }
                    }

                    // If there's no name provided, use default export format.
                    if (rule.name == null) {
                        matchedRule =
                                defaultExport(
                                        matchName,
                                        domain,
                                        beanProperties,
                                        attrKeys,
                                        attributeName,
                                        help,
                                        value,
                                        rule.valueFactor,
                                        rule.type,
                                        attributesAsLabelsWithValues);
                        if (rule.cache) {
                            addToCache(cacheKey, matchedRule);
                        }
                        break;
                    }

                    // Matcher is set below here due to validation in the constructor.
                    String name = toSafeName(matcher.replaceAll(rule.name));
                    if (name.isEmpty()) {
                        return;
                    }
                    if (config.lowercaseOutputName) {
                        name = name.toLowerCase();
                    }

                    String type = rule.type;
                    if (config.inferCounterTypeFromName && name.endsWith("_total")) {
                        type = "COUNTER";
                    }

                    // Set the help.
                    if (rule.help != null) {
                        help = matcher.replaceAll(rule.help);
                    }

                    // Set the labels.
                    ArrayList<String> labelNames = new ArrayList<>();
                    ArrayList<String> labelValues = new ArrayList<>();
                    addAttributesAsLabelsWithValuesToLabels(
                            config, attributesAsLabelsWithValues, labelNames, labelValues);
                    if (rule.labelNames != null) {
                        for (int i = 0; i < rule.labelNames.size(); i++) {
                            final String unsafeLabelName = rule.labelNames.get(i);
                            final String labelValReplacement = rule.labelValues.get(i);
                            try {
                                String labelName = toSafeName(matcher.replaceAll(unsafeLabelName));
                                String labelValue = matcher.replaceAll(labelValReplacement);
                                if (config.lowercaseOutputLabelNames) {
                                    labelName = labelName.toLowerCase();
                                }
                                if (!labelName.isEmpty() && !labelValue.isEmpty()) {
                                    labelNames.add(labelName);
                                    labelValues.add(labelValue);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        format(
                                                "Matcher '%s' unable to use: '%s' value: '%s'",
                                                matcher, unsafeLabelName, labelValReplacement),
                                        e);
                            }
                        }
                    }

                    matchedRule =
                            new MatchedRule(
                                    name,
                                    matchName,
                                    type,
                                    help,
                                    labelNames,
                                    labelValues,
                                    value,
                                    rule.valueFactor);
                    if (rule.cache) {
                        addToCache(cacheKey, matchedRule);
                    }
                    break;
                }
            }

            if (matchedRule.isUnmatched()) {
                addToCache(cacheKey, matchedRule);
                return;
            }

            Number value;
            if (matchedRule.value != null) {
                beanValue = matchedRule.value;
            }

            if (beanValue instanceof Number) {
                value = ((Number) beanValue).doubleValue() * matchedRule.valueFactor;
            } else if (beanValue instanceof Boolean) {
                value = (Boolean) beanValue ? 1 : 0;
            } else {
                LOGGER.trace(
                        "Ignoring unsupported bean: %s%s%s%s: %s ",
                        domain,
                        angleBrackets(beanProperties.toString()),
                        angleBrackets(attrKeys.toString()),
                        attrName,
                        beanValue);
                return;
            }

            // Add to samples.
            LOGGER.trace(
                    "add metric sample: %s %s %s",
                    matchedRule.name, matchedRule.labels, value.doubleValue());

            matchedRules.add(matchedRule.withValue(value.doubleValue()));
        }
    }

    private static void addAttributesAsLabelsWithValuesToLabels(
            Config config,
            Map<String, String> attributesAsLabelsWithValues,
            List<String> labelNames,
            List<String> labelValues) {
        attributesAsLabelsWithValues.forEach(
                (attributeAsLabelName, attributeValue) -> {
                    String labelName = toSafeName(attributeAsLabelName);
                    if (config.lowercaseOutputLabelNames) {
                        labelName = labelName.toLowerCase();
                    }
                    labelNames.add(labelName);
                    labelValues.add(attributeValue);
                });
    }

    @Override
    public MetricSnapshots collect() {
        // Take a reference to the current config and collect with this one
        // (to avoid race conditions in case another thread reloads the config in the meantime)
        Config config = getLatestConfig();

        MatchedRulesCache.StalenessTracker stalenessTracker =
                new MatchedRulesCache.StalenessTracker();

        Receiver receiver = new Receiver(config, stalenessTracker);

        JmxScraper scraper =
                new JmxScraper(
                        config.jmxUrl,
                        config.username,
                        config.password,
                        config.ssl,
                        config.includeObjectNames,
                        config.excludeObjectNames,
                        config.objectNameAttributeFilter,
                        config.metricCustomizers,
                        receiver,
                        jmxMBeanPropertyCache);

        long start = System.nanoTime();
        double error = 0;

        if ((config.startDelaySeconds > 0)
                && ((start - createTimeNanoSecs) / 1000000000L < config.startDelaySeconds)) {
            throw new IllegalStateException("JMXCollector waiting for startDelaySeconds");
        }
        try {
            scraper.doScrape();
        } catch (Exception e) {
            error = 1;
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            LOGGER.error("JMX scrape failed: %s", sw);
        }

        if (config.rulesCache != null) {
            config.rulesCache.evictStaleEntries(stalenessTracker);
        }

        jmxScrapeDurationSeconds.set((System.nanoTime() - start) / 1.0E9);
        jmxScrapeError.set(error);
        jmxScrapeCachedBeans.set(stalenessTracker.freshCount());

        return MatchedRuleToMetricSnapshotsConverter.convert(receiver.matchedRules);
    }
}
