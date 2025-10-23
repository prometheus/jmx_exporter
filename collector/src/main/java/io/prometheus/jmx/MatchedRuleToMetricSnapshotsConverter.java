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

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.UnknownSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Class to implement MatchedRuleToMetricSnapshotsConverter */
public class MatchedRuleToMetricSnapshotsConverter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MatchedRuleToMetricSnapshotsConverter.class);

    private static final String OBJECTNAME = "_objectname";

    /** Constructor */
    public MatchedRuleToMetricSnapshotsConverter() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to convert a List of MatchedRules to MetricSnapshots
     *
     * @param matchedRules matchedRules
     * @return a MetricSnapshots
     */
    public static MetricSnapshots convert(List<MatchedRule> matchedRules) {
        Map<String, List<MatchedRule>> rulesByPrometheusMetricName = new HashMap<>();

        for (MatchedRule matchedRule : matchedRules) {
            List<MatchedRule> matchedRulesWithSameName =
                    rulesByPrometheusMetricName.computeIfAbsent(
                            matchedRule.name, name -> new ArrayList<>());
            matchedRulesWithSameName.add(matchedRule);
        }

        if (LOGGER.isTraceEnabled()) {
            rulesByPrometheusMetricName
                    .values()
                    .forEach(
                            matchedRules1 ->
                                    matchedRules1.forEach(
                                            matchedRule ->
                                                    LOGGER.trace("matchedRule %s", matchedRule)));
        }

        MetricSnapshots.Builder result = MetricSnapshots.builder();
        for (List<MatchedRule> rulesWithSameName : rulesByPrometheusMetricName.values()) {
            result.metricSnapshot(convertRulesWithSameName(rulesWithSameName));
        }
        return result.build();
    }

    private static MetricSnapshot convertRulesWithSameName(List<MatchedRule> rulesWithSameName) {
        boolean labelsUnique = isLabelsUnique(rulesWithSameName);
        switch (getType(rulesWithSameName)) {
            case "COUNTER":
                CounterSnapshot.Builder counterBuilder =
                        CounterSnapshot.builder()
                                .name(rulesWithSameName.get(0).name)
                                .help(rulesWithSameName.get(0).help);
                for (MatchedRule rule : rulesWithSameName) {
                    Labels labels = rule.labels;
                    if (!labelsUnique) {
                        labels =
                                labels.merge(
                                        Labels.of(
                                                OBJECTNAME,
                                                rule.matchName.substring(
                                                        0, rule.matchName.lastIndexOf(":"))));
                    }
                    counterBuilder.dataPoint(
                            CounterSnapshot.CounterDataPointSnapshot.builder()
                                    .labels(labels)
                                    .value(rule.value)
                                    .build());
                }
                return counterBuilder.build();
            case "GAUGE":
                GaugeSnapshot.Builder gaugeBuilder =
                        GaugeSnapshot.builder()
                                .name(rulesWithSameName.get(0).name)
                                .help(rulesWithSameName.get(0).help);
                for (MatchedRule rule : rulesWithSameName) {
                    Labels labels = rule.labels;
                    if (!labelsUnique) {
                        labels =
                                labels.merge(
                                        Labels.of(
                                                OBJECTNAME,
                                                rule.matchName.substring(
                                                        0, rule.matchName.lastIndexOf(":"))));
                    }
                    gaugeBuilder.dataPoint(
                            GaugeSnapshot.GaugeDataPointSnapshot.builder()
                                    .labels(labels)
                                    .value(rule.value)
                                    .build());
                }
                return gaugeBuilder.build();
            default:
                UnknownSnapshot.Builder unknownBuilder =
                        UnknownSnapshot.builder()
                                .name(rulesWithSameName.get(0).name)
                                .help(rulesWithSameName.get(0).help);
                for (MatchedRule rule : rulesWithSameName) {
                    Labels labels = rule.labels;
                    if (!labelsUnique) {
                        labels =
                                labels.merge(
                                        Labels.of(
                                                OBJECTNAME,
                                                rule.matchName.substring(
                                                        0, rule.matchName.lastIndexOf(":"))));
                    }
                    unknownBuilder.dataPoint(
                            UnknownSnapshot.UnknownDataPointSnapshot.builder()
                                    .labels(labels)
                                    .value(rule.value)
                                    .build());
                }
                return unknownBuilder.build();
        }
    }

    /** If all rules have the same type, that type is returned. Otherwise, "UNKNOWN" is returned. */
    private static String getType(List<MatchedRule> rulesWithSameName) {
        if (rulesWithSameName.stream().map(rule -> rule.type).distinct().count() == 1) {
            return rulesWithSameName.get(0).type;
        }
        return "UNKNOWN";
    }

    private static boolean isLabelsUnique(List<MatchedRule> rulesWithSameName) {
        Set<Labels> labelsSet = new HashSet<>(rulesWithSameName.size());
        for (MatchedRule matchedRule : rulesWithSameName) {
            Labels labels = matchedRule.labels;
            if (labelsSet.contains(labels)) {
                return false;
            }
            labelsSet.add(labels);
        }
        return true;
    }
}
