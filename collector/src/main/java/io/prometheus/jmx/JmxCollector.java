package io.prometheus.jmx;

import io.prometheus.client.Collector;
import java.io.Reader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class JmxCollector extends Collector {
    private static final Logger LOGGER = Logger.getLogger(JmxCollector.class.getName());

    private static class Rule {
      Pattern pattern;
      String name;
      String help;
      boolean attrNameSnakeCase;
      Type type = Type.GAUGE;
      ArrayList<String> labelNames;
      ArrayList<String> labelValues;
    }

    String hostPort;
    ArrayList<Rule> rules = new ArrayList<Rule>();

    public JmxCollector(Reader in) throws IOException, ParseException {
        this((JSONObject)new JSONParser().parse(in));
    }
    public JmxCollector(String jsonConfig) throws ParseException {
        this((JSONObject)new JSONParser().parse(jsonConfig));
    }

    private JmxCollector(JSONObject config) throws ParseException {
        if (config.containsKey("hostPort")) {
          hostPort = (String)config.get("hostPort");
        } else {
          // Default to local JVM.
          hostPort = "";
        }

        if (config.containsKey("rules")) {
          JSONArray configRules = (JSONArray) config.get("rules");
          for (Object ruleObject : configRules) {
            JSONObject jsonRule = (JSONObject) ruleObject;
            Rule rule = new Rule();
            rules.add(rule);
            if (jsonRule.containsKey("pattern")) {
              rule.pattern = Pattern.compile("^.*" + (String)jsonRule.get("pattern") + ".*$");
            }
            if (jsonRule.containsKey("name")) {
              rule.name = (String)jsonRule.get("name");
            }
            if (jsonRule.containsKey("attrNameSnakeCase")) {
              rule.attrNameSnakeCase = (Boolean)jsonRule.get("attrNameSnakeCase");
            }
            if (jsonRule.containsKey("type")) {
              rule.type = Type.valueOf((String)jsonRule.get("type"));
            }
            if (jsonRule.containsKey("help")) {
              rule.help = (String)jsonRule.get("help");
            }
            if (jsonRule.containsKey("labels")) {
              JSONObject labels = (JSONObject)jsonRule.get("labels");
              rule.labelNames = new ArrayList<String>();
              rule.labelValues = new ArrayList<String>();
              for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>)labels.entrySet()) {
                rule.labelNames.add(entry.getKey());                                
                rule.labelValues.add((String)entry.getValue());
              }
            }

            // Validation.
            if ((rule.labelNames != null || rule.help != null) && rule.name == null) {
              throw new IllegalArgumentException("Must provide name, if help or labels are given: " + jsonRule);
            }
            if (rule.name != null && rule.pattern == null) {
              throw new IllegalArgumentException("Must provide pattern, if name is given: " + jsonRule);
            }
          }
        } else {
          // Default to a single default rule.
          rules.add(new Rule());
        }

    }
    
    class Receiver implements JmxScraper.MBeanReceiver {
      Map<String, MetricFamilySamples> metricFamilySamplesMap =
        new HashMap<String, MetricFamilySamples>();

      private static final char SEP = '_';

      // [] and () are special in regexes, so swtich to <>.
      private String angleBrackets(String s) {
        return "<" + s.substring(1, s.length() - 1) + ">";
      }

      private String safeName(String s) {
        // Change invalid chars to underscore, and merge underscores.
        return s.replaceAll("[^a-zA-Z0-9:_]", "_").replaceAll("__+", "_");
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

      private void defaultExport(
          String domain,
          LinkedHashMap<String, String> beanProperties,
          LinkedList<String> attrKeys,
          String attrName,
          String attrType,
          String help,
          Object value) {
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

        List<String> labelNames = new ArrayList<String>();
        List<String> labelValues = new ArrayList<String>();
        if (beanProperties.size() > 1) {
            Iterator<Map.Entry<String, String>> iter = beanProperties.entrySet().iterator();
            // Skip the first one, it's been used in the name.
            iter.next();
            while (iter.hasNext()) {
              Map.Entry<String, String> entry = iter.next();
              labelNames.add(safeName(entry.getKey()));
              labelValues.add(entry.getValue());
            }
        }

        addSample(new MetricFamilySamples.Sample(fullname, labelNames, labelValues, ((Number)value).doubleValue()),
          Type.GAUGE, help);
      }

      public void recordBean(
          String domain,
          LinkedHashMap<String, String> beanProperties,
          LinkedList<String> attrKeys,
          String attrName,
          String attrType,
          String attrDescription,
          Object value) {

        String beanName = domain +
                   angleBrackets(beanProperties.toString()) +
                   angleBrackets(attrKeys.toString());
        if (!(value instanceof Number)) {
          LOGGER.fine("Ignoring non-Number bean: " + beanName + attrName + ": " + value);
          return;
        }
        // attrDescription tends not to be useful, so give the fully qualified name too.
        String help = attrDescription + " (" + beanName + attrName + ")";

        String attrNameSnakeCase = attrName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();

        for (Rule rule : rules) {
          Matcher matcher = null;
          String matchName = beanName + (rule.attrNameSnakeCase ? attrNameSnakeCase : attrName);
          if (rule.pattern != null) {
            matcher = rule.pattern.matcher(matchName + ": " + value);
            if (!matcher.matches()) {
              continue;
            }
          }
          // If there's no name provided, use default export format.
          if (rule.name == null) {
            defaultExport(domain, beanProperties, attrKeys, rule.attrNameSnakeCase ? attrNameSnakeCase : attrName, attrType, help, value);
            return;
          }
          // matcher is set below here due to validation in the constructor.
          String name = safeName(matcher.replaceAll(rule.name));
          // Set the help.
          if (rule.help != null) {
            help = matcher.replaceAll(rule.help);
          }
          // Set the labels.
          ArrayList<String> labelNames = new ArrayList<String>();
          ArrayList<String> labelValues = new ArrayList<String>();
          if (rule.labelNames != null) {
            for (int i = 0; i < rule.labelNames.size(); i++) {
              labelNames.add(safeName(matcher.replaceAll(rule.labelNames.get(i))));
              labelValues.add(matcher.replaceAll(rule.labelValues.get(i)));
            }
          }
          // Add to samples.
          addSample(new MetricFamilySamples.Sample(name, labelNames, labelValues, ((Number)value).doubleValue()),
              rule.type, help);
          return;
        }
      }

    }

    public List<MetricFamilySamples> collect() {
      Receiver receiver = new Receiver();
      JmxScraper scraper = new JmxScraper(hostPort, receiver);
      long start = System.nanoTime();
      double error = 0;
      try {
        scraper.doScrape();
      } catch (Exception e) {
        error = 1;
        LOGGER.severe("JMX scrape failed: " + e);
      }
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
      return mfsList;
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

