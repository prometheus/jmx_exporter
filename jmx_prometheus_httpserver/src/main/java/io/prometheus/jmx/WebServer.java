/*
 * Copyright (C) 2015-2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx;

import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.http.HTTPServerFactory;
import io.prometheus.jmx.common.opentelemetry.OpenTelemetryExporterFactory;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebServer {

    private static final String DEFAULT_HOST = "0.0.0.0";

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss.SSS", Locale.getDefault());

    private enum Mode {
        HTTP,
        OPEN_TELEMETRY
    }

    public static void main(String[] args) throws Exception {
        HTTPServer httpServer = null;
        OpenTelemetryExporter openTelemetryExporter = null;
        String usage = ResourceSupport.load("/usage.txt");

        if (args == null || args.length < 1 || args.length > 2) {
            System.err.println(usage);
            System.err.println();
            System.exit(1);
        }

        Mode mode;
        if (args.length == 2) {
            mode = Mode.HTTP;
        } else {
            mode = Mode.OPEN_TELEMETRY;
        }

        try {
            new BuildInfoMetrics().register(PrometheusRegistry.defaultRegistry);

            switch (mode) {
                case HTTP:
                    {
                        String host = DEFAULT_HOST;
                        int port;
                        int colonIndex = args[0].lastIndexOf(':');

                        if (colonIndex < 0) {
                            port = Integer.parseInt(args[0]);
                        } else {
                            port = Integer.parseInt(args[0].substring(colonIndex + 1));
                            host = args[0].substring(0, colonIndex);
                        }

                        new JmxCollector(new File(args[1]), JmxCollector.Mode.STANDALONE)
                                .register(PrometheusRegistry.defaultRegistry);

                        httpServer =
                                HTTPServerFactory.getInstance()
                                        .createHTTPServer(
                                                InetAddress.getByName(host),
                                                port,
                                                PrometheusRegistry.defaultRegistry,
                                                new File(args[1]));
                        break;
                    }
                case OPEN_TELEMETRY:
                    {
                        new JmxCollector(new File(args[0]), JmxCollector.Mode.STANDALONE)
                                .register(PrometheusRegistry.defaultRegistry);

                        openTelemetryExporter =
                                OpenTelemetryExporterFactory.getInstance()
                                        .create(new File(args[0]));
                    }
            }

            System.out.println(
                    String.format(
                            "%s | %s | INFO | %s | %s (%s)",
                            SIMPLE_DATE_FORMAT.format(new Date()),
                            Thread.currentThread().getName(),
                            WebServer.class.getName(),
                            "Running",
                            mode == Mode.HTTP ? "HTTP" : "OpenTelemetry"));

            Thread.currentThread().join();
        } catch (ConfigurationException e) {
            System.err.println("Configuration Exception : " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        } catch (Throwable t) {
            System.err.println("Exception starting");
            t.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (openTelemetryExporter != null) {
                openTelemetryExporter.close();
            }
            if (httpServer != null) {
                httpServer.close();
            }
        }
    }
}
