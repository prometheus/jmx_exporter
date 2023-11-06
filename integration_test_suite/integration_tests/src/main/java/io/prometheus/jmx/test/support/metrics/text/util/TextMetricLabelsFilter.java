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

package io.prometheus.jmx.test.support.metrics.text.util;

import io.prometheus.jmx.test.support.metrics.text.TextMetric;
import java.util.TreeMap;
import java.util.function.Predicate;

public class TextMetricLabelsFilter implements Predicate<TextMetric> {

    private final TreeMap<String, String> labels;

    public TextMetricLabelsFilter(TreeMap<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public boolean test(TextMetric metric) {
        TreeMap<String, String> labels = metric.getLabels();
        return labels.entrySet().containsAll(this.labels.entrySet());
    }
}
