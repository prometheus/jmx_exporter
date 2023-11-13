/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.support.metrics.text;

import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.metrics.text.util.LineReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

public class TextMetricsParser {

    /** Constructor */
    private TextMetricsParser() {
        // DO NOTING
    }

    /**
     * Method to parse an HttpResponse that contains text format metrics
     *
     * @param httpResponse httpResponse
     * @return a Collection of TextMetrics
     * @throws TextMetricsParserException TextMetricsParserException
     */
    public static Collection<TextMetric> parse(HttpResponse httpResponse)
            throws TextMetricsParserException {
        return parse(httpResponse.body().string());
    }

    /**
     * Method to parse a String that contains text format metrics
     *
     * @param string string
     * @return a Collection of TextMetrics
     * @throws TextMetricsParserException TextMetricsParserException
     */
    public static Collection<TextMetric> parse(String string) throws TextMetricsParserException {
        Collection<TextMetric> metrics = new ArrayList<>();

        try (LineReader lineReader = new LineReader(string)) {
            String typeLine;
            String helpLine;

            while (true) {
                helpLine = readHelpLine(lineReader);
                if (helpLine == null) {
                    break;
                }

                typeLine = readTypeLine(lineReader);

                while (true) {
                    String metricLine = readMetricLine(lineReader);
                    if (metricLine == null) {
                        break;
                    }
                    metrics.add(parseMetric(typeLine, helpLine, metricLine));
                }
            }

            return metrics;
        } catch (Throwable t) {
            throw new TextMetricsParserException("Exception parsing text metrics", t);
        }
    }

    private static String readHelpLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        if (line != null) {
            line = line.substring("# HELP".length()).trim();
        }
        return line;
    }

    private static String readTypeLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        return line.substring(line.lastIndexOf(" ")).trim();
    }

    private static String readMetricLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        if (line != null && line.startsWith("#")) {
            lineReader.unreadLine(line);
            return null;
        }
        return line;
    }

    private static TextMetric parseMetric(String typeLine, String helpLine, String metricLine) {
        String help = helpLine.substring("# HELP".length());
        String name;
        TreeMap<String, String> labels = new TreeMap<>();

        int curlyBraceIndex = metricLine.indexOf("{");
        if (curlyBraceIndex > 1) {
            name = metricLine.substring(0, curlyBraceIndex);
            labels =
                    parseLabels(
                            metricLine.substring(curlyBraceIndex, metricLine.lastIndexOf("}") + 1));
        } else {
            name = metricLine.substring(0, metricLine.indexOf(" "));
        }

        double value = Double.parseDouble(metricLine.substring(metricLine.lastIndexOf(" ")));

        if (typeLine.equalsIgnoreCase("COUNTER")) {
            return new TextCounterMetric(name, help, labels, value);
        } else if (typeLine.equalsIgnoreCase("GAUGE")) {
            return new TextGaugeMetric(name, help, labels, value);
        } else {
            return new TextUntypedMetric(name, help, labels, value);
        }
    }

    private static TreeMap<String, String> parseLabels(String labelsLine) {
        if (labelsLine.endsWith(",")) {
            labelsLine = labelsLine.substring(0, labelsLine.length() - 1);
        }

        labelsLine = labelsLine.substring(1, labelsLine.length() - 1);

        TreeMap<String, String> map = new TreeMap<>();

        List<String> tokens = splitOnCommas(labelsLine);
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
