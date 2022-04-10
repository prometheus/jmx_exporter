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

public class Config {

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

    public int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public CollectorConfig getCollectorConfig() {
        return collectorConfig;
    }

    public HttpServerConfig getHttpServerConfig() {
        return httpServerConfig;
    }

    public SslConfig getSslConfig() {
        return sslConfig;
    }

    public JmxBeanFilterConfig getJmxBeanFilterConfig() {
        return jmxBeanFilterConfig;
    }

    public MetricFilterConfig getMetricFilterConfig() {
        return metricFilterConfig;
    }

    public boolean isLowercaseOutputName() {
        return lowercaseOutputName;
    }

    public boolean isLowercaseOutputLabelNames() {
        return lowercaseOutputLabelNames;
    }

    public List<RuleConfig> getRules() {
        return rules;
    }

    public List<RuleConfig> getRulesOrDefault() {
        if (!rules.isEmpty()) {
            return rules;
        } else {
            return RuleConfig.DEFAULT;
        }
    }

    public static class CollectorConfig {

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

        public String getJmxUrl() {
            return jmxUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean isSslEnabled() {
            return sslEnabled;
        }

        public boolean isSslClientAuth() {
            return sslClientAuth;
        }

        public String getSslKeyAlias() {
            return sslKeyAlias;
        }
    }

    public static class HttpServerConfig {

        private final String address;
        private final Integer port;
        private final String username;
        private final String password;
        private final boolean sslEnabled;
        private final boolean sslClientAuth;
        private final String sslKeyAlias;

        private HttpServerConfig(String address, Integer port, String username, String password,
                         boolean sslEnabled, boolean sslClientAuth, String sslKeyAlias) {
            this.address = address;
            this.port = port;
            this.username = username;
            this.password = password;
            this.sslEnabled = sslEnabled;
            this.sslClientAuth = sslClientAuth;
            this.sslKeyAlias = sslKeyAlias;
        }

        public String getAddress() {
            return address;
        }

        public Integer getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public boolean isSslEnabled() {
            return sslEnabled;
        }

        public boolean isSslClientAuth() {
            return sslClientAuth;
        }

        public String getSslKeyAlias() {
            return sslKeyAlias;
        }
    }

    public static class SslConfig {

        private final String sslKeyStore;
        private final String sslKeyStorePassword;
        private final String sslTrustStore;
        private final String sslTrustStorePassword;

        private SslConfig(String sslKeyStore, String sslKeyStorePassword, String sslTrustStore, String sslTrustStorePassword) {
            this.sslKeyStore = sslKeyStore;
            this.sslKeyStorePassword = sslKeyStorePassword;
            this.sslTrustStore = sslTrustStore;
            this.sslTrustStorePassword = sslTrustStorePassword;
        }

        public String getSslKeyStore() {
            return sslKeyStore;
        }

        public String getSslKeyStorePassword() {
            return sslKeyStorePassword;
        }

        public String getSslTrustStore() {
            return sslTrustStore;
        }

        public String getSslTrustStorePassword() {
            return sslTrustStorePassword;
        }
    }

    public static class JmxBeanFilterConfig {

        private final List<ObjectName> includedObjectNames;
        private final List<ObjectName> excludedObjectNames;

        private JmxBeanFilterConfig(List<ObjectName> includedObjectNames, List<ObjectName> excludedObjectNames) {
            this.includedObjectNames = includedObjectNames;
            this.excludedObjectNames = excludedObjectNames;
        }

        public List<ObjectName> getIncludedObjectNames() {
            return includedObjectNames;
        }

        public List<ObjectName> getExcludedObjectNames() {
            return excludedObjectNames;
        }
    }

    public static class MetricFilterConfig {

        private final List<String> nameMustStartWith;
        private final List<String> nameMustNotStartWith;
        private final List<String> nameMustBeEqualTo;
        private final List<String> nameMustNotBeEqualTo;
        private final List<String> nameMustEndWith;
        private final List<String> nameMustNotEndWith;

        private MetricFilterConfig(List<String> nameMustStartWith, List<String> nameMustNotStartWith,
                           List<String> nameMustBeEqualTo, List<String> nameMustNotBeEqualTo,
                           List<String> nameMustEndWith, List<String> nameMustNotEndWith) {
            this.nameMustStartWith = nameMustStartWith;
            this.nameMustNotStartWith = nameMustNotStartWith;
            this.nameMustBeEqualTo = nameMustBeEqualTo;
            this.nameMustNotBeEqualTo = nameMustNotBeEqualTo;
            this.nameMustEndWith = nameMustEndWith;
            this.nameMustNotEndWith = nameMustNotEndWith;
        }

        public List<String> getNameMustStartWith() {
            return nameMustStartWith;
        }

        public List<String> getNameMustNotStartWith() {
            return nameMustNotStartWith;
        }

        public List<String> getNameMustBeEqualTo() {
            return nameMustBeEqualTo;
        }

        public List<String> getNameMustNotBeEqualTo() {
            return nameMustNotBeEqualTo;
        }

        public List<String> getNameMustEndWith() {
            return nameMustEndWith;
        }

        public List<String> getNameMustNotEndWith() {
            return nameMustNotEndWith;
        }
    }

    public static class RuleConfig {

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

        public static final List<RuleConfig> DEFAULT;
        static {
            DEFAULT = new ArrayList<RuleConfig>();
            DEFAULT.add(new RuleConfig(null, null, null, null, 1.0, false, false, Collector.Type.UNKNOWN, null, null));
        }

        private RuleConfig(Pattern pattern, String name, String value, String help, double valueFactor,
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

        public Pattern getPattern() {
            return pattern;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getHelp() {
            return help;
        }

        public double getValueFactor() {
            return valueFactor;
        }

        public boolean isAttrNameSnakeCase() {
            return attrNameSnakeCase;
        }

        public boolean isCache() {
            return cache;
        }

        public Collector.Type getType() {
            return type;
        }

        public List<String> getLabelNames() {
            return labelNames;
        }

        public List<String> getLabelValues() {
            return labelValues;
        }
    }

    public static Config load(Map<?, ?> yaml) throws ConfigException {
        if (yaml == null) {
            return load(new HashMap<Object, Object>());
        } else {
            Yaml rootConfig = new Yaml(yaml);
            return new Config(
                    rootConfig.getInteger(null, "startDelaySeconds", 0),
                    loadCollectorConfig("collector", rootConfig),
                    loadHttpServerConfig("httpServer", rootConfig),
                    loadSslConfig("ssl", rootConfig),
                    loadJmxBeanFilterConfig("jmxBeanFilter", rootConfig),
                    loadMetricFilterConfig("metricFilter", rootConfig),
                    loadRules("rules", rootConfig),
                    rootConfig.getBoolean(null, "lowercaseOutputName", false),
                    rootConfig.getBoolean(null, "lowercaseOutputLabelNames", false)
            );
        }
    }


    private static CollectorConfig loadCollectorConfig(String prefix, Yaml rootConfig) throws ConfigException {
        Yaml collectorConfig = rootConfig.getYaml(null, prefix);
        if (collectorConfig.isEmpty()) {
            // The legacy config format has the collector config on the root level. Try loading it from there.
            collectorConfig = rootConfig;
        }
        CollectorConfig result = new CollectorConfig(
                loadJmxUrl(prefix, collectorConfig),
                collectorConfig.getString(prefix, "username"),
                collectorConfig.getString(prefix, "password"),
                collectorConfig.getBoolean(prefix, "sslEnabled", loadLegacySslFlagOrFalse(collectorConfig)),
                collectorConfig.getBoolean(prefix, "sslClientAuth", false),
                collectorConfig.getString(prefix, "sslKeyAlias"));
        if (result.jmxUrl == null) {
            if (result.username != null || result.password != null) {
                throw new ConfigException(prefix, "Cannot set username or password without specifying hostPort or jmxUrl");
            }
            if (collectorConfig.getBoolean(prefix, "ssl") != null) {
                throw new ConfigException(prefix, "Cannot set ssl=" + result.sslEnabled + " without specifying hostPort or jmxUrl");
            }
            if (collectorConfig.getBoolean(prefix, "sslEnabled") != null) {
                throw new ConfigException(prefix, "Cannot set sslEnabled=" + result.sslEnabled + " without specifying hostPort or jmxUrl");
            }
            if (result.sslKeyAlias != null) {
                throw new ConfigException(prefix, "Cannot configure keyAlias without specifying hostPort or jmxUrl");
            }
        }
        if (result.username != null && result.password == null) {
            throw new ConfigException(prefix, "Cannot set username without password");
        }
        if (result.username == null && result.password != null) {
            throw new ConfigException(prefix, "Cannot set password without username");
        }
        return result;
    }

    private static String loadJmxUrl(String prefix, Yaml collectorConfig) throws ConfigException {
        String hostPort = collectorConfig.getString(prefix, "hostPort");
        String jmxUrl = collectorConfig.getString(prefix, "jmxUrl");
        if (hostPort != null) {
            if (jmxUrl != null) {
                throw new ConfigException(prefix, "At most one of hostPort and jmxUrl must be provided");
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
        return Boolean.TRUE.equals(rootConfig.get(null, "ssl", Object.class));
    }

    private static HttpServerConfig loadHttpServerConfig(String prefix, Yaml rootConfig) throws ConfigException {
        Yaml httpServerConfig = rootConfig.getYaml(null, prefix);
        HttpServerConfig result = new HttpServerConfig(
                httpServerConfig.getString(prefix, "address", "0.0.0.0"),
                httpServerConfig.getInteger(prefix, "port"),
                httpServerConfig.getString(prefix, "username"),
                httpServerConfig.getString(prefix, "password"),
                httpServerConfig.getBoolean(prefix, "sslEnabled", false),
                httpServerConfig.getBoolean(prefix, "sslClientAuth", false),
                httpServerConfig.getString(prefix, "sslKeyAlias"));
        if (result.username != null && result.password == null) {
            throw new ConfigException(prefix, "Cannot set username without password");
        }
        if (result.username == null && result.password != null) {
            throw new ConfigException(prefix, "Cannot set password without username");
        }
        return result;
    }

    private static SslConfig loadSslConfig(String prefix, Yaml rootConfig) throws ConfigException {
        Object sslEntry = rootConfig.get(null, prefix, Object.class);
        if (sslEntry instanceof Boolean || sslEntry == null) {
            // legacy format or empty
            return new SslConfig(null, null, null, null);
        } else if (sslEntry instanceof Map) {
            // new format
            Yaml sslConfig = rootConfig.getYaml(null, prefix);
            return new SslConfig(
                    sslConfig.getString(prefix, "sslKeyStore"),
                    sslConfig.getString(prefix, "sslKeyStorePassword"),
                    sslConfig.getString(prefix, "sslTrustStore"),
                    sslConfig.getString(prefix, "sslTrustStorePassword"));
        } else {
            throw new ConfigException("Invalid type of ssl: " + sslEntry.getClass().getSimpleName());
        }
    }

    private static JmxBeanFilterConfig loadJmxBeanFilterConfig(String prefix, Yaml rootConfig) throws ConfigException {
        Yaml jmxBeanFilterConfig = rootConfig.getYaml(null, prefix);
        List<ObjectName> include, exclude;
        if (jmxBeanFilterConfig.isEmpty()) {
            // try loading the legacy config file format
            include = readJmxObjectNames(prefix, "whitelistObjectNames", rootConfig);
            exclude = readJmxObjectNames(prefix, "blacklistObjectNames", rootConfig);
        } else {
            include = readJmxObjectNames(prefix, "include", jmxBeanFilterConfig);
            exclude = readJmxObjectNames(prefix, "exclude", jmxBeanFilterConfig);
        }
        return new JmxBeanFilterConfig(include, exclude);
    }

    private static List<ObjectName> readJmxObjectNames(String prefix, String key, Yaml jmxBeanFilterConfig) throws ConfigException {
        List<String> entries = jmxBeanFilterConfig.getStringList(prefix, key);
        List<ObjectName> result = new ArrayList<ObjectName>();
        for (String entry : entries) {
            try {
                result.add(new ObjectName(entry));
            } catch (MalformedObjectNameException e) {
                throw new ConfigException(prefix, "Illegal JMX object name: " + entry, e);
            }
        }
        return result;
    }

    private static MetricFilterConfig loadMetricFilterConfig(String prefix, Yaml rootConfig) throws ConfigException {
        Yaml metricFilterConfig = rootConfig.getYaml(null, prefix);
        return new MetricFilterConfig(
                metricFilterConfig.getStringList(prefix, "nameMustStartWith"),
                metricFilterConfig.getStringList(prefix, "nameMustNotStartWith"),
                metricFilterConfig.getStringList(prefix, "nameMustBeEqualTo"),
                metricFilterConfig.getStringList(prefix, "nameMustNotBeEqualTo"),
                metricFilterConfig.getStringList(prefix, "nameMustEndWith"),
                metricFilterConfig.getStringList(prefix, "nameMustNotEndWith")
            );
    }

    private static List<RuleConfig> loadRules(String prefix, Yaml rootConfig) throws ConfigException {
        List<Map<?, ?>> yamlRules = rootConfig.getMapList(null, prefix);
        List<RuleConfig> result = new ArrayList<RuleConfig>(yamlRules.size());
        int i=0;
        for (Map<?, ?> yamlRule : yamlRules) {
            result.add(loadRuleConfig(prefix + "[" + i + "]", new Yaml(yamlRule)));
            i++;
        }
        return result;
    }

    private static RuleConfig loadRuleConfig(String prefix, Yaml ruleConfig) throws ConfigException {
        List<String> labelNames = null;
        List<String> labelValues = null;
        Map<?, ?> labels = ruleConfig.getMap(prefix, "labels");
        if (labels != null) {
            TreeMap<String, String> sortedLabels = new TreeMap<String, String>();
            for (Map.Entry<?, ?> entry : labels.entrySet()) {
                if (!(entry.getKey() instanceof String)) {
                    throw new ConfigException(prefix, entry.getKey() + ": invalid type for label name. Expected String, but got " + entry.getKey().getClass().getSimpleName());
                }
                if (Yaml.getAsString(entry.getValue()) == null) {
                    throw new ConfigException(prefix, entry.getValue() + ": invalid type for label value. Expected String, but got " + entry.getValue().getClass().getSimpleName());
                }
                sortedLabels.put((String) entry.getKey(), Yaml.getAsString(entry.getValue()));
            }
            labelNames = new ArrayList<String>(sortedLabels.size());
            labelValues = new ArrayList<String>(sortedLabels.size());
            for (Map.Entry<String, String> entry : sortedLabels.entrySet()) {
                labelNames.add(entry.getKey());
                labelValues.add(entry.getValue());
            }
        }
        RuleConfig result = new RuleConfig(
                loadPattern(prefix, ruleConfig),
                ruleConfig.getString(prefix, "name"),
                ruleConfig.getString(prefix, "value"),
                ruleConfig.getString(prefix, "help"),
                ruleConfig.getDouble(prefix, "valueFactor", 1.0),
                ruleConfig.getBoolean(prefix, "attrNameSnakeCase", false),
                ruleConfig.getBoolean(prefix, "cache", false),
                loadType(prefix, ruleConfig),
                labelNames,
                labelValues
        );
        if ((result.labelNames != null || result.help != null) && result.name == null) {
            throw new ConfigException("rules", "Must provide name if help or labels are given.");
        }
        if (result.name != null && result.pattern == null) {
            throw new ConfigException("rules", "Must provide pattern if name is given.");
        }
        return result;
    }

    private static Pattern loadPattern(String prefix, Yaml ruleConfig) throws ConfigException {
        String patternString = ruleConfig.getString(prefix, "pattern");
        if (patternString == null) {
            return null;
        }
        try {
            return Pattern.compile("^.*(?:" + patternString + ").*$");
        } catch (PatternSyntaxException e) {
            throw new ConfigException("rules", "invalid regular expression: " + patternString, e);
        }
    }

    private static Collector.Type loadType(String prefix, Yaml ruleConfig) throws ConfigException {
        String typeString = ruleConfig.getString(prefix, "type", "UNKNOWN");
        // Gracefully handle switch to OM data model.
        if ("UNTYPED".equals(typeString)) {
            typeString = "UNKNOWN";
        }
        try {
            return Collector.Type.valueOf(typeString);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(prefix, typeString + ": illegal value for type", e);
        }
    }

    public static class ConfigException extends Exception {

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

        private String getString(String prefix, String key) throws ConfigException {
            return getString(prefix, key, null);
        }

        private String getString(String prefix, String key, String defaultValue) throws ConfigException {
            Object value = map.get(key);
            if (value == null) {
                return defaultValue;
            }
            String result = getAsString(value);
            if (result != null) {
                return result;
            } else {
                throw new ConfigException("Illegal type for '" + prefix + "." + key + "': Expected String, got " + value.getClass().getSimpleName() + ".");
            }
        }

        private static String getAsString(Object value) {
            if (value != null && (String.class.isAssignableFrom(value.getClass()) || Boolean.class.isAssignableFrom(value.getClass()) || Number.class.isAssignableFrom(value.getClass()))) {
                return value.toString();
            }
            return null;
        }

        private Integer getInteger(String prefix, String key) throws ConfigException {
            return get(prefix, key, Integer.class);
        }

        private int getInteger(String prefix, String key, int defaultValue) throws ConfigException {
            Integer result = getInteger(prefix, key);
            return result != null ? result : defaultValue;
        }

        private double getDouble(String prefix, String key, double defaultValue) throws ConfigException {
            Double result = getDouble(prefix, key);
            if (result != null) {
                return result;
            }
            return defaultValue;
        }

        private Double getDouble(String prefix, String key) throws ConfigException {
            Object value = map.get(key);
            if (value == null) {
                return null;
            } else if (Double.class.isAssignableFrom(value.getClass())) {
                return (Double) value;
            } else if (Integer.class.isAssignableFrom(value.getClass())) {
                return Double.valueOf((Integer) value);
            } else if (Long.class.isAssignableFrom(value.getClass())) {
                return Double.valueOf((Long) value);
            } else {
                throw new ConfigException("Illegal type for '" + prefix + "." + key + "': Expected Double, got " + value.getClass().getSimpleName() + ".");
            }
        }

        private Boolean getBoolean(String prefix, String key) throws ConfigException {
            return get(prefix, key, Boolean.class);
        }

        private boolean getBoolean(String prefix, String key, boolean defaultValue) throws ConfigException {
            Boolean result = getBoolean(prefix, key);
            return result != null ? result : defaultValue;
        }

        private Map<?, ?> getMap(String prefix, String key) throws ConfigException {
            return get(prefix, key, Map.class);
        }

        private List<String> getStringList(String prefix, String key) throws ConfigException {
            return list(prefix, key, String.class);
        }

        private List<Map<?, ?>> getMapList(String prefix, String key) throws ConfigException {
            List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();
            for (Map<?, ?> map : list(prefix, key, Map.class)) {
                result.add(map);
            }
            return result;
        }

        private Yaml getYaml(String prefix, String key) throws ConfigException {
            Map<?, ?> map = getMap(prefix, key);
            if (map == null) {
                return Yaml.empty();
            } else {
                return new Yaml(map);
            }
        }

        private boolean isEmpty() {
            return map.isEmpty();
        }

        private <T> T get(String prefix, String key, Class<T> c) throws ConfigException {
            Object value = map.get(key);
            if (value == null) {
                return null;
            } else if (!c.isAssignableFrom(value.getClass())) {
                throw new ConfigException("Illegal type for '" + prefix + "." + key + "': Expected " + c.getSimpleName() + ", got " + value.getClass().getSimpleName() + ".");
            } else {
                return (T) value;
            }
        }

        private <T> List<T> list(String prefix, String key, Class<T> c) throws ConfigException {
            List<?> entries = get(prefix, key, List.class);
            if (entries == null) {
                return Collections.emptyList();
            }
            // convert List<?> to List<T> for convenience
            List<T> result = new ArrayList<T>(entries.size());
            for (Object entry : entries) {
                if (!c.isAssignableFrom(entry.getClass())) {
                    throw new ConfigException("Illegal type for list item '" + prefix + "." + key + "': Expected " + c.getSimpleName() + ", got " + entry.getClass().getSimpleName() + ".");
                }
                result.add((T) entry);
            }
            return result;
        }
    }
}