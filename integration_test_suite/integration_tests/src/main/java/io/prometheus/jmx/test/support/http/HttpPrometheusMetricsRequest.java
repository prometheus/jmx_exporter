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

/** Class to implement a Prometheus metrics test (Content-Type for Prometheus metrics) */
public class HttpPrometheusMetricsRequest extends HttpRequest {

    private static final String CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    /** Constructor */
    public HttpPrometheusMetricsRequest() {
        super();
        path("/metrics").header(HttpHeader.CONTENT_TYPE, CONTENT_TYPE);
    }
}
