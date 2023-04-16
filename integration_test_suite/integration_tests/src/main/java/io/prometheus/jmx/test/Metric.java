/*
 * Copyright 2022-2023 Douglas Hoard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import io.prometheus.jmx.test.util.Precondition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Metric {

    private String line;
    private String name;
    private Map<String, String> labelValueMap;
    private double value;

    /**
     * Constructor
     *
     * @param line
     */
    public Metric(String line) {
        Precondition.notNull(line, "line is null");
        Precondition.notEmpty(line, "line is empty");

        line = line.trim();

        this.line = line;
        name = line.substring(0, line.indexOf(" "));

        int index = name.indexOf("{");
        if (index > 0) {
            name = name.substring(0, index);
        }

        labelValueMap = parseLabels(line);
        value = Double.parseDouble(line.substring(line.lastIndexOf(" ") + 1));
    }

    /**
     * Method to get the raw metric line
     *
     * @return
     */
    public String getLine() {
        return line;
    }

    /**
     * Method to get the metric name
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Method to get a map of label / values
     *
     * @return
     */
    public Map<String, String> getLabels() {
        return labelValueMap;
    }

    /**
     * Method to get the metric value
     *
     * @return
     */
    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metric metric = (Metric) o;
        return line.equals(metric.line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line);
    }

    private static Map<String, String> parseLabels(String line) {
        Map<String, String> map = new LinkedHashMap<>();

        int index = line.indexOf("{");
        int lastIndex = line.lastIndexOf("}");

        if ((index != 0) && (lastIndex != -1)) {
            line = line.substring(line.indexOf("{") + 1, line.lastIndexOf("}"));
            if (line.endsWith(",")) {
                line = line.substring(0, line.length() - 1);
            }

            List<String> tokens = splitOnCommas(line);
            for (String token : tokens) {
                int equalIndex = token.indexOf("=");
                String label = token.substring(0, equalIndex);
                String value = token.substring(equalIndex + 1);
                if (value.startsWith("\"")) {
                    value = value.substring(1);
                }
                if (value.endsWith("\"")) {
                    value = value.substring(0, value.length() - 1);
                }
                map.put(label, value);
            }
        }

        return map;
    }

    private static List<String> splitOnCommas(String input) {
        List<String> result = new ArrayList<>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < input.length(); current++) {
            if (input.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
            else if (input.charAt(current) == ',' && !inQuotes) {
                result.add(input.substring(start, current));
                start = current + 1;
            }
        }
        result.add(input.substring(start));
        return result;
    }
}
