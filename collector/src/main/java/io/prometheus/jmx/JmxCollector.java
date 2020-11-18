package io.prometheus.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import org.yaml.snakeyaml.Yaml;

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
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static java.lang.String.format;

public class JmxCollector extends Collector implements Collector.Describable {
    static final Counter configReloadSuccess = Counter.build()
      .name("jmx_config_reload_success_total")
      .help("Number of times configuration have successfully been reloaded.").register();

    static final Counter configReloadFailure = Counter.build()
      .name("jmx_config_reload_failure_total")
      .help("Number of times configuration have failed to be reloaded.").register();

    private static final Logger LOGGER = Logger.getLogger(JmxCollector.class.getName());

    static class Rule {
      Pattern pattern;
      String name;
      String value;
      Double valueFactor = 1.0;
      String help;
      boolean attrNameSnakeCase;
      boolean cache = false;
      Type type = Type.UNTYPED;
      ArrayList<String> labelNames;
      ArrayList<String> labelValues;
    }

    private static class Config {
      Integer startDelaySeconds = 0;
      String jmxUrl = "";
      String username = "";
      String password = "";
      boolean ssl = false;
      boolean lowercaseOutputName;
      boolean lowercaseOutputLabelNames;
      boolean resetMetrics;      
      List<ObjectName> MBeansToReset = new ArrayList<ObjectName>();
      List<ObjectName> whitelistObjectNames = new ArrayList<ObjectName>();
      List<ObjectName> blacklistObjectNames = new ArrayList<ObjectName>();
      List<Rule> rules = new ArrayList<Rule>();
      long lastUpdate = 0L;

      MatchedRulesCache rulesCache;
    }

    private Config config;
    private File configFile;
    private long createTimeNanoSecs = System.nanoTime();

    private final JmxMBeanPropertyCache jmxMBeanPropertyCache = new JmxMBeanPropertyCache();

    public JmxCollector(File in) throws IOException, MalformedObjectNameException {
        configFile = in;
        config = loadConfig((Map<String, Object>)new Yaml().load(new FileReader(in)));
        config.lastUpdate = configFile.lastModified();
    }

    public JmxCollector(String yamlConfig) throws MalformedObjectNameException {
      config = loadConfig((Map<String, Object>)new Yaml().load(yamlConfig));
    }

    public JmxCollector(InputStream inputStream) throws MalformedObjectNameException {
      config = loadConfig((Map<String, Object>)new Yaml().load(inputStream));
    }

    private void reloadConfig() {
      try {
        FileReader fr = new FileReader(configFile);

        try {
          Map<String, Object> newYamlConfig = (Map<String, Object>)new Yaml().load(fr);
          config = loadConfig(newYamlConfig);
          config.lastUpdate = configFile.lastModified();
          configReloadSuccess.inc();
        } catch (Exception e) {
          LOGGER.severe("Configuration reload failed: " + e.toString());
          configReloadFailure.inc();
        } finally {
          fr.close();
        }

      } catch (IOException e) {
        LOGGER.severe("Configuration reload failed: " + e.toString());
        configReloadFailure.inc();
      }
    }

    private synchronized Config getLatestConfig() {
      if (configFile != null) {
          long mtime = configFile.lastModified();
          if (mtime > config.lastUpdate) {
            LOGGER.fine("Configuration file changed, reloading...");
            reloadConfig();
          }
      }

      return config;
    }

  private Config loadConfig(Map<String, Object> yamlConfig) throws MalformedObjectNameException {
        Config cfg = new Config();

        if (yamlConfig == null) {  // Yaml config empty, set config to empty map.
          yamlConfig = new HashMap<String, Object>();
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
          cfg.jmxUrl ="service:jmx:rmi:///jndi/rmi://" + (String)yamlConfig.get("hostPort") + "/jmxrmi";
        } else if (yamlConfig.containsKey("jmxUrl")) {
          cfg.jmxUrl = (String)yamlConfig.get("jmxUrl");
        }

        if (yamlConfig.containsKey("username")) {
          cfg.username = (String)yamlConfig.get("username");
        }

        if (yamlConfig.containsKey("password")) {
          cfg.password = (String)yamlConfig.get("password");
        }

        if (yamlConfig.containsKey("ssl")) {
          cfg.ssl = (Boolean)yamlConfig.get("ssl");
        }

        if (yamlConfig.containsKey("lowercaseOutputName")) {
          cfg.lowercaseOutputName = (Boolean)yamlConfig.get("lowercaseOutputName");
        }

        if (yamlConfig.containsKey("lowercaseOutputLabelNames")) {
          cfg.lowercaseOutputLabelNames = (Boolean)yamlConfig.get("lowercaseOutputLabelNames");
        }

        if (yamlConfig.containsKey("whitelistObjectNames")) {
          List<Object> names = (List<Object>) yamlConfig.get("whitelistObjectNames");
          for(Object name : names) {
            cfg.whitelistObjectNames.add(new ObjectName((String)name));
          }
        } else {
          cfg.whitelistObjectNames.add(null);
        }

        if (yamlConfig.containsKey("blacklistObjectNames")) {
          List<Object> names = (List<Object>) yamlConfig.get("blacklistObjectNames");
          for (Object name : names) {
            cfg.blacklistObjectNames.add(new ObjectName((String)name));
          }
        }
        
        if (yamlConfig.containsKey("resetMetrics")) {
            try {
                if(((Boolean)(yamlConfig.get("resetMetrics")) != true) && ((Boolean)(yamlConfig.get("resetMetrics")) != false)) {
                    // nonsense value, set the attribute to false
                    cfg.resetMetrics=false;
                }
                else {
                    cfg.resetMetrics = (Boolean)yamlConfig.get("resetMetrics");
                    if (yamlConfig.containsKey("MBeansToReset")) {
                        List<Object> mbeans = (List<Object>) yamlConfig.get("MBeansToReset");
            LOGGER.fine("In JmxCollector: MBeansToReset=" + mbeans);
                        for(Object mbean : mbeans) {
              LOGGER.fine("In JmxCollector: bean=" + mbean);
                          cfg.MBeansToReset.add(new ObjectName((String)mbean));
                        }
                      } else {
                        cfg.MBeansToReset.add(null);
                      }

                }
            }
            catch(Exception e) {
                cfg.resetMetrics=false;
            }
       }

      if (yamlConfig.containsKey("rules")) {
          List<Map<String,Object>> configRules = (List<Map<String,Object>>) yamlConfig.get("rules");
          for (Map<String, Object> ruleObject : configRules) {
            Map<String, Object> yamlRule = ruleObject;
            Rule rule = new Rule();
            cfg.rules.add(rule);
            if (yamlRule.containsKey("pattern")) {
              rule.pattern = Pattern.compile("^.*(?:" + (String)yamlRule.get("pattern") + ").*$");
            }
            if (yamlRule.containsKey("name")) {
              rule.name = (String)yamlRule.get("name");
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
              rule.attrNameSnakeCase = (Boolean)yamlRule.get("attrNameSnakeCase");
            }
            if (yamlRule.containsKey("cache")) {
              rule.cache = (Boolean)yamlRule.get("cache");
            }
            if (yamlRule.containsKey("type")) {
              rule.type = Type.valueOf((String)yamlRule.get("type"));
            }
            if (yamlRule.containsKey("help")) {
              rule.help = (String)yamlRule.get("help");
            }
            if (yamlRule.containsKey("labels")) {
              TreeMap labels = new TreeMap((Map<String, Object>)yamlRule.get("labels"));
              rule.labelNames = new ArrayList<String>();
              rule.labelValues = new ArrayList<String>();
              for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>)labels.entrySet()) {
                rule.labelNames.add(entry.getKey());
                rule.labelValues.add((String)entry.getValue());
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
          cfg.rules.add(new Rule());
        }

        cfg.rulesCache = new MatchedRulesCache(cfg.rules);

        return cfg;

    }

    static String toSnakeAndLowerCase(String attrName) {
      if (attrName == null || attrName.isEmpty()) {
        return attrName;
      }
      char firstChar = attrName.subSequence(0, 1).charAt(0);
      boolean prevCharIsUpperCaseOrUnderscore = Character.isUpperCase(firstChar) || firstChar == '_';
      StringBuilder resultBuilder = new StringBuilder(attrName.length()).append(Character.toLowerCase(firstChar));
      for (char attrChar : attrName.substring(1).toCharArray()) {
        boolean charIsUpperCase = Character.isUpperCase(attrChar);
        if (!prevCharIsUpperCaseOrUnderscore && charIsUpperCase) {
          resultBuilder.append("_");
        }
        resultBuilder.append(Character.toLowerCase(attrChar));
        prevCharIsUpperCaseOrUnderscore = charIsUpperCase || attrChar == '_';
      }
      return resultBuilder.toString();
    }

  /**
   * Change invalid chars to underscore, and merge underscores.
   * @param name Input string
   * @return
   */
  static String safeName(String name) {
      if (name == null) {
        return null;
      }
      boolean prevCharIsUnderscore = false;
      StringBuilder safeNameBuilder = new StringBuilder(name.length());
      if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
        // prevent a numeric prefix.
        safeNameBuilder.append("_");
      }
      for (char nameChar : name.toCharArray()) {
        boolean isUnsafeChar = !JmxCollector.isLegalCharacter(nameChar);
        if ((isUnsafeChar || nameChar == '_')) {
          if (prevCharIsUnderscore) {
            continue;
          } else {
            safeNameBuilder.append("_");
            prevCharIsUnderscore = true;
          }
        } else {
          safeNameBuilder.append(nameChar);
          prevCharIsUnderscore = false;
        }
      }

      return safeNameBuilder.toString();
    }

  private static boolean isLegalCharacter(char input) {
    return ((input == ':') ||
            (input == '_') ||
            (input >= 'a' && input <= 'z') ||
            (input >= 'A' && input <= 'Z') ||
            (input >= '0' && input <= '9'));
  }

    class Receiver implements JmxScraper.MBeanReceiver {
      Map<String, MetricFamilySamples> metricFamilySamplesMap =
        new HashMap<String, MetricFamilySamples>();

      Config config;
      MatchedRulesCache.StalenessTracker stalenessTracker;

      private static final char SEP = '_';

      Receiver(Config config, MatchedRulesCache.StalenessTracker stalenessTracker) {
        this.config = config;
        this.stalenessTracker = stalenessTracker;
      }

      // [] and () are special in regexes, so swtich to <>.
      private String angleBrackets(String s) {
        return "<" + s.substring(1, s.length() - 1) + ">";
      }

      void addSample(MetricFamilySamples.Sample sample, Type type, String help) {
        MetricFamilySamples mfs = metricFamilySamplesMap.get(sample.name);
        if (mfs == null) {
          // JmxScraper.MBeanReceiver is only called from one thread,
          // so there's no race here.
          mfs = new MetricFamilySamples(sample.name, type, help, new ArrayList<MetricFamilySamples.Sample>());
          metricFamilySamplesMap.put(sample.name, mfs);
        }
        mfs.samples.add(sample);
      }

      // Add the matched rule to the cached rules and tag it as not stale
      // if the rule is configured to be cached
      private void addToCache(final Rule rule, final String cacheKey, final MatchedRule matchedRule) {
        if (rule.cache) {
          config.rulesCache.put(rule, cacheKey, matchedRule);
          stalenessTracker.add(rule, cacheKey);
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
          Type type) {
        StringBuilder name = new StringBuilder();
        name.append(domain);
        if (beanProperties.size() > 0) {
            name.append(SEP);
            name.append(beanProperties.values().iterator().next());
        }
        for (String k : attrKeys) {
            name.append(SEP);
            name.append(k);
        }
        name.append(SEP);
        name.append(attrName);
        String fullname = safeName(name.toString());

        if (config.lowercaseOutputName) {
          fullname = fullname.toLowerCase();
        }

        List<String> labelNames = new ArrayList<String>();
        List<String> labelValues = new ArrayList<String>();
        if (beanProperties.size() > 1) {
            Iterator<Map.Entry<String, String>> iter = beanProperties.entrySet().iterator();
            // Skip the first one, it's been used in the name.
            iter.next();
            while (iter.hasNext()) {
              Map.Entry<String, String> entry = iter.next();
              String labelName = safeName(entry.getKey());
              if (config.lowercaseOutputLabelNames) {
                labelName = labelName.toLowerCase();
              }
              labelNames.add(labelName);
              labelValues.add(entry.getValue());
            }
        }

        return new MatchedRule(fullname, matchName, type, help, labelNames, labelValues, value, valueFactor);
      }

      public void recordBean(
          String domain,
          LinkedHashMap<String, String> beanProperties,
          LinkedList<String> attrKeys,
          String attrName,
          String attrType,
          String attrDescription,
          Object beanValue) {

        String beanName = domain + angleBrackets(beanProperties.toString()) + angleBrackets(attrKeys.toString());
        // attrDescription tends not to be useful, so give the fully qualified name too.
        String help = attrDescription + " (" + beanName + attrName + ")";
        String attrNameSnakeCase = toSnakeAndLowerCase(attrName);

        MatchedRule matchedRule = MatchedRule.unmatched();

        for (Rule rule : config.rules) {
          // Rules with bean values cannot be properly cached (only the value from the first scrape will be cached).
          // If caching for the rule is enabled, replace the value with a dummy <cache> to avoid caching different values at different times.
          Object matchBeanValue = rule.cache ? "<cache>" : beanValue;

          String matchName = beanName + (rule.attrNameSnakeCase ? attrNameSnakeCase : attrName) + ": " + matchBeanValue;

          if (rule.cache) {
            MatchedRule cachedRule = config.rulesCache.get(rule, matchName);
            if (cachedRule != null) {
              stalenessTracker.add(rule, matchName);
              if (cachedRule.isMatched()) {
                matchedRule = cachedRule;
                break;
              }

              // The bean was cached earlier, but did not match the current rule.
              // Skip it to avoid matching against the same pattern again
              continue;
            }
          }

          Matcher matcher = null;
          if (rule.pattern != null) {
            matcher = rule.pattern.matcher(matchName);
            if (!matcher.matches()) {
              addToCache(rule, matchName, MatchedRule.unmatched());
              continue;
            }
          }

          Double value = null;
          if (rule.value != null && !rule.value.isEmpty()) {
            String val = matcher.replaceAll(rule.value);
            try {
              value = Double.valueOf(val);
            } catch (NumberFormatException e) {
              LOGGER.fine("Unable to parse configured value '" + val + "' to number for bean: " + beanName + attrName + ": " + beanValue);
              return;
            }
          }

          // If there's no name provided, use default export format.
          if (rule.name == null) {
            matchedRule = defaultExport(matchName, domain, beanProperties, attrKeys, rule.attrNameSnakeCase ? attrNameSnakeCase : attrName, help, value, rule.valueFactor, rule.type);
            addToCache(rule, matchName, matchedRule);
            break;
          }

          // Matcher is set below here due to validation in the constructor.
          String name = safeName(matcher.replaceAll(rule.name));
          if (name.isEmpty()) {
            return;
          }
          if (config.lowercaseOutputName) {
            name = name.toLowerCase();
          }

          // Set the help.
          if (rule.help != null) {
            help = matcher.replaceAll(rule.help);
          }

          // Set the labels.
          ArrayList<String> labelNames = new ArrayList<String>();
          ArrayList<String> labelValues = new ArrayList<String>();
          if (rule.labelNames != null) {
            for (int i = 0; i < rule.labelNames.size(); i++) {
              final String unsafeLabelName = rule.labelNames.get(i);
              final String labelValReplacement = rule.labelValues.get(i);
              try {
                String labelName = safeName(matcher.replaceAll(unsafeLabelName));
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
                        format("Matcher '%s' unable to use: '%s' value: '%s'", matcher, unsafeLabelName, labelValReplacement), e);
              }
            }
          }

          matchedRule = new MatchedRule(name, matchName, rule.type, help, labelNames, labelValues, value, rule.valueFactor);
          addToCache(rule, matchName, matchedRule);
          break;
        }

        if (matchedRule.isUnmatched()) {
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
          LOGGER.fine("Ignoring unsupported bean: " + beanName + attrName + ": " + beanValue);
          return;
        }

        // Add to samples.
        LOGGER.fine("add metric sample: " + matchedRule.name + " " + matchedRule.labelNames + " " + matchedRule.labelValues + " " + value.doubleValue());
        addSample(new MetricFamilySamples.Sample(matchedRule.name, matchedRule.labelNames, matchedRule.labelValues, value.doubleValue()), matchedRule.type, help);
      }

    }

  public List<MetricFamilySamples> collect() {
      // Take a reference to the current config and collect with this one
      // (to avoid race conditions in case another thread reloads the config in the meantime)
      Config config = getLatestConfig();
      JmxScraper scraper;

      MatchedRulesCache.StalenessTracker stalenessTracker = new MatchedRulesCache.StalenessTracker();
      Receiver receiver = new Receiver(config, stalenessTracker);
      if(config.resetMetrics) {
          scraper = new JmxScraper(config.jmxUrl, config.username, config.password, config.ssl,
              config.whitelistObjectNames, config.blacklistObjectNames, receiver, jmxMBeanPropertyCache, config.resetMetrics, config.MBeansToReset);
      } 
      else {
           scraper = new JmxScraper(config.jmxUrl, config.username, config.password, config.ssl,
                  config.whitelistObjectNames, config.blacklistObjectNames, receiver, jmxMBeanPropertyCache);
      }
      long start = System.nanoTime();
      double error = 0;
      if ((config.startDelaySeconds > 0) &&
        ((start - createTimeNanoSecs) / 1000000000L < config.startDelaySeconds)) {
        throw new IllegalStateException("JMXCollector waiting for startDelaySeconds");
      }
      try {
        scraper.doScrape();
      } catch (Exception e) {
        error = 1;
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOGGER.severe("JMX scrape failed: " + sw.toString());
      }
      config.rulesCache.evictStaleEntries(stalenessTracker);

      List<MetricFamilySamples> mfsList = new ArrayList<MetricFamilySamples>();
      mfsList.addAll(receiver.metricFamilySamplesMap.values());
      List<MetricFamilySamples.Sample> samples = new ArrayList<MetricFamilySamples.Sample>();
      samples.add(new MetricFamilySamples.Sample(
          "jmx_scrape_duration_seconds", new ArrayList<String>(), new ArrayList<String>(), (System.nanoTime() - start) / 1.0E9));
      mfsList.add(new MetricFamilySamples("jmx_scrape_duration_seconds", Type.GAUGE, "Time this JMX scrape took, in seconds.", samples));

      samples = new ArrayList<MetricFamilySamples.Sample>();
      samples.add(new MetricFamilySamples.Sample(
          "jmx_scrape_error", new ArrayList<String>(), new ArrayList<String>(), error));
      mfsList.add(new MetricFamilySamples("jmx_scrape_error", Type.GAUGE, "Non-zero if this scrape failed.", samples));
      samples = new ArrayList<MetricFamilySamples.Sample>();
      samples.add(new MetricFamilySamples.Sample(
              "jmx_scrape_cached_beans", new ArrayList<String>(), new ArrayList<String>(), stalenessTracker.cachedCount()));
      mfsList.add(new MetricFamilySamples("jmx_scrape_cached_beans", Type.GAUGE, "Number of beans with their matching rule cached", samples));
      return mfsList;
    }

    public List<MetricFamilySamples> describe() {
      List<MetricFamilySamples> sampleFamilies = new ArrayList<MetricFamilySamples>();
      sampleFamilies.add(new MetricFamilySamples("jmx_scrape_duration_seconds", Type.GAUGE, "Time this JMX scrape took, in seconds.", new ArrayList<MetricFamilySamples.Sample>()));
      sampleFamilies.add(new MetricFamilySamples("jmx_scrape_error", Type.GAUGE, "Non-zero if this scrape failed.", new ArrayList<MetricFamilySamples.Sample>()));
      sampleFamilies.add(new MetricFamilySamples("jmx_scrape_cached_beans", Type.GAUGE, "Number of beans with their matching rule cached", new ArrayList<MetricFamilySamples.Sample>()));
      return sampleFamilies;
    }

    /**
     * Convenience function to run standalone.
     */
    public static void main(String[] args) throws Exception {
      String hostPort = "";
      if (args.length > 0) {
        hostPort = args[0];
      }
      JmxCollector jc = new JmxCollector(("{"
      + "`hostPort`: `" + hostPort + "`,"
      + "}").replace('`', '"'));
      for(MetricFamilySamples mfs : jc.collect()) {
        System.out.println(mfs);
      }
    }
}
