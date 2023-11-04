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

package io.prometheus.jmx.test;

import io.prometheus.jmx.test.support.Response;
import java.util.Collection;

/** Class to implements a Protobuf response metrics parser */
public class ProtobufResponseMetricsParser {

    /** Constructor */
    private ProtobufResponseMetricsParser() {
        // DO NOTHING
    }

    /**
     * Method to parse a response as a list of Metric objects
     *
     * <p>A List is used because Metrics could have the same name, but with different labels
     *
     * @param response response
     * @return a Collection of Metrics
     */
    public static Collection<Metric> parse(Response response) {
        throw new RuntimeException("Not yet implemented");
    }
}
