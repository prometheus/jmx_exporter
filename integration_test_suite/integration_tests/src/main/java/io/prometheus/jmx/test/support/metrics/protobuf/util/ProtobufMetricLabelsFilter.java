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

package io.prometheus.jmx.test.support.metrics.protobuf.util;

import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class ProtobufMetricLabelsFilter
        implements Function<Metrics.MetricFamily, Stream<Metrics.Metric>> {

    private final TreeMap<String, String> labels;

    /**
     * Constructor
     *
     * @param labels labels
     */
    public ProtobufMetricLabelsFilter(TreeMap<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public Stream<Metrics.Metric> apply(Metrics.MetricFamily metrics) {
        Collection<Metrics.Metric> collection = new ArrayList<>();
        for (Metrics.Metric metric : metrics.getMetricList()) {
            Map<String, String> labels = new TreeMap<>();
            List<Metrics.LabelPair> labelPairs = metric.getLabelList();
            for (Metrics.LabelPair labelPair : labelPairs) {
                labels.put(labelPair.getName(), labelPair.getValue());
            }

            if (labels.entrySet().containsAll(this.labels.entrySet())) {
                collection.add(metric);
            }
        }
        return collection.stream();
    }
}
