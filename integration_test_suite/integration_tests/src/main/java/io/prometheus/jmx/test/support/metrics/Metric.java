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

/** Interface implemented by all metrics */
public interface Metric {

    /**
     * Method to get the Metric type
     *
     * @return the Metric type
     */
    String type();

    /**
     * Method to get the Metric help
     *
     * @return the Metric help
     */
    String help();

    /**
     * Method to get the Metric name
     *
     * @return the Metric name
     */
    String name();

    /**
     * Metric to get the Metric labels
     *
     * @return the Metric labels
     */
    Map<String, String> labels();
}
