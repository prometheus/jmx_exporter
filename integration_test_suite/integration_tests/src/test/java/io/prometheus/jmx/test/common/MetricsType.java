/*
 * Copyright (C) 2024 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.common;

/** Class to implement ContentType */
public class MetricsType {

    /** Prometheus text format */
    public static final String PROMETHEUS_TEXT_METRICS = "text/plain; version=0.0.4; charset=utf-8";

    /** Prometheus Protobuf format */
    public static final String PROMETHEUS_PROTOBUF_METRICS =
            "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily;"
                    + " encoding=delimited";

    /** OpenMetrics text format */
    public static final String OPEN_METRICS_TEXT_METRICS =
            "application/openmetrics-text; version=1.0.0; charset=utf-8";

    /** Constructor */
    private MetricsType() {
        // INTENTIONALLY BLANK
    }
}
