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
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WebServer {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd | HH:mm:ss.SSS", Locale.getDefault());

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
            System.exit(1);
        }

        String host = "0.0.0.0";
        int port;
        int colonIndex = args[0].lastIndexOf(':');

        if (colonIndex < 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = Integer.parseInt(args[0].substring(colonIndex + 1));
            host = args[0].substring(0, colonIndex);
        }

        new BuildInfoMetrics().register(PrometheusRegistry.defaultRegistry);
        new JmxCollector(new File(args[1]), JmxCollector.Mode.STANDALONE)
                .register(PrometheusRegistry.defaultRegistry);

        HTTPServer httpServer = null;

        try {
            httpServer =
                    new HTTPServerFactory()
                            .createHTTPServer(
                                    InetAddress.getByName(host),
                                    port,
                                    PrometheusRegistry.defaultRegistry,
                                    new File(args[1]));

            System.out.println(
                    String.format(
                            "%s | %s | INFO | %s | %s",
                            SIMPLE_DATE_FORMAT.format(new Date()),
                            Thread.currentThread().getName(),
                            WebServer.class.getName(),
                            "Running"));

            Thread.currentThread().join();
        } catch (ConfigurationException e) {
            System.err.println("Configuration Exception : " + e.getMessage());
            System.exit(1);
        } catch (Throwable t) {
            System.err.println("Exception starting");
            t.printStackTrace();
        } finally {
            if (httpServer != null) {
                httpServer.close();
            }
        }
    }
}
