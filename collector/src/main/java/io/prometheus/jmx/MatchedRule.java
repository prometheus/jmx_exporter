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

import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.PrometheusNaming;
import java.util.List;
import java.util.Objects;

/**
 * MatchedRule is the result of matching a JMX bean against the rules present in the configuration
 * file. As rules are matched using regular expressions, caching helps prevent having to match the
 * same beans to the same list of regular expressions.
 */
public class MatchedRule {

    final String name;
    final String matchName;
    final String type;
    final String help;
    final Labels labels;
    final Double value;
    final double valueFactor;

    private static final MatchedRule _unmatched = new MatchedRule();

    /** Constructor */
    private MatchedRule() {
        this.name = null;
        this.matchName = null;
        this.type = null;
        this.help = null;
        this.labels = null;
        this.value = null;
        this.valueFactor = 1.0;
    }

    /**
     * Constructor
     *
     * @param name name
     * @param matchName matchName
     * @param type type
     * @param help help
     * @param labelNames labelNames
     * @param labelValues labelValues
     * @param value value
     * @param valueFactor valueFactor
     */
    public MatchedRule(
            final String name,
            final String matchName,
            final String type,
            final String help,
            final List<String> labelNames,
            final List<String> labelValues,
            final Double value,
            double valueFactor) {
        this.name = PrometheusNaming.sanitizeMetricName(name);
        this.matchName = matchName;
        this.type = type;
        this.help = help;
        this.labels = Labels.of(labelNames, labelValues);
        this.value = value;
        this.valueFactor = valueFactor;
    }

    /**
     * Constructor
     *
     * @param name name - has to be already sanitized (we ensure this by keeping the constructor
     *     private)
     * @param matchName matchName
     * @param type type
     * @param help help
     * @param labels labels
     * @param value value
     * @param valueFactor valueFactor
     */
    private MatchedRule(
            final String name,
            final String matchName,
            final String type,
            final String help,
            final Labels labels,
            final Double value,
            double valueFactor) {
        this.name = name;
        this.matchName = matchName;
        this.type = type;
        this.help = help;
        this.labels = labels;
        this.value = value;
        this.valueFactor = valueFactor;
    }

    /**
     * Method to create a MatchedRule with a value
     *
     * @param value value
     * @return a MatchedRule with a value
     */
    public MatchedRule withValue(double value) {
        return new MatchedRule(
                this.name,
                this.matchName,
                this.type,
                this.help,
                this.labels,
                value,
                this.valueFactor);
    }

    /**
     * An unmatched MatchedRule, used when no rule matching a JMX bean has been found in the
     * configuration. Cached unmatched rules are still a cache hit, that will not produce any
     * metric/value.
     *
     * @return the invalid rule
     */
    public static MatchedRule unmatched() {
        return _unmatched;
    }

    /**
     * Method to return if the rule is unmatched
     *
     * @return true if unmatched, else false
     */
    public boolean isUnmatched() {
        return this == _unmatched;
    }

    /**
     * Method to return if the rule is matched
     *
     * @return true if matched, else false
     */
    public boolean isMatched() {
        return !isUnmatched();
    }

    @Override
    public String toString() {
        return "MatchedRule{"
                + "name='"
                + name
                + '\''
                + ", matchName='"
                + matchName
                + '\''
                + ", type='"
                + type
                + '\''
                + ", help='"
                + help
                + '\''
                + ", labels="
                + labels
                + ", value="
                + value
                + ", valueFactor="
                + valueFactor
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatchedRule that = (MatchedRule) o;
        return Double.compare(valueFactor, that.valueFactor) == 0
                && Objects.equals(name, that.name)
                && Objects.equals(matchName, that.matchName)
                && Objects.equals(type, that.type)
                && Objects.equals(help, that.help)
                && Objects.equals(labels, that.labels)
                && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, matchName, type, help, labels, value, valueFactor);
    }
}
