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

package io.prometheus.jmx.test.support.metrics;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/** Class to filter to test if a Metric contains a subset of labels */
public class LabelsSubsetFilter implements Predicate<Metric> {

    private final TreeMap<String, String> labels;

    /**
     * Constructor
     *
     * @param labels labels
     */
    public LabelsSubsetFilter(TreeMap<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public boolean test(Metric metric) {
        Map<String, String> labels = metric.labels();
        return labels.entrySet().containsAll(this.labels.entrySet());
    }
}
