package io.prometheus.jmx;

import io.prometheus.client.Collector;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Config extends JmxCollector.JmxCollectorConfig {
  private Config() {
  }

  Integer minThreads;

  Integer maxThreads;

  static Config loadConfig(Map<String, Object> yamlConfig, File file) throws MalformedObjectNameException {
    Config cfg = new Config();

    cfg.lastUpdate = file == null ? 0 : file.lastModified();

    if (yamlConfig == null) {  // Yaml jmxCollectorConfig empty, set jmxCollectorConfig to empty map.
      yamlConfig = new HashMap<String, Object>();
    }

    if (yamlConfig.containsKey("minThreads")) {
      try {
        cfg.minThreads = (Integer) yamlConfig.get("minThreads");
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number provided for minThreads", e);
      }
    }
    if (yamlConfig.containsKey("maxThreads")) {
      try {
        cfg.maxThreads = (Integer) yamlConfig.get("maxThreads");
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number provided for maxThreads", e);
      }
    }


    if (yamlConfig.containsKey("startDelaySeconds")) {
      try {
        cfg.startDelaySeconds = (Integer) yamlConfig.get("startDelaySeconds");
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid number provided for startDelaySeconds", e);
      }
    }
    if (yamlConfig.containsKey("hostPort")) {
      if (yamlConfig.containsKey("jmxUrl")) {
        throw new IllegalArgumentException("At most one of hostPort and jmxUrl must be provided");
      }
      cfg.jmxUrl = "service:jmx:rmi:///jndi/rmi://" + (String) yamlConfig.get("hostPort") + "/jmxrmi";
    } else if (yamlConfig.containsKey("jmxUrl")) {
      cfg.jmxUrl = (String) yamlConfig.get("jmxUrl");
    }

    if (yamlConfig.containsKey("username")) {
      cfg.username = (String) yamlConfig.get("username");
    }

    if (yamlConfig.containsKey("password")) {
      cfg.password = (String) yamlConfig.get("password");
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

    if (yamlConfig.containsKey("whitelistObjectNames")) {
      List<Object> names = (List<Object>) yamlConfig.get("whitelistObjectNames");
      for (Object name : names) {
        cfg.whitelistObjectNames.add(new ObjectName((String) name));
      }
    } else {
      cfg.whitelistObjectNames.add(null);
    }

    if (yamlConfig.containsKey("blacklistObjectNames")) {
      List<Object> names = (List<Object>) yamlConfig.get("blacklistObjectNames");
      for (Object name : names) {
        cfg.blacklistObjectNames.add(new ObjectName((String) name));
      }
    }

    if (yamlConfig.containsKey("rules")) {
      List<Map<String, Object>> configRules = (List<Map<String, Object>>) yamlConfig.get("rules");
      for (Map<String, Object> ruleObject : configRules) {
        Map<String, Object> yamlRule = ruleObject;
        JmxCollector.Rule rule = new JmxCollector.Rule();
        cfg.rules.add(rule);
        if (yamlRule.containsKey("pattern")) {
          rule.pattern = Pattern.compile("^.*(?:" + (String) yamlRule.get("pattern") + ").*$");
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
          rule.type = Collector.Type.valueOf(t);
        }
        if (yamlRule.containsKey("help")) {
          rule.help = (String) yamlRule.get("help");
        }
        if (yamlRule.containsKey("labels")) {
          TreeMap labels = new TreeMap((Map<String, Object>) yamlRule.get("labels"));
          rule.labelNames = new ArrayList<String>();
          rule.labelValues = new ArrayList<String>();
          for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) labels.entrySet()) {
            rule.labelNames.add(entry.getKey());
            rule.labelValues.add((String) entry.getValue());
          }
        }

        // Validation.
        if ((rule.labelNames != null || rule.help != null) && rule.name == null) {
          throw new IllegalArgumentException("Must provide name, if help or labels are given: " + yamlRule);
        }
        if (rule.name != null && rule.pattern == null) {
          throw new IllegalArgumentException("Must provide pattern, if name is given: " + yamlRule);
        }
      }
    } else {
      // Default to a single default rule.
      cfg.rules.add(new JmxCollector.Rule());
    }

    cfg.rulesCache = new MatchedRulesCache(cfg.rules);

    return cfg;

  }
}
