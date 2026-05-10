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
import io.prometheus.metrics.expositionformats.generated.Metrics;
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

/**
 * Parses Prometheus-format metrics from HTTP responses, supporting text, OpenMetrics text,
 * and Protobuf exposition formats.
 */
public class MetricsParser {

    /**
     * Private constructor to prevent instantiation.
     */
    private MetricsParser() {
        // INTENTIONALLY BLANK
    }

    /**
     * Parses metrics from an HTTP response and groups them by metric name.
     *
     * @param httpResponse the HTTP response containing metrics in a supported exposition format
     * @return a map of metric names to their corresponding metric collections
     * @throws MetricsParserException if the response lacks a Content-Type header or the format is unsupported
     */
    public static Map<String, Collection<Metric>> parseMap(HttpResponse httpResponse) {
        return parseCollection(httpResponse).stream()
                .collect(Collectors.groupingBy(Metric::name, Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Parses metrics from an HTTP response based on the Content-Type header.
     *
     * <p>Supports Prometheus text format, OpenMetrics text format, and Protobuf format.
     *
     * @param httpResponse the HTTP response containing metrics in a supported exposition format
     * @return a collection of parsed metrics
     * @throws MetricsParserException if the response lacks a Content-Type header or the format is unsupported
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
                    format("Exception parsing text metrics. No parser for CONTENT-TYPE = [%s]", contentType));
        }
    }

    /**
     * Parses metrics in Prometheus text exposition format.
     *
     * @param body the text body containing Prometheus-format metrics
     * @return a collection of parsed metrics
     * @throws MetricsParserException if parsing fails
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

    /**
     * Parses metrics in OpenMetrics text exposition format.
     *
     * @param body the text body containing OpenMetrics-format metrics
     * @return a collection of parsed metrics
     * @throws MetricsParserException if a metric is found without a preceding TYPE line or parsing fails
     */
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
                        if (type == null) {
                            throw new MetricsParserException(format(
                                    "Exception parsing OpenMetrics text metrics. No TYPE found for metric with HELP [%s]",
                                    help));
                        }

                        if (type.equals("INFO")) {
                            type = "GAUGE";
                        }

                        metrics.add(createMetric(type, help, line));
                    }
                }
            }

            return metrics;
        } catch (MetricsParserException e) {
            throw e;
        } catch (Throwable t) {
            throw new MetricsParserException("Exception parsing OpenMetrics text metrics", t);
        }
    }

    /**
     * Parses metrics in Prometheus Protobuf exposition format.
     *
     * @param bytes the raw bytes containing delimited Protobuf metric families
     * @return a collection of parsed metrics
     * @throws MetricsParserException if an unsupported metric type is encountered or parsing fails
     */
    private static Collection<Metric> parseProtobufMetrics(byte[] bytes) {
        Collection<Metric> collection = new ArrayList<>();

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            while (true) {
                Metrics.MetricFamily metricFamily = Metrics.MetricFamily.parseDelimitedFrom(inputStream);
                if (metricFamily == null) {
                    break;
                }

                String name = metricFamily.getName();
                String help = metricFamily.getHelp();
                Metrics.MetricType metricType = metricFamily.getType();

                for (Metrics.Metric metric : metricFamily.getMetricList()) {
                    switch (metricType) {
                        case COUNTER: {
                            Metrics.Counter counter = metric.getCounter();

                            collection.add(new Metric(
                                    Metric.Type.COUNTER,
                                    help,
                                    name,
                                    toLabels(metric.getLabelList()),
                                    counter.getValue()));

                            break;
                        }
                        case GAUGE: {
                            Metrics.Gauge gauge = metric.getGauge();

                            collection.add(new Metric(
                                    Metric.Type.GAUGE, help, name, toLabels(metric.getLabelList()), gauge.getValue()));

                            break;
                        }
                        case UNTYPED: {
                            Metrics.Untyped untyped = metric.getUntyped();

                            collection.add(new Metric(
                                    Metric.Type.UNTYPED,
                                    help,
                                    name,
                                    toLabels(metric.getLabelList()),
                                    untyped.getValue()));

                            break;
                        }
                        case SUMMARY: {
                            // TODO refactor to support Summary metrics
                            break;
                        }
                        default: {
                            throw new MetricsParserException(format(
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
     * Reads the next HELP line and extracts the metric name and help text.
     *
     * @param lineReader the line reader positioned at a potential HELP line
     * @return the help text content, or {@code null} if no more lines are available
     * @throws IOException if an I/O error occurs while reading
     */
    private static String readHelpLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        if (line != null) {
            line = line.substring("# HELP".length()).trim();
        }
        return line;
    }

    /**
     * Reads the next TYPE line and extracts the metric type.
     *
     * @param lineReader the line reader positioned at a TYPE line
     * @return the metric type string (e.g., COUNTER, GAUGE, UNTYPED)
     * @throws IOException if an I/O error occurs while reading
     */
    private static String readTypeLine(LineReader lineReader) throws IOException {
        String line = lineReader.readLine();
        return line.substring(line.lastIndexOf(" ")).trim();
    }

    /**
     * Reads the next metric data line, returning {@code null} if the next line is a comment.
     *
     * @param lineReader the line reader positioned at a potential metric line
     * @return the metric data line, or {@code null} if the next line starts a new metric group
     * @throws IOException if an I/O error occurs while reading
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
     * Creates a {@link Metric} from parsed type, help, and metric line data.
     *
     * @param type the metric type string (e.g., COUNTER, GAUGE)
     * @param help the metric help text
     * @param metricLine the raw metric line containing name, labels, and value
     * @return a new {@link Metric} instance
     */
    private static Metric createMetric(String type, String help, String metricLine) {
        String name;
        TreeMap<String, String> labels = new TreeMap<>();

        int curlyBraceIndex = metricLine.indexOf("{");
        if (curlyBraceIndex > 1) {
            name = metricLine.substring(0, curlyBraceIndex);
            labels = parseLabels(metricLine.substring(curlyBraceIndex, metricLine.lastIndexOf("}") + 1));
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
     * Parses label key-value pairs from a curly-brace-enclosed label expression.
     *
     * @param labelsLine the label expression including curly braces
     * @return a sorted map of label names to their unquoted values
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
     * Splits a string on commas, respecting quoted segments.
     *
     * @param string the string to split
     * @return a list of tokens split on unquoted commas
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
     * Converts a list of Protobuf label pairs to a sorted map.
     *
     * @param labelPairs the Protobuf label pairs to convert
     * @return a sorted map of label names to their values
     */
    private static TreeMap<String, String> toLabels(List<Metrics.LabelPair> labelPairs) {
        TreeMap<String, String> labels = new TreeMap<>();

        for (Metrics.LabelPair labelPair : labelPairs) {
            labels.put(labelPair.getName(), labelPair.getValue());
        }

        return labels;
    }

    /**
     * Reads a character stream line by line with the ability to push a line back
     * for re-reading, supporting one-line lookahead during metric parsing.
     */
    private static class LineReader implements AutoCloseable {

        private final LinkedList<String> lineBuffer;
        private BufferedReader bufferedReader;

        /**
         * Creates a line reader wrapping the specified reader.
         *
         * @param reader the reader to read lines from; wrapped in a {@link BufferedReader} if not already one
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
         * Reads the next line from the reader, returning from the push-back buffer first if available.
         *
         * @return the next line, or {@code null} if no more lines are available
         * @throws IOException if an I/O error occurs while reading
         */
        public String readLine() throws IOException {
            if (!lineBuffer.isEmpty()) {
                return lineBuffer.removeLast();
            } else {
                return bufferedReader.readLine();
            }
        }

        /**
         * Pushes a line back onto the buffer so it will be returned by the next {@link #readLine()} call.
         *
         * @param line the line to push back
         */
        public void unreadLine(String line) {
            lineBuffer.add(line);
        }

        /**
         * Closes the underlying reader and clears the push-back buffer.
         */
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
