package com.typingduck.jmmix;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.StringBuilder;


/**
 * Write mbeans in a JSON format suitable for https://github.com/prometheus.
 */
class PrometheusBeanFormatter implements JmxScraper.MBeanFormatter {

    private static final char SEP = '_';
    private PrintWriter io;

    public PrometheusBeanFormatter(PrintWriter io) {
        this.io = io;
    }

    public void recordBean(
                    String domain,
                    LinkedHashMap<String, String> beanProperties,
                    LinkedList<String> attrKeys,
                    String attrName,
                    String attrType,
                    String attrDescription,
                    Object value) {
        if (!validPrometheusType(value)) {
            return;
        }
        printJsonFormat(domain, beanProperties, attrKeys,
                        attrName, attrType, attrDescription, value);
        io.println(",");
    }

    public void printJsonFormat(
                        String domain,
                        LinkedHashMap<String, String> beanProperties,
                        LinkedList<String> attrKeys,
                        String attrName,
                        String attrType,
                        String attrDescription,
                        Object value) {

        String prometheusName = createExportedName(domain, beanProperties, attrKeys, attrName);

        io.println("{");
        io.println("    \"baseLabels\": {");
        io.println("      \"name\": \"" + prometheusName + "\"");
        io.println("    },");
        io.println("    \"metric\": {");
        io.println("        \"type\": \"gauge\",");
        io.println("        \"value\": [{");
        io.println("           " + beanLabels(beanProperties));
        io.println("           \"value\": " + value + "");
        io.println("        }]");
        io.println("    },");
        io.println("    \"docstring\": \"" + attrDescription + "\"");
        io.println("}");
    }

    public void printJsonFormat(
                        String domain,
                        String attrName,
                        Object value) {
        printJsonFormat(domain, new LinkedHashMap<String, String>(),
            new LinkedList<String>(), attrName, "", "", value);
    }


    /**
     * Convert name to a format that won't make prometheus explode.
     */
    static String safeName(String name) {
        return name.replace('.', SEP).replace(' ', SEP).replace('-', SEP);
    }

    static String createExportedName(
                        String domain,
                        LinkedHashMap<String, String> beanProperties,
                        LinkedList<String> attrKeys,
                        String attrName) {
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
        return safeName(name.toString());
    }

    private static String beanLabels(Map<String, String> labels) {
        StringBuilder out = new StringBuilder();
        for (String key : labels.keySet()) {
            if (key.toLowerCase().equals("name")) {
                out.append("\"nom\": \"");
                out.append(labels.get(key));
                out.append("\",");
            }
            else if (!key.toLowerCase().equals("type")) {
                out.append("\"");
                out.append(key);
                out.append("\": \"");
                out.append(labels.get(key));
                out.append("\",");
            }
        }
        if (out.length() == 0) return "";
        else out.deleteCharAt(out.length()-1); // ','

        out.insert(0, "\"labels\": {");
        out.append("},");
        return out.toString();
    }

    public static boolean validPrometheusType(Object value) {
        return JmxScraper.isNumeric(value) && !value.toString().equals("NaN");
    }
}
