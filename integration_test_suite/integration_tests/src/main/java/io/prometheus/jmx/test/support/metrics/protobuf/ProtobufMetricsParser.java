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

package io.prometheus.jmx.test.support.metrics.protobuf;

import io.prometheus.jmx.test.support.http.HttpResponse;
import io.prometheus.metrics.expositionformats.generated.com_google_protobuf_3_21_7.Metrics;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ProtobufMetricsParser {

    /** Constructor */
    private ProtobufMetricsParser() {
        // DO NOTHING
    }

    /**
     * Method to parse a Collection of Metrics.MetricFamily (Protobuf) metrics
     *
     * @param httpResponse httpResponse
     * @return a Collection of Metrics.MetricFamily
     * @throws ProtobufMetricsParserException ProtobufMetricsParserException
     */
    public static Collection<Metrics.MetricFamily> parse(HttpResponse httpResponse)
            throws ProtobufMetricsParserException {
        List<Metrics.MetricFamily> metricFamilies = new ArrayList<>();

        try (InputStream inputStream = new ByteArrayInputStream(httpResponse.body())) {
            while (true) {
                Metrics.MetricFamily metricFamily =
                        Metrics.MetricFamily.parseDelimitedFrom(inputStream);
                if (metricFamily == null) {
                    break;
                }
                metricFamilies.add(metricFamily);
            }

            return metricFamilies;
        } catch (Throwable t) {
            throw new ProtobufMetricsParserException("Exception parsing Protobuf metrics", t);
        }
    }
}
