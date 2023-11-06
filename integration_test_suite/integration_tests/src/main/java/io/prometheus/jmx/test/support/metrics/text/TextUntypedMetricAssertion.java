package io.prometheus.jmx.test.support.metrics.text;

import io.prometheus.jmx.test.support.metrics.text.util.TextMetricLabelsFilter;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opentest4j.AssertionFailedError;

public class TextUntypedMetricAssertion {

    private final Collection<TextMetric> metrics;
    private String name;
    private final TreeMap<String, String> labels;
    private Double value;

    public TextUntypedMetricAssertion(Collection<TextMetric> metrics) {
        this.metrics = metrics;
        this.labels = new TreeMap<>();
    }

    public TextUntypedMetricAssertion name(String name) {
        this.name = name;
        return this;
    }

    public TextUntypedMetricAssertion label(String name, String value) {
        labels.put(name, value);
        return this;
    }

    public TextUntypedMetricAssertion value(Double value) {
        this.value = value;
        return this;
    }

    public void isPresent() {
        List<TextUntypedMetric> metrics =
                this.metrics.stream()
                        .filter(metric -> TextCounterMetric.MetricType.UNTYPED == metric.getType())
                        .filter(metric -> name.equals(metric.getName()))
                        .filter(new TextMetricLabelsFilter(labels))
                        .map(textMetric -> (TextUntypedMetric) textMetric)
                        .filter(metric -> value == null || metric.getValue() == value)
                        .collect(Collectors.toList());

        if (metrics.size() != 1) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] name [%s] labels [%s] value [%f] is not present",
                            TextMetric.MetricType.COUNTER, name, labels, value));
        }
    }

    public void isPresent(boolean isPresent) {
        boolean found = false;

        try {
            isPresent();
            found = true;
        } catch (AssertionFailedError e) {
            // Expected
        }

        if (found != isPresent) {
            throw new AssertionFailedError(
                    String.format(
                            "Metric type [%s] name [%s] labels [%s] value [%f] is present",
                            TextMetric.MetricType.COUNTER, name, labels, value));
        }
    }

    public void isNotPresent() {
        isPresent(false);
    }

    public void isNotPresent(boolean isNotPresent) {
        isPresent(!isNotPresent);
    }
}
