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

package io.prometheus.jmx.test.support.metrics.text;

import java.util.TreeMap;

public class TextGaugeMetric implements TextMetric {

    private final String help;
    private final String name;
    private final TreeMap<String, String> labels;
    private final double value;

    public TextGaugeMetric(String name, String help, TreeMap<String, String> labels, Double value) {
        this.name = name;
        this.help = help;
        this.labels = labels;
        this.value = value;
    }

    /**
     * Method to get the metric type
     *
     * @return the metric type
     */
    @Override
    public MetricType getType() {
        return MetricType.GAUGE;
    }

    /**
     * Method to get the metric name
     *
     * @return the metric name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Method to get the metric help
     *
     * @return the metric help
     */
    @Override
    public String getHelp() {
        return help;
    }

    /**
     * Method to get the map of labels / values
     *
     * @return the map of labels / values
     */
    @Override
    public TreeMap<String, String> getLabels() {
        return labels;
    }

    /**
     * Method to get the metric value
     *
     * @return the metric value
     */
    public double getValue() {
        return value;
    }
}
