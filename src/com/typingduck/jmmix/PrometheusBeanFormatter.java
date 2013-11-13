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



/*
Copyright (c) 2013 typingduck

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
