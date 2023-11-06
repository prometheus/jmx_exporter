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

public interface TextMetric {

    enum MetricType {
        COUNTER,
        GAUGE,
        UNTYPED
    }

    /**
     * Method to get the metric type
     *
     * @return the metric type
     */
    MetricType getType();

    /**
     * Method to get the metric name
     *
     * @return the metric name
     */
    String getName();

    /**
     * Method to get the metric help
     *
     * @return the metric help
     */
    String getHelp();

    /**
     * Method to get the metric labels
     *
     * @return the metric labels
     */
    TreeMap<String, String> getLabels();
}
