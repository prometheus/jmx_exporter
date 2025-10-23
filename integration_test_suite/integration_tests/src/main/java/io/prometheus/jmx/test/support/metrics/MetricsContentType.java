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

package io.prometheus.jmx.test.support.metrics;

import java.util.Objects;

/** Class to implement MetricsContentType */
public class MetricsContentType {

    /** Default text format */
    public static final MetricsContentType DEFAULT =
            new MetricsContentType("text/plain; version=0.0.4; charset=utf-8");

    /** Prometheus text format */
    public static final MetricsContentType PROMETHEUS_TEXT_METRICS =
            new MetricsContentType("text/plain; version=0.0.4; charset=utf-8");

    /** Prometheus Protobuf format */
    public static final MetricsContentType PROMETHEUS_PROTOBUF_METRICS =
            new MetricsContentType(
                    "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily;"
                            + " encoding=delimited");

    /** OpenMetrics text format */
    public static final MetricsContentType OPEN_METRICS_TEXT_METRICS =
            new MetricsContentType("application/openmetrics-text; version=1.0.0; charset=utf-8");

    private final String string;

    /** Constructor */
    private MetricsContentType(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return string;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        MetricsContentType that = (MetricsContentType) object;
        return Objects.equals(string, that.string);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(string);
    }
}
