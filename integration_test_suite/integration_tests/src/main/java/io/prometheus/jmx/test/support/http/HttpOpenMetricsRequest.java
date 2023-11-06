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

package io.prometheus.jmx.test.support.http;

/** Class to implement an OpenMetrics metrics test (Content-Type for OpenMetrics) */
public class HttpOpenMetricsRequest extends HttpRequest {

    private static final String CONTENT_TYPE =
            "application/openmetrics-text; version=1.0.0; charset=utf-8";

    /** Constructor */
    public HttpOpenMetricsRequest() {
        super();
        path("/metrics").header(HttpHeader.CONTENT_TYPE, CONTENT_TYPE);
    }
}
