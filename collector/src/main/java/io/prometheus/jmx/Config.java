package io.prometheus.jmx;

import io.prometheus.client.Collector;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Config {

    private final int startDelaySeconds;
    private final CollectorConfig collectorConfig;
    private final HttpServerConfig httpServerConfig;
    private final SslConfig sslConfig;
    private final JmxBeanFilterConfig jmxBeanFilterConfig;
    private final MetricFilterConfig metricFilterConfig;
    private final boolean lowercaseOutputName;
    private final boolean lowercaseOutputLabelNames;
    private final List<RuleConfig> rules;

    private Config(int startDelaySeconds, CollectorConfig collectorConfig, HttpServerConfig httpServerConfig,
                   SslConfig sslConfig, JmxBeanFilterConfig jmxBeanFilterConfig, MetricFilterConfig metricFilterConfig,
                   List<RuleConfig> rules, Boolean lowercaseOutputName, Boolean lowercaseOutputLabelNames) {
        this.startDelaySeconds = startDelaySeconds;
        this.collectorConfig = collectorConfig;
        this.httpServerConfig = httpServerConfig;
        this.sslConfig = sslConfig;
        this.jmxBeanFilterConfig = jmxBeanFilterConfig;
        this.metricFilterConfig = metricFilterConfig;
        this.rules = rules;
        this.lowercaseOutputName = lowercaseOutputName;
        this.lowercaseOutputLabelNames = lowercaseOutputLabelNames;
    }

    int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    CollectorConfig getCollectorConfig() {
        return collectorConfig;
    }

    HttpServerConfig getHttpServerConfig() {
        return httpServerConfig;
    }

    SslConfig getSslConfig() {
        return sslConfig;
    }

    JmxBeanFilterConfig getJmxBeanFilterConfig() {
        return jmxBeanFilterConfig;
    }

    MetricFilterConfig getMetricFilterConfig() {
        return metricFilterConfig;
    }

    boolean isLowercaseOutputName() {
        return lowercaseOutputName;
    }

    boolean isLowercaseOutputLabelNames() {
        return lowercaseOutputLabelNames;
    }

    List<RuleConfig> getRules() {
        return rules;
    }

    static class CollectorConfig {

        private final String jmxUrl;
        private final String username;
        private final String password;
        private final boolean sslEnabled;
        private final boolean sslClientAuth;
        private final String sslKeyAlias;

        private CollectorConfig(String jmxUrl, String username, String password,
                                boolean sslEnabled, boolean sslClientAuth, String sslKeyAlias) {
            this.jmxUrl = jmxUrl;
            this.username = username;
            this.password = password;
            this.sslEnabled = sslEnabled;
            this.sslClientAuth = sslClientAuth;
            this.sslKeyAlias = sslKeyAlias;
        }

        String getJmxUrl() {
            return jmxUrl;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        boolean isSslEnabled() {
            return sslEnabled;
        }

        boolean isSslClientAuth() {
            return sslClientAuth;
        }

        String getSslKeyAlias() {
            return sslKeyAlias;
        }
    }

    static class HttpServerConfig {

        private final String address;
        private final Integer port;
        private final String username;
        private final String password;
        private final boolean sslEnabled;
        private final boolean sslClientAuth;
        private final String sslKeyAlias;

        HttpServerConfig(String address, Integer port, String username, String password,
                         boolean sslEnabled, boolean sslClientAuth, String sslKeyAlias) {
            this.address = address;
            this.port = port;
            this.username = username;
            this.password = password;
            this.sslEnabled = sslEnabled;
            this.sslClientAuth = sslClientAuth;
            this.sslKeyAlias = sslKeyAlias;
        }

        String getAddress() {
            return address;
        }

        Integer getPort() {
            return port;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        boolean isSslEnabled() {
            return sslEnabled;
        }

        boolean isSslClientAuth() {
            return sslClientAuth;
        }

        String getSslKeyAlias() {
            return sslKeyAlias;
        }
    }

    static class SslConfig {

        private final String sslKeyStore;
        private final String sslKeyStorePassword;
        private final String sslTrustStore;
        private final String sslTrustStorePassword;

        SslConfig(String sslKeyStore, String sslKeyStorePassword, String sslTrustStore, String sslTrustStorePassword) {
            this.sslKeyStore = sslKeyStore;
            this.sslKeyStorePassword = sslKeyStorePassword;
            this.sslTrustStore = sslTrustStore;
            this.sslTrustStorePassword = sslTrustStorePassword;
        }

        String getSslKeyStore() {
            return sslKeyStore;
        }

        String getSslKeyStorePassword() {
            return sslKeyStorePassword;
        }

        String getSslTrustStore() {
            return sslTrustStore;
        }

        String getSslTrustStorePassword() {
            return sslTrustStorePassword;
        }
    }

    static class JmxBeanFilterConfig {

        private final List<ObjectName> includedObjectNames;
        private final List<ObjectName> excludedObjectNames;

        JmxBeanFilterConfig(List<ObjectName> includedObjectNames, List<ObjectName> excludedObjectNames) {
            this.includedObjectNames = includedObjectNames;
            this.excludedObjectNames = excludedObjectNames;
        }

        List<ObjectName> getIncludedObjectNames() {
            return includedObjectNames;
        }

        List<ObjectName> getExcludedObjectNames() {
            return excludedObjectNames;
        }
    }

    static class MetricFilterConfig {

        final List<String> nameMustStartWith;
        final List<String> nameMustNotStartWith;
        final List<String> nameMustBeEqualTo;
        final List<String> nameMustNotBeEqualTo;

        MetricFilterConfig(List<String> nameMustStartWith, List<String> nameMustNotStartWith,
                           List<String> nameMustBeEqualTo, List<String> nameMustNotBeEqualTo) {
            this.nameMustStartWith = nameMustStartWith;
            this.nameMustNotStartWith = nameMustNotStartWith;
            this.nameMustBeEqualTo = nameMustBeEqualTo;
            this.nameMustNotBeEqualTo = nameMustNotBeEqualTo;
        }

        List<String> getNameMustStartWith() {
            return nameMustStartWith;
        }

        List<String> getNameMustNotStartWith() {
            return nameMustNotStartWith;
        }

        List<String> getNameMustBeEqualTo() {
            return nameMustBeEqualTo;
        }

        List<String> getNameMustNotBeEqualTo() {
            return nameMustNotBeEqualTo;
        }
    }

    static class RuleConfig {

        private final Pattern pattern;
        private final String name;
        private final String value;
        private final String help;
        private final double valueFactor;
        private final boolean attrNameSnakeCase;
        private final boolean cache;
        private final Collector.Type type;
        private final List<String> labelNames;
        private final List<String> labelValues;

        RuleConfig(Pattern pattern, String name, String value, String help, double valueFactor,
                   boolean attrNameSnakeCase, boolean cache, Collector.Type type,
                   List<String> labelNames, List<String> labelValues) {
            this.pattern = pattern;
            this.name = name;
            this.value = value;
            this.help = help;
            this.valueFactor = valueFactor;
            this.attrNameSnakeCase = attrNameSnakeCase;
            this.cache = cache;
            this.type = type;
            this.labelNames = labelNames;
            this.labelValues = labelValues;
        }

        Pattern getPattern() {
            return pattern;
        }

        String getName() {
            return name;
        }

        String getValue() {
            return value;
        }

        String getHelp() {
            return help;
        }

        double getValueFactor() {
            return valueFactor;
        }

        boolean isAttrNameSnakeCase() {
            return attrNameSnakeCase;
        }

        public boolean isCache() {
            return cache;
        }

        public Collector.Type getType() {
            return type;
        }

        List<String> getLabelNames() {
            return labelNames;
        }

        List<String> getLabelValues() {
            return labelValues;
        }
    }

    static Config load(Map<?, ?> yaml) throws ConfigException {
        if (yaml == null) {
            return null;
        } else {
            Yaml rootConfig = new Yaml(yaml);
            return new Config(
                    rootConfig.getInteger("startDelaySeconds", 0),
                    loadCollectorConfig(rootConfig),
                    loadHttpServerConfig(rootConfig),
                    loadSslConfig(rootConfig),
                    loadJmxBeanFilterConfig(rootConfig),
                    loadMetricFilterConfig(rootConfig),
                    loadRules(rootConfig),
                    rootConfig.getBoolean("lowercaseOutputName", false),
                    rootConfig.getBoolean("lowercaseOutputLabelNames", false)
            );
        }
    }


    private static CollectorConfig loadCollectorConfig(Yaml rootConfig) throws ConfigException {
        Yaml collectorConfig = rootConfig.getYaml("collector");
        if (collectorConfig.isEmpty()) {
            // The legacy config format has the collector config on the root level. Try loading it from there.
            collectorConfig = rootConfig;
        }
        CollectorConfig result = new CollectorConfig(
                loadJmxUrl(collectorConfig),
                collectorConfig.getString("username"),
                collectorConfig.getString("password"),
                collectorConfig.getBoolean("sslEnabled", loadLegacySslFlagOrFalse(collectorConfig)),
                collectorConfig.getBoolean("sslClientAuth", false),
                collectorConfig.getString("sslKeyAlias"));
        if (result.jmxUrl == null) {
            if (result.username != null || result.password != null) {
                throw new ConfigException("collector", "Cannot set username or password without specifying hostPort or jmxUrl");
            }
            if (collectorConfig.getBoolean("ssl") != null) {
                throw new ConfigException("collector", "Cannot set ssl=" + result.sslEnabled + " without specifying hostPort or jmxUrl");
            }
            if (collectorConfig.getBoolean("sslEnabled") != null) {
                throw new ConfigException("collector", "Cannot set sslEnabled=" + result.sslEnabled + " without specifying hostPort or jmxUrl");
            }
            if (result.sslKeyAlias != null) {
                throw new ConfigException("collector", "Cannot configure keyAlias without specifying hostPort or jmxUrl");
            }
        }
        if (result.username != null && result.password == null) {
            throw new ConfigException("collector", "Cannot set username without password");
        }
        if (result.username == null && result.password != null) {
            throw new ConfigException("collector", "Cannot set password without username");
        }
        return result;
    }

    private static String loadJmxUrl(Yaml collectorConfig) throws ConfigException {
        String hostPort = collectorConfig.getString("hostPort");
        String jmxUrl = collectorConfig.getString("jmxUrl");
        if (hostPort != null) {
            if (jmxUrl != null) {
                throw new ConfigException("collector", "At most one of hostPort and jmxUrl must be provided");
            } else {
                jmxUrl = "service:jmx:rmi:///jndi/rmi://" + hostPort + "/jmxrmi";
            }
        }
        return jmxUrl;
    }

    private static boolean loadLegacySslFlagOrFalse(Yaml rootConfig) throws ConfigException {
        // In the legacy format, ssl was a Boolean.
        // In the current format, ssl is a Map.
        // Return true if the old boolean is found and true, false otherwise.
        return Boolean.TRUE.equals(rootConfig.get("ssl", Object.class));
    }

    private static HttpServerConfig loadHttpServerConfig(Yaml rootConfig) throws ConfigException {
        Yaml httpServerConfig = rootConfig.getYaml("httpServer");
        HttpServerConfig result = new HttpServerConfig(
                httpServerConfig.getString("address", "0.0.0.0"),
                httpServerConfig.getInteger("port"),
                httpServerConfig.getString("username"),
                httpServerConfig.getString("password"),
                httpServerConfig.getBoolean("sslEnabled", false),
                httpServerConfig.getBoolean("sslClientAuth", false),
                httpServerConfig.getString("sslKeyAlias"));
        if (result.username != null && result.password == null) {
            throw new ConfigException("httpServer", "Cannot set username without password");
        }
        if (result.username == null && result.password != null) {
            throw new ConfigException("httpServer", "Cannot set password without username");
        }
        return result;
    }

    private static SslConfig loadSslConfig(Yaml rootConfig) throws ConfigException {
        Object sslEntry = rootConfig.get("ssl", Object.class);
        if (sslEntry instanceof Boolean || sslEntry == null) {
            // legacy format or empty
            return new SslConfig(null, null, null, null);
        } else if (sslEntry instanceof Map) {
            // new format
            Yaml sslConfig = rootConfig.getYaml("ssl");
            return new SslConfig(
                    sslConfig.getString("sslKeyStore"),
                    sslConfig.getString("sslKeyStorePassword"),
                    sslConfig.getString("sslTrustStore"),
                    sslConfig.getString("sslTrustStorePassword"));
        } else {
            throw new ConfigException("Invalid type of ssl: " + sslEntry.getClass().getSimpleName());
        }
    }

    private static JmxBeanFilterConfig loadJmxBeanFilterConfig(Yaml rootConfig) throws ConfigException {
        Yaml jmxBeanFilterConfig = rootConfig.getYaml("jmxBeanFilter");
        List<ObjectName> include, exclude;
        if (jmxBeanFilterConfig.isEmpty()) {
            // try loading the legacy config file format
            include = readJmxObjectNames("whitelistObjectNames", rootConfig);
            exclude = readJmxObjectNames("blacklistObjectNames", rootConfig);
        } else {
            include = readJmxObjectNames("include", jmxBeanFilterConfig);
            exclude = readJmxObjectNames("exclude", jmxBeanFilterConfig);
        }
        return new JmxBeanFilterConfig(include, exclude);
    }

    private static List<ObjectName> readJmxObjectNames(String key, Yaml jmxBeanFilterConfig) throws ConfigException {
        List<String> entries = jmxBeanFilterConfig.getStringList(key);
        List<ObjectName> result = new ArrayList<ObjectName>();
        for (String entry : entries) {
            try {
                result.add(new ObjectName(entry));
            } catch (MalformedObjectNameException e) {
                throw new ConfigException("jmxBeanFilter", "Illegal JMX object name: " + entry, e);
            }
        }
        return result;
    }

    private static MetricFilterConfig loadMetricFilterConfig(Yaml rootConfig) throws ConfigException {
        Yaml metricFilterConfig = rootConfig.getYaml("metricFilter");
        return new MetricFilterConfig(
                metricFilterConfig.getStringList("nameMustStartWith"),
                metricFilterConfig.getStringList("nameMustNotStartWith"),
                metricFilterConfig.getStringList("nameMustBeEqualTo"),
                metricFilterConfig.getStringList("nameMustNotBeEqualTo")
        );
    }

    private static List<RuleConfig> loadRules(Yaml rootConfig) throws ConfigException {
        List<Map<?, ?>> yamlRules = rootConfig.getMapList("rules");
        List<RuleConfig> result = new ArrayList<RuleConfig>(yamlRules.size());
        for (Map<?, ?> yamlRule : yamlRules) {
            result.add(loadRuleConfig(new Yaml(yamlRule)));
        }
        return result;
    }

    private static RuleConfig loadRuleConfig(Yaml ruleConfig) throws ConfigException {
        List<String> labelNames = null;
        List<String> labelValues = null;
        Map<?, ?> labels = ruleConfig.getMap("labels");
        if (labels != null) {
            TreeMap<String, String> sortedLabels = new TreeMap<String, String>();
            for (Map.Entry<?, ?> entry : labels.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new ConfigException("rules", entry.getKey() + ": invalid type for label name. Expected String, but got " + entry.getKey().getClass().getSimpleName());
                }
                if (!(entry.getValue() instanceof String)) {
                    throw new ConfigException("rules", entry.getValue() + ": invalid type for label value. Expected String, but got " + entry.getValue().getClass().getSimpleName());
                }
                sortedLabels.put((String) entry.getKey(), (String) entry.getValue());
            }
            labelNames = new ArrayList<String>(sortedLabels.size());
            labelValues = new ArrayList<String>(sortedLabels.size());
            for (Map.Entry<String, String> entry : sortedLabels.entrySet()) {
                labelNames.add(entry.getKey());
                labelValues.add(entry.getValue());
            }
        }
        RuleConfig result = new RuleConfig(
                loadPattern(ruleConfig),
                ruleConfig.getString("name"),
                ruleConfig.getString("value"),
                ruleConfig.getString("help"),
                ruleConfig.getDouble("valueFactor", 1.0),
                ruleConfig.getBoolean("attrNameSnakeCase", false),
                ruleConfig.getBoolean("cache", false),
                loadType(ruleConfig),
                labelNames,
                labelValues
        );
        if ((!result.labelNames.isEmpty() || result.help != null) && result.name == null) {
            throw new ConfigException("rules", "Must provide name if help or labels are given.");
        }
        if (result.name != null && result.pattern == null) {
            throw new ConfigException("rules", "Must provide pattern if name is given.");
        }
        return result;
    }

    private static Pattern loadPattern(Yaml ruleConfig) throws ConfigException {
        String patternString = ruleConfig.getString("pattern");
        try {
            return Pattern.compile("^.*(?:" + patternString + ").*$");
        } catch (PatternSyntaxException e) {
            throw new ConfigException("rules", "invalid regular expression: " + patternString, e);
        }
    }

    private static Collector.Type loadType(Yaml ruleConfig) throws ConfigException {
        String typeString = ruleConfig.getString("type", "UNKNOWN");
        // Gracefully handle switch to OM data model.
        if ("UNTYPED".equals(typeString)) {
            typeString = "UNKNOWN";
        }
        try {
            return Collector.Type.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            throw new ConfigException("rules", typeString + ": illegal value for type", e);
        }
    }

    static class ConfigException extends Exception {

        private ConfigException(String msg) {
            super(msg);
        }

        private ConfigException(String section, String msg) {
            super("Configuration error in " + section + ": " + msg);
        }

        private ConfigException(String section, String msg, Throwable cause) {
            super("Configuration error in " + section + ": " + msg, cause);
        }
    }


    /**
     * Helper for accessing the YAML data.
     */
    private static class Yaml {

        private final Map<?, ?> map;

        private Yaml(Map<?, ?> map) {
            this.map = map;
        }

        private static Yaml empty() {
            return new Yaml(new HashMap<Object,Object>());
        }

        private String getString(String key) throws ConfigException {
            return get(key, String.class);
        }

        private String getString(String key, String defaultValue) throws ConfigException {
            String result = getString(key);
            return result == null ? defaultValue : result;
        }

        private Integer getInteger(String key) throws ConfigException {
            return get(key, Integer.class);
        }

        private int getInteger(String key, int defaultValue) throws ConfigException {
            Integer result = getInteger(key);
            return result != null ? result : defaultValue;
        }

        private Double getDouble(String key) throws ConfigException {
            return get(key, Double.class);
        }

        private double getDouble(String key, double defaultValue) throws ConfigException {
            Double result = getDouble(key);
            return result != null ? result : defaultValue;
        }

        private Boolean getBoolean(String key) throws ConfigException {
            return get(key, Boolean.class);
        }

        private boolean getBoolean(String key, boolean defaultValue) throws ConfigException {
            Boolean result = getBoolean(key);
            return result != null ? result : defaultValue;
        }

        private Map<?, ?> getMap(String key) throws ConfigException {
            return get(key, Map.class);
        }

        private List<String> getStringList(String key) throws ConfigException {
            return list(key, String.class);
        }

        private List<Map<?, ?>> getMapList(String key) throws ConfigException {
            List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();
            for (Map<?, ?> map : list(key, Map.class)) {
                result.add(map);
            }
            return result;
        }

        private Yaml getYaml(String key) throws ConfigException {
            Map<?, ?> map = getMap(key);
            if (map == null) {
                return Yaml.empty();
            } else {
                return new Yaml(map);
            }
        }

        private boolean isEmpty() {
            return map.isEmpty();
        }

        private <T> T get(String key, Class<T> c) throws ConfigException {
            Object value = map.get(key);
            if (value == null) {
                return null;
            } else if (!c.isAssignableFrom(value.getClass())) {
                throw new ConfigException("Illegal type for '" + key + "': Expected " + c.getSimpleName() + ", got " + value.getClass().getSimpleName());
            } else {
                return (T) value;
            }
        }

        private <T> List<T> list(String key, Class<T> c) throws ConfigException {
            List<?> entries = get(key, List.class);
            if (entries == null) {
                return Collections.emptyList();
            }
            // convert List<?> to List<T> for convenience
            List<T> result = new ArrayList<T>(entries.size());
            for (Object entry : entries) {
                if (!c.isAssignableFrom(entry.getClass())) {
                    throw new ConfigException("Illegal type for list item '" + key + "': Expected " + c.getSimpleName() + ", got " + entry.getClass().getSimpleName());
                }
                result.add((T) entry);
            }
            return result;
        }
    }
}