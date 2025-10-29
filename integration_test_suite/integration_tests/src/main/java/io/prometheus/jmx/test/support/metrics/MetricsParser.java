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

package io.prometheus.jmx.test.support.metrics;

import static java.lang.String.format;

import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_4_33_0.Metrics;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Class to parse Metrics from an HttpResponse */
public class MetricsParser {

    /** Constructor */
    private MetricsParser() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to parse Metrics from an HttpResponse
     *
     * @param httpResponse httpResponse
     * @return a Map of Metrics
     */
    public static Map<String, Collection<Metric>> parseMap(HttpResponse httpResponse) {
        return parseCollection(httpResponse).stream()
                .collect(
                        Collectors.groupingBy(
                                Metric::name, Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Method to parse Metrics from an HttpResponse
     *
     * @param httpResponse httpResponse
     * @return a Collection of Metrics
     */
    public static Collection<Metric> parseCollection(HttpResponse httpResponse) {
        if (httpResponse.headers().get(HttpHeader.CONTENT_TYPE) == null) {
            throw new MetricsParserException(
                    format("Exception parsing metrics. No %s header", HttpHeader.CONTENT_TYPE));
        }

        String contentType = httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0);

        if (contentType.contains("text/plain")) {
            return parseTextMetrics(httpResponse.body().string());
        } else if (contentType.contains("application/openmetrics-text")) {
            return parseOpenMetricsTextMetrics(httpResponse.body().string());
        } else if (contentType.contains("application/vnd.google.protobuf")) {
            return parseProtobufMetrics(httpResponse.body().bytes());
        } else {
            throw new MetricsParserException(
                    format(
                            "Exception parsing text metrics. No parser for CONTENT-TYPE = [%s]",
                            contentType));
        }
    }

    /**
     * Method to parse test Metrics
     *
     * @param body body
     * @return a Collection of Metrics
     */
    private static Collection<Metric> parseTextMetrics(String body) {
        Collection<Metric> metrics = new ArrayList<>();

        try (LineReader lineReader = new LineReader(new StringReader(body))) {
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
                    metrics.add(createMetric(typeLine, helpLine, metricLine));
                }
            }

            return metrics;
        } catch (Throwable t) {
            throw new MetricsParserException("Exception parsing text metrics", t);
        }
    }

    private static Collection<Metric> parseOpenMetricsTextMetrics(String body) {
        Collection<Metric> metrics = new ArrayList<>();

        String line;

        try (LineReader lineReader = new LineReader(new StringReader(body))) {
            while (true) {
                String type = null;
                String help = null;

                while (true) {
                    line = lineReader.readLine();
                    if (line == null) {
                        break;
                    }

                    if (line.startsWith("# EOF")) {
                        break;
                    }

                    if (line.startsWith("# TYPE")) {
                        type = line.substring(line.lastIndexOf(" ")).trim().toUpperCase(Locale.US);
                    } else if (line.startsWith("# UNIT")) {
                        // INTENTIONALLY BLANK
                    } else if (line.startsWith("# HELP")) {
                        help = line.substring("# HELP".length()).trim();
                        break;
                    }
                }

                if (line == null) {
                    break;
                }

                while (true) {
                    line = lineReader.readLine();
                    if (line == null) {
                        break;
                    }

                    if (line.startsWith("# EOF")) {
                        break;
                    } else if (line.startsWith("#")) {
                        lineReader.unreadLine(line);
                        break;
                    } else {
                        if (type.equals("INFO")) {
                            type = "GAUGE";
                        }

                        metrics.add(createMetric(type, help, line));
                    }
                }
            }

            return metrics;
        } catch (Throwable t) {
            throw new MetricsParserException("Exception parsing OpenMetrics text metrics", t);
        }
    }

    /**
     * Method to parse Protobuf Metrics
     *
     * @param bytes bytes
     * @return a Collection of metrics
     */
    private static Collection<Metric> parseProtobufMetrics(byte[] bytes) {
        Collection<Metric> collection = new ArrayList<>();

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            while (true) {
                Metrics.MetricFamily metricFamily =
                        Metrics.MetricFamily.parseDelimitedFrom(inputStream);
                if (metricFamily == null) {
                    break;
                }

                String name = metricFamily.getName();
                String help = metricFamily.getHelp();
                Metrics.MetricType metricType = metricFamily.getType();

                for (Metrics.Metric metric : metricFamily.getMetricList()) {
                    switch (metricType) {
                        case COUNTER:
                            {
                                Metrics.Counter counter = metric.getCounter();

                                collection.add(
                                        new Metric(
                                                Metric.Type.COUNTER,
                                                help,
                                                name,
                                                toLabels(metric.getLabelList()),
                                                counter.getValue()));

                                break;
                            }
                        case GAUGE:
                            {
                                Metrics.Gauge gauge = metric.getGauge();

                                collection.add(
                                        new Metric(
                                                Metric.Type.GAUGE,
                                                help,
                                                name,
                                                toLabels(metric.getLabelList()),
                                                gauge.getValue()));

                                break;
                            }
                        case UNTYPED:
                            {
                                Metrics.Untyped untyped = metric.getUntyped();

                                collection.add(
                                        new Metric(
                                                Metric.Type.UNTYPED,
                                                help,
                                                name,
                                                toLabels(metric.getLabelList()),
                                                untyped.getValue()));

                                break;
                            }
                        case SUMMARY:
                            {
                                // TODO refactor to support Summary metrics
                                break;
                            }
                        default:
                            {
                                throw new MetricsParserException(
                                        format(
                                                "Exception parsing Protobuf metrics. MetricsParser"
                                                        + " doesn't support metric type [%s]",
                                                metricType));
                            }
                    }
                }
            }

            return collection;
        } catch (MetricsParserException e) {
            throw e;
        } catch (Throwable t) {
            throw new MetricsParserException("Exception parsing Protobuf metrics", t);
        }
    }

    /**
     * Method to read the HELP line
     *
     * @param lineReader lineReader
     * @return the HELP line
     * @throws IOException IOException
     */
    private static String readHelpLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        if (line != null) {
            line = line.substring("# HELP".length()).trim();
        }
        return line;
    }

    /**
     * Method to read the TYPE line
     *
     * @param lineReader lineReader
     * @return the TYPE line
     * @throws IOException IOException
     */
    private static String readTypeLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        return line.substring(line.lastIndexOf(" ")).trim();
    }

    /**
     * Method to read the metric line
     *
     * @param lineReader lineReader
     * @return the metric line
     * @throws IOException IOException
     */
    private static String readMetricLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        if (line != null && line.startsWith("#")) {
            lineReader.unreadLine(line);
            return null;
        }
        return line;
    }

    /**
     * Method to create a Metric
     *
     * @param type type
     * @param help help
     * @param metricLine metricLine
     * @return a Metric
     */
    private static Metric createMetric(String type, String help, String metricLine) {
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

        if (type.equalsIgnoreCase("COUNTER")) {
            return new Metric(Metric.Type.COUNTER, help, name, labels, value);
        } else if (type.equalsIgnoreCase("GAUGE")) {
            return new Metric(Metric.Type.GAUGE, help, name, labels, value);
        } else {
            return new Metric(Metric.Type.UNTYPED, help, name, labels, value);
        }
    }

    /**
     * Method to parse labels
     *
     * @param labelsLine labelsLine
     * @return a TreeMap of labels
     */
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

    /**
     * Method to split a String on commas
     *
     * @param string string
     * @return a List of String tokens
     */
    private static List<String> splitOnCommas(String string) {
        List<String> result = new ArrayList<>();
        int start = 0;
        boolean inQuotes = false;
        for (int current = 0; current < string.length(); current++) {
            if (string.charAt(current) == '\"') inQuotes = !inQuotes; // toggle state
            else if (string.charAt(current) == ',' && !inQuotes) {
                result.add(string.substring(start, current));
                start = current + 1;
            }
        }
        result.add(string.substring(start));
        return result;
    }

    /**
     * Method to convert a List of Metrics.LabelPair to a TreeMap
     *
     * @param labelPairs labelPairs
     * @return a TreeMap of labels
     */
    private static TreeMap<String, String> toLabels(List<Metrics.LabelPair> labelPairs) {
        TreeMap<String, String> labels = new TreeMap<>();

        for (Metrics.LabelPair labelPair : labelPairs) {
            labels.put(labelPair.getName(), labelPair.getValue());
        }

        return labels;
    }

    /** Class to read a Reader line by line with the ability to push a line back to the Reader */
    private static class LineReader implements AutoCloseable {

        private final LinkedList<String> lineBuffer;
        private BufferedReader bufferedReader;

        /**
         * Constructor
         *
         * @param reader reader
         */
        public LineReader(Reader reader) {
            if (reader instanceof BufferedReader) {
                this.bufferedReader = (BufferedReader) reader;
            } else {
                this.bufferedReader = new BufferedReader(reader);
            }
            this.lineBuffer = new LinkedList<>();
        }

        /**
         * Method to read a line from the reader
         *
         * @return a line or null of no more lines are available
         * @throws IOException IOException
         */
        public String readLine() throws IOException {
            if (!lineBuffer.isEmpty()) {
                return lineBuffer.removeLast();
            } else {
                return bufferedReader.readLine();
            }
        }

        /**
         * Method to unread (push) a line back to the reader
         *
         * @param line line
         */
        public void unreadLine(String line) {
            lineBuffer.add(line);
        }

        @Override
        public void close() {
            lineBuffer.clear();

            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {
                    // INTENTIONALLY BLANK
                }

                bufferedReader = null;
            }
        }
    }
}
