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
import static org.assertj.core.api.Assertions.assertThat;

import io.prometheus.jmx.test.support.http.HttpHeader;
import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.jmx.test.support.http.HttpResponseBody;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.paramixel.api.exception.FailException;

/**
 * Loads, verifies, and writes line-oriented metric assertion files.
 *
 * <p>Each assertion is a single line. The first word is the keyword:
 *
 * <ul>
 *   <li>{@code match TYPE name} — metric exists (presence check)
 *   <li>{@code match TYPE name{key="val"}} — metric exists with labels
 *   <li>{@code match TYPE name{key="val"} 1.23} — metric exists with labels and exact value
 *   <li>{@code present PREFIX} — at least one metric with this prefix
 *   <li>{@code absent PREFIX} — no metric with this prefix
 * </ul>
 *
 * <p>Labels use Prometheus-like inline syntax: {@code {key1="value1",key2="value2"}}. Label values
 * support UTF-8 and use Prometheus-style escaping for special characters ({@code \"}, {@code \\},
 * {@code \n}, {@code \t}).
 *
 * <p>The file is stored under a test resource path derived from the test class, exporter mode, and
 * Java Docker image:
 *
 * <pre>
 * io/prometheus/jmx/test/core/BasicTest/JavaAgent/amazoncorretto_17--01234567.txt
 * </pre>
 */
public class MetricsAssertions {

    /**
     * System property that updates metric assertion files even if they already exist. This
     * is useful for regenerating files after changes to the exporter or tests, but should be used
     * with caution to avoid accidentally overwriting curated expectations.
     */
    public static final String METRIC_ASSERTIONS_UPDATE = "metric.assertions.update";

    /**
     * Metric name prefixes whose metrics should be completely excluded from assertion files. These
     * are runtime-varying metrics that may not be present in all response formats.
     */
    private static final Set<String> EXCLUDE_METRIC_NAME_PREFIXES = new HashSet<>(List.of(
            // Vendor / VM implementation-specific
            "com_ibm_",
            "com_sun_management_",
            "sun_management_",
            "io_graalvm_",
            "org_graalvm_",
            "jvm_runtime_info_",

            // JDK-version / module-specific
            "jdk_management_jfr_",
            "jdk_management_virtualthreadscheduler_",

            // GC collector-name specific
            "java_lang_G1_",
            "java_lang_G1C_",
            "java_lang_ZGC_",
            "java_lang_Shenandoah_",
            "java_lang_PS_",
            "java_lang_ParNew_",
            "java_lang_ConcurrentMarkSweep_",
            "java_lang_Copy_",
            "java_lang_MarkSweepCompact_",
            "java_lang_Epsilon_",

            // Memory pool / memory manager names vary by GC and JDK
            "java_lang_MemoryPool_",
            "java_lang_MemoryManager_",
            "java_lang_Code_Cache_",
            "java_lang_CodeHeap_",
            "java_lang_Compressed_Class_Space_",
            "java_lang_Metaspace_",
            "java_lang_G1_Eden_Space_",
            "java_lang_G1_Old_Gen_",
            "java_lang_G1_Survivor_Space_",
            "java_lang_GarbageCollector_",
            "java_lang_PS_Eden_Space_",
            "java_lang_PS_Old_Gen_",
            "java_lang_PS_Survivor_Space_",
            "java_lang_Eden_Space_",
            "java_lang_Survivor_Space_",
            "java_lang_Tenured_Gen_",
            "java_lang_ZHeap_",

            // NIO buffer pools can vary by JDK/platform
            "java_nio_",

            // client_java JVM metrics that are GC/runtime-state dependent
            "jvm_gc_",
            "jvm_memory_pool_allocated_bytes_total",
            "jvm_memory_pool_collection_"));

    /**
     * Canonical label value suffixes for metric families whose label values
     * vary by runtime environment (e.g., garbage collector).
     *
     * <p>Key: metric name prefix. Value: (label key {@code ->} set of canonical suffixes).
     *
     * <p>When writing a golden file, if a metric name starts with a key and
     * the metric has a matching label key, the label value is tested against
     * each canonical suffix via {@code endsWith}. If it matches, the canonical
     * suffix is written prefixed with {@code *}.
     *
     * <p>When verifying, if an expected label value starts with {@code *},
     * the actual value is matched via {@code endsWith} against the
     * remainder of the expected value.
     */
    private static final Map<String, Map<String, Set<String>>> LABEL_SUFFIX_CANONICALIZATION =
            Map.of("jvm_memory_pool_", Map.of("pool", Set.of("Eden Space", "Survivor Space", "Gen")));

    /**
     * Metric name prefixes whose values are runtime-specific and should be written as
     * {@code *} (any value acceptable) in assertion files.
     */
    private static final Set<String> RUNTIME_SPECIFIC_VALUE_PREFIXES = new HashSet<>(List.of(
            // Standard JVM/JMX domains that should generally exist, but values are runtime-dependent.
            "java_lang_",
            "java_util_logging_",

            // client_java JVM metrics. Names generally exist when registered, but values/labels
            // vary by runtime.
            "jvm_",

            // Process metrics. Values are inherently runtime/host dependent.
            "process_",

            // JMX Exporter metrics.
            "jmx_build_info",
            "jmx_scrape_duration_",
            "jmx_scrape_cached_beans",

            // Test/demo metrics that intentionally change.
            "io_prometheus_jmx_autoIncrementing_",
            "auto_increment_counter"));

    private static final double VALUE_TOLERANCE = 1e-10;
    private static final String WRITE_DIRECTORY_PROPERTY = "metricAssertions.write.dir";
    private static final String DEFAULT_RESOURCE_DIRECTORY =
            "integration_test_suite/integration_tests/src/test/resources";
    private static final String MODULE_RESOURCE_DIRECTORY = "src/test/resources";

    private final String resourcePath;
    private final List<MetricExpectation> metricExpectations;
    private final List<PrefixExpectation> prefixExpectations;

    private MetricsAssertions(
            String resourcePath,
            List<MetricExpectation> metricExpectations,
            List<PrefixExpectation> prefixExpectations) {
        this.resourcePath = resourcePath;
        this.metricExpectations = metricExpectations;
        this.prefixExpectations = prefixExpectations;
    }

    /**
     * Verifies parsed metrics against the assertion file for the test class, mode, and Java Docker
     * image. If the file does not exist or force-write is enabled, writes the file first, then
     * verifies.
     *
     * @param testClass the test class
     * @param mode the exporter mode name
     * @param javaDockerImage the Java Docker image name
     * @param metrics the parsed metrics
     */
    public static void assertMetrics(
            Class<?> testClass, String mode, String javaDockerImage, Collection<Metric> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");

        String resourcePath = resourcePath(testClass, mode, javaDockerImage);
        Path path = filePath(resourcePath);

        if (Files.exists(path) && !updateMetricsFile()) {
            readMetricsFile(testClass, mode, javaDockerImage).verify(metrics);
        } else {
            writeMetricsFile(testClass, mode, javaDockerImage, metrics);
            readMetricsFile(testClass, mode, javaDockerImage).verify(metrics);
        }
    }

    /**
     * Verifies parsed metrics against the assertion file for the test class, mode, and Java Docker
     * image. If the file does not exist or force-write is enabled, writes the file first, then
     * verifies.
     *
     * @param testClass       the test class
     * @param javaDockerImage the Java Docker image name
     * @param mode            the exporter mode name
     * @param metrics         the parsed metrics grouped by metric name
     */
    public static void assertMetrics(
            Class<?> testClass, String javaDockerImage, String mode, Map<String, Collection<Metric>> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");
        assertMetrics(testClass, mode, javaDockerImage, flatten(metrics));
    }

    /**
     * Loads the set of metric names from the assertion file for the given test class, mode, and
     * Java Docker image.
     *
     * <p>The file must exist. Callers must ensure {@link #assertMetrics(Class, String, String,
     * Collection)} has been called first to generate the file if it does not yet exist.
     *
     * @param testClass the test class
     * @param mode the exporter mode name
     * @param javaDockerImage the Java Docker image name
     * @return set of metric names from the file
     * @throws FailException if the file does not exist or cannot be read
     */
    public static Set<String> loadMetricNames(Class<?> testClass, String mode, String javaDockerImage) {
        String resourcePath = resourcePath(testClass, mode, javaDockerImage);
        Path path = filePath(resourcePath);

        if (!Files.exists(path)) {
            throw new FailException(format(
                    "Metric assertion file not found [%s]; " + "run assertMetricsResponse first to generate it",
                    resourcePath));
        }

        try {
            String content = loadContent(resourcePath);
            return parseMetricNames(content);
        } catch (IOException e) {
            throw new FailException(
                    format("Unable to load metric assertion resource [%s]: %s", resourcePath, e.getMessage()));
        }
    }

    /**
     * Loads the metric assertion file for the test class, mode, and Java Docker image from the
     * classpath or filesystem.
     *
     * @param testClass the test class
     * @param mode the exporter mode name
     * @param javaDockerImage the Java Docker image name
     * @return the loaded metric assertions
     */
    private static MetricsAssertions readMetricsFile(Class<?> testClass, String mode, String javaDockerImage) {
        String resourcePath = resourcePath(testClass, mode, javaDockerImage);
        try {
            return parse(resourcePath, loadContent(resourcePath));
        } catch (IOException e) {
            throw new FailException(
                    format("Unable to load metric assertion resource [%s]: %s", resourcePath, e.getMessage()));
        }
    }

    /**
     * Writes a metric assertion file for the test class, mode, Java Docker image, and parsed
     * metrics.
     *
     * <p>If the file already exists, this method is a no-op unless {@link
     * #METRIC_ASSERTIONS_UPDATE} is set to {@code true}.
     *
     * @param testClass the test class
     * @param mode the exporter mode name
     * @param javaDockerImage the Java Docker image name
     * @param metrics the parsed metrics
     * @return the path written
     */
    private static Path writeMetricsFile(
            Class<?> testClass, String mode, String javaDockerImage, Collection<Metric> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");

        String resourcePath = resourcePath(testClass, mode, javaDockerImage);
        Path path = filePath(resourcePath);

        boolean forceRewrite = updateMetricsFile();

        if (Files.exists(path) && !forceRewrite) {
            return path;
        }

        String content = formatWrite(testClass, mode, javaDockerImage, sort(metrics));

        try {
            Files.createDirectories(path.getParent());

            Files.writeString(
                    path,
                    content,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            return path;
        } catch (IOException e) {
            throw new FailException(format("Unable to write metric assertion resource [%s]: %s", path, e.getMessage()));
        }
    }

    /**
     * Verifies the parsed metrics against these assertions.
     *
     * @param metrics the parsed metrics
     */
    private void verify(Collection<Metric> metrics) {
        Objects.requireNonNull(metrics, "metrics is null");

        Collection<Metric> sortedMetrics = sort(metrics);

        assertNoDuplicates(sortedMetrics);

        for (MetricExpectation expectation : metricExpectations) {
            expectation.verify(resourcePath, sortedMetrics);
        }

        for (PrefixExpectation expectation : prefixExpectations) {
            expectation.verify(resourcePath, sortedMetrics);
        }
    }

    private static MetricsAssertions parse(String resourcePath, String content) {
        List<MetricExpectation> metricExpectations = new ArrayList<>();
        List<PrefixExpectation> prefixExpectations = new ArrayList<>();

        String[] lines = content.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i].trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("match ")) {
                metricExpectations.add(parseMatchLine(resourcePath, lineNumber, line.substring("match ".length())));
            } else if (line.startsWith("present ")) {
                prefixExpectations.add(new PrefixExpectation(line.substring("present ".length()), true));
            } else if (line.startsWith("absent ")) {
                prefixExpectations.add(new PrefixExpectation(line.substring("absent ".length()), false));
            } else {
                throw parseException(resourcePath, lineNumber, "Unsupported statement");
            }
        }

        return new MetricsAssertions(resourcePath, metricExpectations, prefixExpectations);
    }

    /**
     * Extracts metric names from assertion file content. Parses {@code match} lines and returns
     * the unique set of metric names, stripping labels and values.
     *
     * @param content the file content
     * @return sorted set of metric names
     */
    private static Set<String> parseMetricNames(String content) {
        Set<String> names = new TreeSet<>();
        for (String line : content.split("\\R", -1)) {
            line = line.trim();
            if (!line.startsWith("match ")) {
                continue;
            }
            String rest = line.substring("match ".length());
            int spaceIndex = rest.indexOf(' ');
            if (spaceIndex < 0) continue;
            String afterType = rest.substring(spaceIndex + 1).trim();
            int braceIndex = afterType.indexOf('{');
            String name = (braceIndex >= 0) ? afterType.substring(0, braceIndex) : afterType;
            int lastSpace = name.lastIndexOf(' ');
            if (lastSpace > 0) {
                String maybeValue = name.substring(lastSpace + 1);
                if ("*".equals(maybeValue) || isNumeric(maybeValue)) {
                    name = name.substring(0, lastSpace);
                }
            }
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    /**
     * Parses a match line: {@code TYPE name{key="val"} 123.45}
     *
     * <p>Format: TYPE (whitespace) NAME (optional labels) (optional value)
     */
    private static MetricExpectation parseMatchLine(String resourcePath, int lineNumber, String rest) {
        // Extract TYPE (first whitespace-delimited token)
        int spaceIndex = rest.indexOf(' ');
        if (spaceIndex < 0) {
            throw parseException(resourcePath, lineNumber, "Expected [match TYPE name ...]");
        }
        String typeStr = rest.substring(0, spaceIndex);
        Metric.Type type;
        try {
            type = Metric.Type.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw parseException(resourcePath, lineNumber, format("Unknown metric type [%s]", typeStr));
        }

        String nameAndLabelsAndValue = rest.substring(spaceIndex + 1);

        // Find name: up to '{' if labels present, otherwise parse manually
        int braceIndex = nameAndLabelsAndValue.indexOf('{');
        String name;
        String labelsAndValue;

        if (braceIndex >= 0) {
            name = nameAndLabelsAndValue.substring(0, braceIndex);
            labelsAndValue = nameAndLabelsAndValue.substring(braceIndex);
        } else {
            // No labels — name is up to the last space (if a value follows)
            int lastSpace = nameAndLabelsAndValue.lastIndexOf(' ');
            if (lastSpace > 0) {
                String maybeValue = nameAndLabelsAndValue.substring(lastSpace + 1);
                if ("*".equals(maybeValue) || isNumeric(maybeValue)) {
                    // It's a value (wildcard or numeric)
                    name = nameAndLabelsAndValue.substring(0, lastSpace);
                    labelsAndValue = " " + maybeValue; // prefix with space for uniform handling
                } else {
                    // Not a value, entire string is the name
                    name = nameAndLabelsAndValue;
                    labelsAndValue = "";
                }
            } else {
                name = nameAndLabelsAndValue;
                labelsAndValue = "";
            }
        }

        if (name.isBlank()) {
            throw parseException(resourcePath, lineNumber, "Metric name is required");
        }

        // Parse labels and value from labelsAndValue
        TreeMap<String, String> labels = new TreeMap<>();
        Double value = null;

        if (!labelsAndValue.isEmpty()) {
            String remaining = labelsAndValue;

            // Parse labels if present
            if (remaining.startsWith("{")) {
                int closeBrace = findClosingBrace(remaining);
                if (closeBrace < 0) {
                    throw parseException(resourcePath, lineNumber, "Unterminated label braces");
                }
                String labelsStr = remaining.substring(1, closeBrace);
                labels = parseLabels(resourcePath, lineNumber, labelsStr);
                remaining = remaining.substring(closeBrace + 1).trim();
            }

            // Parse value if present
            if (!remaining.isEmpty()) {
                String trimmed = remaining.trim();
                if ("*".equals(trimmed)) {
                    value = null; // wildcard: any value is acceptable
                } else {
                    try {
                        value = Double.parseDouble(trimmed);
                    } catch (NumberFormatException e) {
                        throw parseException(
                                resourcePath, lineNumber, format("Expected numeric value or *, got [%s]", trimmed));
                    }
                }
            }
        }

        return new MetricExpectation(type, name, labels, value);
    }

    private static int findClosingBrace(String str) {
        int depth = 0;
        boolean inQuote = false;
        boolean escaped = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\' && inQuote) {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inQuote = !inQuote;
                continue;
            }

            if (!inQuote) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    /**
     * Parses label pairs from a string like: {@code key1="value1",key2="value2"}
     */
    private static TreeMap<String, String> parseLabels(String resourcePath, int lineNumber, String labelsStr) {
        TreeMap<String, String> labels = new TreeMap<>();
        int i = 0;

        while (i < labelsStr.length()) {
            // Skip whitespace
            while (i < labelsStr.length() && Character.isWhitespace(labelsStr.charAt(i))) {
                i++;
            }
            if (i >= labelsStr.length()) {
                break;
            }

            // Parse key
            int eqIndex = labelsStr.indexOf('=', i);
            if (eqIndex < 0) {
                throw parseException(resourcePath, lineNumber, "Expected '=' in label");
            }
            String key = labelsStr.substring(i, eqIndex).trim();
            i = eqIndex + 1;

            // Parse value (quoted)
            if (i >= labelsStr.length() || labelsStr.charAt(i) != '"') {
                throw parseException(resourcePath, lineNumber, "Expected '\"' to start label value");
            }
            i++; // skip opening quote

            StringBuilder valueBuilder = new StringBuilder();
            while (i < labelsStr.length()) {
                char c = labelsStr.charAt(i);
                if (c == '\\' && i + 1 < labelsStr.length()) {
                    char next = labelsStr.charAt(i + 1);
                    switch (next) {
                        case '"':
                            valueBuilder.append('"');
                            break;
                        case '\\':
                            valueBuilder.append('\\');
                            break;
                        case 'n':
                            valueBuilder.append('\n');
                            break;
                        case 't':
                            valueBuilder.append('\t');
                            break;
                        default:
                            valueBuilder.append(c);
                            valueBuilder.append(next);
                            break;
                    }
                    i += 2;
                } else if (c == '"') {
                    i++; // skip closing quote
                    break;
                } else {
                    valueBuilder.append(c);
                    i++;
                }
            }

            labels.put(key, valueBuilder.toString());

            // Skip comma
            while (i < labelsStr.length() && Character.isWhitespace(labelsStr.charAt(i))) {
                i++;
            }
            if (i < labelsStr.length() && labelsStr.charAt(i) == ',') {
                i++;
            }
        }

        return labels;
    }

    private static String loadContent(String resourcePath) throws IOException {
        Path path = filePath(resourcePath);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static FailException parseException(String resourcePath, int lineNumber, String message) {
        return new FailException(format("%s at [%s:%d]", message, resourcePath, lineNumber));
    }

    private static String formatWrite(
            Class<?> testClass, String mode, String javaDockerImage, Collection<Metric> metrics) {
        StringBuilder builder = new StringBuilder();
        builder.append("# metric-assertions-version 1\n");
        builder.append("# ")
                .append(testClass.getName())
                .append(' ')
                .append(mode)
                .append(' ')
                .append(javaDockerImage)
                .append("\n\n");

        metrics.stream().filter(metric -> !isRuntimeNameExcluded(metric.name())).forEach(metric -> {
            builder.append("match ").append(metric.type()).append(' ').append(metric.name());
            if (!metric.labels().isEmpty()) {
                builder.append('{');
                boolean first = true;
                for (Map.Entry<String, String> entry : metric.labels().entrySet()) {
                    if (!first) {
                        builder.append(',');
                    }
                    builder.append(entry.getKey())
                            .append("=\"")
                            .append(escapeLabelValue(
                                    canonicalizeLabelValue(metric.name(), entry.getKey(), entry.getValue())))
                            .append('"');
                    first = false;
                }
                builder.append('}');
            }
            builder.append(' ');
            if (isRuntimeSpecificValue(metric.name())) {
                builder.append('*');
            } else {
                builder.append(metric.value());
            }
            builder.append('\n');
        });

        return builder.toString();
    }

    /**
     * Escapes a label value using Prometheus-style escaping: {@code \}, {@code "}, newline, and tab.
     */
    private static String escapeLabelValue(String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }

    private static Comparator<Metric> metricComparator() {
        return Comparator.comparing(Metric::name)
                .thenComparing(metric -> metric.type().name())
                .thenComparing(metric -> metric.labels().toString())
                .thenComparingDouble(Metric::value);
    }

    private static Collection<Metric> sort(Collection<Metric> metrics) {
        return metrics.stream().sorted(metricComparator()).collect(Collectors.toList());
    }

    private static void assertNoDuplicates(Collection<Metric> metrics) {
        Set<String> seen = new HashSet<>();
        for (Metric metric : metrics) {
            String key = metric.name() + " " + metric.labels();
            if (!seen.add(key)) {
                throw new FailException(
                        format("Duplicate metric found: name [%s] labels %s", metric.name(), metric.labels()));
            }
        }
    }

    private static Collection<Metric> flatten(Map<String, Collection<Metric>> metrics) {
        return metrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    private static String resourcePath(Class<?> testClass, String mode, String javaDockerImage) {
        return testClass.getName().replace(".", "/")
                + "/mode/"
                + mode
                + "/assertions/"
                + sanitizedDockerImage(javaDockerImage)
                + ".txt";
    }

    private static boolean isRuntimeSpecificValue(String name) {
        String lowerName = name.toLowerCase();
        for (String prefix : RUNTIME_SPECIFIC_VALUE_PREFIXES) {
            if (lowerName.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRuntimeNameExcluded(String name) {
        String lowerName = name.toLowerCase();
        for (String prefix : EXCLUDE_METRIC_NAME_PREFIXES) {
            if (lowerName.startsWith(prefix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Canonicalizes a label value by matching it against known canonical
     * suffixes. If the value ends with a canonical suffix, returns that
     * suffix prefixed with {@code *}. Otherwise returns the original value.
     *
     * @param metricName the metric name to look up in the canonicalization table
     * @param labelKey the label key to look up in the canonicalization table
     * @param labelValue the original label value from the exporter
     * @return the canonicalized label value, or the original value if no match
     */
    static String canonicalizeLabelValue(String metricName, String labelKey, String labelValue) {
        for (Map.Entry<String, Map<String, Set<String>>> familyEntry : LABEL_SUFFIX_CANONICALIZATION.entrySet()) {
            if (!metricName.startsWith(familyEntry.getKey())) {
                continue;
            }
            Set<String> canonicalSuffixes = familyEntry.getValue().get(labelKey);
            if (canonicalSuffixes == null) {
                continue;
            }
            for (String suffix : canonicalSuffixes) {
                if (labelValue.endsWith(suffix)) {
                    return "*" + suffix;
                }
            }
        }
        return labelValue;
    }

    private static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean updateMetricsFile() {
        if (Boolean.getBoolean(METRIC_ASSERTIONS_UPDATE)) {
            return true;
        }
        String envValue = System.getenv(METRIC_ASSERTIONS_UPDATE.toUpperCase().replace('.', '_'));
        return Boolean.parseBoolean(envValue);
    }

    private static Path filePath(String resourcePath) {
        return updateResourceDirectory().resolve(resourcePath).toAbsolutePath().normalize();
    }

    private static String sanitizedDockerImage(String javaDockerImage) {
        return javaDockerImage.replaceAll("[^A-Za-z0-9._-]+", "_");
    }

    /**
     * Asserts that the HTTP response contains metrics with the expected content type,
     * status code 200, and a non-empty body.
     *
     * @param httpResponse the HTTP response to validate
     * @param metricsContentType the expected metrics content type
     * @throws AssertionError if the response status is not 200 or the content type does not match
     */
    public static void assertMetricsContentType(HttpResponse httpResponse, MetricsContentType metricsContentType) {
        assertThat(httpResponse).isNotNull();

        int statusCode = httpResponse.statusCode();
        if (statusCode != 200) {
            HttpResponseBody body = httpResponse.body();
            if (body != null) {
                throw new AssertionError(
                        format("Expected statusCode [%d] but was [%d] body [%s]", 200, statusCode, body.string()));
            } else {
                throw new AssertionError(format("Expected statusCode [%d] but was [%d] no body", 200, statusCode));
            }
        }

        assertThat(httpResponse.headers()).isNotNull();
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE)).hasSize(1);
        assertThat(httpResponse.headers().get(HttpHeader.CONTENT_TYPE).get(0)).isEqualTo(metricsContentType.toString());
        assertThat(httpResponse.body()).isNotNull();
        assertThat(httpResponse.body().bytes()).isNotNull();
        assertThat(httpResponse.body().bytes().length).isGreaterThan(0);
    }

    private static Path updateResourceDirectory() {
        String configuredDirectory = System.getProperty(WRITE_DIRECTORY_PROPERTY);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            return Path.of(configuredDirectory);
        }

        Path defaultPath = Path.of(DEFAULT_RESOURCE_DIRECTORY);
        if (Files.isDirectory(defaultPath)) {
            return defaultPath;
        }

        Path modulePath = Path.of(MODULE_RESOURCE_DIRECTORY);
        if (Files.isDirectory(modulePath)) {
            return modulePath;
        }

        return defaultPath;
    }

    private static class MetricExpectation {

        private final Metric.Type type;
        private final String name;
        private final Map<String, String> labels;
        private final Double value;

        MetricExpectation(Metric.Type type, String name, Map<String, String> labels, Double value) {
            this.type = type;
            this.name = name;
            this.labels = labels;
            this.value = value;
        }

        void verify(String resourcePath, Collection<Metric> metrics) {
            List<Metric> matching = metrics.stream()
                    .filter(metric -> metric.type() == type)
                    .filter(metric -> metric.name().equals(name))
                    .filter(metric -> labelsMatchExpected(labels, metric.labels()))
                    .collect(Collectors.toList());

            if (matching.isEmpty()) {
                throw new FailException(format(
                        "Expected metric from [%s] not found: type [%s] name [%s] labels %s",
                        resourcePath, type, name, labels));
            }

            if (value != null) {
                List<Metric> valueMatching = matching.stream()
                        .filter(metric -> Math.abs(metric.value() - value) <= VALUE_TOLERANCE)
                        .collect(Collectors.toList());
                if (valueMatching.isEmpty()) {
                    throw new FailException(format(
                            "Expected metric from [%s] found with wrong value: type [%s] name [%s] labels %s expected [%s] actual %s",
                            resourcePath,
                            type,
                            name,
                            labels,
                            value,
                            matching.stream().map(Metric::value).collect(Collectors.toList())));
                }
                matching = valueMatching;
            }

            if (matching.size() > 1) {
                throw new FailException(format(
                        "Expected metric from [%s] matched multiple metrics: type [%s] name [%s] labels %s",
                        resourcePath, type, name, labels));
            }
        }

        private static boolean labelsMatchExpected(
                Map<String, String> expectedLabels, Map<String, String> actualLabels) {
            for (Map.Entry<String, String> entry : expectedLabels.entrySet()) {
                String expectedValue = entry.getValue();
                String actualValue = actualLabels.get(entry.getKey());
                if (actualValue == null) {
                    return false;
                }
                if (expectedValue.equals("*")) {
                    // "*" alone means any non-null value — skip check.
                    continue;
                }
                if (expectedValue.startsWith("*")) {
                    String suffix = expectedValue.substring(1);
                    if (!actualValue.endsWith(suffix)) {
                        return false;
                    }
                } else {
                    if (!actualValue.equals(expectedValue)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static class PrefixExpectation {

        private final String prefix;
        private final boolean present;

        PrefixExpectation(String prefix, boolean present) {
            this.prefix = prefix;
            this.present = present;
        }

        void verify(String resourcePath, Collection<Metric> metrics) {
            boolean found = metrics.stream().anyMatch(metric -> metric.name().startsWith(prefix));
            if (present && !found) {
                throw new FailException(
                        format("Expected metric prefix from [%s] not found: prefix [%s]", resourcePath, prefix));
            }
            if (!present && found) {
                throw new FailException(
                        format("Expected metric prefix from [%s] to be absent: prefix [%s]", resourcePath, prefix));
            }
        }
    }
}
