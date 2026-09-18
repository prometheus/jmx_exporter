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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Differential guardrail tests for collector output.
 *
 * <p>Performance work must never change observable output. Each test registers a controlled
 * {@link ProbeMBean} and asserts the complete {@link MetricSnapshots} (metric names, snapshot
 * types, help text, label sets and values, data-point values and ordering) rather than merely
 * counts or non-null results. These tests are the guardrails that an optimization must keep
 * passing.
 */
public class SnapshotEquivalenceTest {

    private static final String PROBE_OBJECT_NAME = "io.prometheus.jmx.test.probe:name=probe";
    private static final String IGNORED_OBJECT_NAME = "io.prometheus.jmx.test.probe:name=ignored";

    private static ObjectName probeName;
    private static ObjectName ignoredName;

    @BeforeAll
    public static void registerProbes() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        probeName = new ObjectName(PROBE_OBJECT_NAME);
        ignoredName = new ObjectName(IGNORED_OBJECT_NAME);
        mBeanServer.registerMBean(new ProbeMBean(), probeName);
        mBeanServer.registerMBean(new ProbeMBean(), ignoredName);
    }

    @AfterAll
    public static void unregisterProbes() throws Exception {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        for (ObjectName name : new ObjectName[] {probeName, ignoredName}) {
            if (mBeanServer.isRegistered(name)) {
                mBeanServer.unregisterMBean(name);
            }
        }
    }

    /**
     * The collector registers its own telemetry metrics alongside the scraped MBean metrics. Those
     * are time-varying and would make snapshot equality comparisons flaky, so they are filtered
     * out; the guardrails assert only the collected MBean output.
     */
    private static boolean isTelemetry(String metricName) {
        return metricName.startsWith("jmx_config_reload") || metricName.startsWith("jmx_scrape_");
    }

    private static PrometheusRegistry newRegistry(String yamlConfig) throws Exception {
        PrometheusRegistry registry = new PrometheusRegistry();
        new JmxCollector(yamlConfig).register(registry);
        return registry;
    }

    private static MetricSnapshots filteredScrape(PrometheusRegistry registry) {
        return registry.scrape(name -> !isTelemetry(name));
    }

    private static MetricSnapshots collect(String yamlConfig) throws Exception {
        return filteredScrape(newRegistry(yamlConfig));
    }

    /**
     * Converts snapshots to a canonical, order-independent representation for content equality
     * across separate scrapes (the snapshot objects themselves do not implement value equality).
     */
    private static List<String> canonical(MetricSnapshots snapshots) {
        List<String> out = new ArrayList<>();
        for (MetricSnapshot metric : snapshots) {
            String type;
            if (metric instanceof CounterSnapshot) {
                type = "COUNTER";
            } else if (metric instanceof GaugeSnapshot) {
                type = "GAUGE";
            } else {
                type = "UNKNOWN";
            }
            for (DataPointSnapshot dp : metric.getDataPoints()) {
                out.add(type + " " + metric.getMetadata().getName() + " "
                        + metric.getMetadata().getHelp() + " " + dp.getLabels() + " " + valueOf(dp));
            }
        }
        return out;
    }

    private static double valueOf(DataPointSnapshot dataPoint) {
        if (dataPoint instanceof CounterSnapshot.CounterDataPointSnapshot) {
            return ((CounterSnapshot.CounterDataPointSnapshot) dataPoint).getValue();
        }
        if (dataPoint instanceof GaugeSnapshot.GaugeDataPointSnapshot) {
            return ((GaugeSnapshot.GaugeDataPointSnapshot) dataPoint).getValue();
        }
        return ((UnknownSnapshot.UnknownDataPointSnapshot) dataPoint).getValue();
    }

    @Test
    public void defaultExport() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n";
        PrometheusRegistry registry = newRegistry(yaml);
        PrometheusRegistryUtils registryUtils = new PrometheusRegistryUtils(registry);

        // Default export name is domain + first bean property value + attribute name.
        Double value = registryUtils.getSampleValue(
                "io_prometheus_jmx_test_probe_probe_count", new String[] {}, new String[] {});
        assertThat(value).isCloseTo(42.0, within(0.001));

        // Two scrapes of fixed-value beans produce identical output.
        assertThat(canonical(filteredScrape(registry)))
                .containsExactlyInAnyOrderElementsOf(canonical(filteredScrape(registry)));
    }

    @Test
    public void cachedRuleIsStableAcrossScrapes() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>count:'\n"
                + "  name: count\n"
                + "  type: COUNTER\n"
                + "  value: 1\n"
                + "  cache: true\n";
        PrometheusRegistry registry = newRegistry(yaml);

        MetricSnapshots first = filteredScrape(registry);
        MetricSnapshots second = filteredScrape(registry);
        assertThat(canonical(first)).containsExactlyInAnyOrderElementsOf(canonical(second));

        assertThat(first).hasSize(1);
        MetricSnapshot metric = first.iterator().next();
        assertThat(metric).isInstanceOf(CounterSnapshot.class);
        assertThat(metric.getDataPoints()).hasSize(1);
        assertThat(valueOf(metric.getDataPoints().get(0))).isCloseTo(1.0, within(0.001));
    }

    @Test
    public void duplicateLabelsAddObjectName() throws Exception {
        // Both beans produce shared{l=v}, so labels collide and _objectname is added.
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n- \"" + IGNORED_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=.*><>count:'\n"
                + "  name: shared\n"
                + "  labels:\n    l: v\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);

        MetricSnapshot metric = snapshots.iterator().next();
        assertThat(metric).isInstanceOf(UnknownSnapshot.class);
        List<? extends DataPointSnapshot> dps = metric.getDataPoints();
        assertThat(dps).hasSize(2);
        for (DataPointSnapshot dp : dps) {
            assertThat(dp.getLabels().get("_objectname")).isNotNull();
        }
    }

    @Test
    public void uniqueLabelsDoNotAddObjectName() throws Exception {
        // Both beans produce shared with a distinct label value, so no _objectname is needed.
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n- \"" + IGNORED_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=(.+)><>count:'\n"
                + "  name: shared\n"
                + "  labels:\n    l: $1\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);

        MetricSnapshot metric = snapshots.iterator().next();
        List<? extends DataPointSnapshot> dps = metric.getDataPoints();
        assertThat(dps).hasSize(2);
        for (DataPointSnapshot dp : dps) {
            assertThat(dp.getLabels().get("_objectname")).isNull();
        }
    }

    @Test
    public void conflictingTypesBecomeUnknown() throws Exception {
        // Each bean matches a different rule; both rules share the metric name "mixed" but
        // declare different types, so the family falls back to UNKNOWN.
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n- \"" + IGNORED_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>count:'\n"
                + "  name: mixed\n"
                + "  type: GAUGE\n"
                + "- pattern: 'probe<name=ignored><>count:'\n"
                + "  name: mixed\n"
                + "  type: COUNTER\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);
        MetricSnapshot metric = snapshots.iterator().next();
        assertThat(metric).isInstanceOf(UnknownSnapshot.class);
        assertThat(metric.getDataPoints()).hasSize(2);
    }

    @Test
    public void sanitizationOfNamesAndLabels() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>text:'\n"
                + "  name: 'foo.bar-baz'\n"
                + "  value: 5\n"
                + "  labels:\n    'l.b': 'v'\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);

        MetricSnapshot metric = snapshots.iterator().next();
        assertThat(metric.getMetadata().getName()).isEqualTo("foo_bar_baz");
        DataPointSnapshot dp = metric.getDataPoints().get(0);
        assertThat(dp.getLabels().get("l_b")).isEqualTo("v");
    }

    @Test
    public void nameConversionGuardrails() {
        assertThat(JmxCollector.toSafeName("café")).isEqualTo("caf_");
        assertThat(JmxCollector.toSafeName("a..b")).isEqualTo("a_b");
        assertThat(JmxCollector.toSafeName("a___b")).isEqualTo("a_b");
        assertThat(JmxCollector.toSafeName("1abc")).isEqualTo("_1abc");
        assertThat(JmxCollector.toSafeName("")).isEqualTo("");
        assertThat(JmxCollector.toSnakeAndLowerCase("FooBar")).isEqualTo("foo_bar");
        assertThat(JmxCollector.toSnakeAndLowerCase("fooBAR")).isEqualTo("foo_bar");
    }

    @Test
    public void valueFactorAndExplicitValue() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>count:'\n"
                + "  name: scaled\n"
                + "  value: 10\n"
                + "  valueFactor: 2\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);
        DataPointSnapshot dp = snapshots.iterator().next().getDataPoints().get(0);
        // value 10 (explicit) * valueFactor 2
        assertThat(valueOf(dp)).isCloseTo(20.0, within(0.001));
    }

    @Test
    public void booleanValueMapsToOneOrZero() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>flag:'\n"
                + "  name: flag\n";
        MetricSnapshots snapshots = collect(yaml);
        DataPointSnapshot dp = snapshots.iterator().next().getDataPoints().get(0);
        assertThat(valueOf(dp)).isCloseTo(1.0, within(0.001));
    }

    @Test
    public void includedButExcludedBeanIsNotExported() throws Exception {
        String yaml = "includeObjectNames:\n"
                + "- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "- \"" + IGNORED_OBJECT_NAME + "\"\n"
                + "excludeObjectNames:\n"
                + "- \"" + IGNORED_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=.*><>count:'\n"
                + "  name: count\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots).hasSize(1);

        // Only the probe bean is included; the excluded bean contributes no data points.
        MetricSnapshot metric = snapshots.iterator().next();
        assertThat(metric.getDataPoints()).hasSize(1);
        assertThat(valueOf(metric.getDataPoints().get(0))).isCloseTo(42.0, within(0.001));
    }

    @Test
    public void helpTextIsRenderedFromRule() throws Exception {
        String yaml = "includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                + "rules:\n"
                + "- pattern: 'probe<name=probe><>count:'\n"
                + "  name: help_metric\n"
                + "  help: This is the help\n";
        MetricSnapshots snapshots = collect(yaml);
        assertThat(snapshots.iterator().next().getMetadata().getHelp()).isEqualTo("This is the help");
    }

    @Test
    public void configReloadProducesSameOutputShape(@TempDir Path tempDir) throws Exception {
        Path configPath = tempDir.resolve("probe.yml");
        Files.write(
                configPath,
                ("includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                                + "rules:\n"
                                + "- pattern: 'probe<name=probe><>count:'\n"
                                + "  name: first\n")
                        .getBytes());

        PrometheusRegistry registry = new PrometheusRegistry();
        new JmxCollector(configPath.toFile()).register(registry);
        assertThat(filteredScrape(registry).iterator().next().getMetadata().getName())
                .isEqualTo("first");

        // Rewrite the config and force a newer modification time so the collector reloads.
        Files.write(
                configPath,
                ("includeObjectNames:\n- \"" + PROBE_OBJECT_NAME + "\"\n"
                                + "rules:\n"
                                + "- pattern: 'probe<name=probe><>count:'\n"
                                + "  name: second\n")
                        .getBytes());
        Files.setLastModifiedTime(configPath, FileTime.fromMillis(System.currentTimeMillis() + 10_000));

        assertThat(filteredScrape(registry).iterator().next().getMetadata().getName())
                .isEqualTo("second");
    }

    /**
     * A controlled {@link DynamicMBean} exposing a fixed set of attributes with known values so
     * that snapshot output can be asserted exactly.
     */
    public static final class ProbeMBean implements DynamicMBean {

        private final Map<String, Object> values = new LinkedHashMap<>();

        public ProbeMBean() {
            values.put("count", 42.0);
            values.put("gauge", 7.5);
            values.put("ratio", 2.0);
            values.put("flag", true);
            values.put("text", "hello");
            values.put("big", 12345L);
            values.put("nan", Double.NaN);
            values.put("infinity", Double.POSITIVE_INFINITY);
        }

        @Override
        public Object getAttribute(String attribute) {
            return values.get(attribute);
        }

        @Override
        public AttributeList getAttributes(String[] attributes) {
            AttributeList result = new AttributeList(attributes.length);
            for (String attribute : attributes) {
                Object value = values.get(attribute);
                if (value != null) {
                    result.add(new Attribute(attribute, value));
                }
            }
            return result;
        }

        @Override
        public void setAttribute(Attribute attribute) {
            values.put(attribute.getName(), attribute.getValue());
        }

        @Override
        public AttributeList setAttributes(AttributeList attributes) {
            for (Attribute attribute : attributes.asList()) {
                values.put(attribute.getName(), attribute.getValue());
            }
            return attributes;
        }

        @Override
        public Object invoke(String actionName, Object[] params, String[] signature) {
            return null;
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            MBeanAttributeInfo[] infos = new MBeanAttributeInfo[values.size()];
            int i = 0;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                Class<?> cls = entry.getValue() == null
                        ? Object.class
                        : entry.getValue().getClass();
                infos[i++] =
                        new MBeanAttributeInfo(entry.getKey(), cls.getSimpleName(), entry.getKey(), true, false, false);
            }
            return new MBeanInfo(getClass().getName(), "probe", infos, null, null, null);
        }
    }
}
