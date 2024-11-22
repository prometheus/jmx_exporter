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

import static java.lang.String.format;

import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.http.HTTPServerFactory;
import io.prometheus.jmx.common.opentelemetry.OpenTelemetryExporterFactory;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Class to implement Standalone */
public class Standalone {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    /**
     * Main method
     *
     * @param args args
     * @throws Exception Exception
     */
    public static void main(String[] args) throws Exception {
        info("Starting ...");

        HTTPServer httpServer = null;
        OpenTelemetryExporter openTelemetryExporter = null;
        String usage = ResourceSupport.load("/usage.txt");

        if (args == null || args.length < 1 || args.length > 2) {
            System.err.println(usage);
            System.err.println();
            System.exit(1);
        }

        try {
            Arguments arguments = Arguments.parse(args);
            File file = new File(arguments.getFilename());

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);
            new JmxCollector(file, JmxCollector.Mode.STANDALONE).register(DEFAULT_REGISTRY);

            YamlMapAccessor yamlMapAccessor = new YamlMapAccessor().load(file);
            boolean httpEnabled = arguments.isHttpEnabled();
            boolean openTelemetryEnabled = yamlMapAccessor.containsPath("/openTelemetry");

            info("HTTP enabled [%b]", httpEnabled);
            if (httpEnabled) {
                info("HTTP host:port [%s:%d]", arguments.getHost(), arguments.getPort());
            }
            info("OpenTelemetry enabled [%b]", openTelemetryEnabled);

            if (httpEnabled) {
                httpServer =
                        new HTTPServerFactory()
                                .createHTTPServer(
                                        InetAddress.getByName(arguments.getHost()),
                                        arguments.getPort(),
                                        PrometheusRegistry.defaultRegistry,
                                        file);
            }

            if (openTelemetryEnabled) {
                openTelemetryExporter =
                        OpenTelemetryExporterFactory.getInstance()
                                .createOpenTelemetryExporter(
                                        PrometheusRegistry.defaultRegistry, file);
            }

            info("Running ...", httpEnabled, openTelemetryEnabled);

            Thread.currentThread().join();
        } catch (ConfigurationException e) {
            synchronized (System.err) {
                System.err.println(
                        "Failed to start Prometheus JMX Exporter (Configuration exception) ...");
                System.err.println();
                e.printStackTrace(System.err);
                System.err.println();
                System.err.println("Prometheus JMX Exporter exiting");
                System.err.flush();
            }

            System.exit(1);
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("Failed to start Prometheus JMX Exporter ...");
                System.err.println();
                t.printStackTrace(System.err);
                System.err.println();
                System.err.println("Prometheus JMX Exporter exiting");
                System.err.flush();
            }

            System.exit(1);
        } finally {
            close(openTelemetryExporter);
            close(httpServer);
        }
    }

    private static void close(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Throwable t) {
                // INTENTIONALLY BLANK
            }
        }
    }

    private static void info(String format, Object... objects) {
        System.out.printf(
                "%s | %s | INFO | %s | %s%n",
                SIMPLE_DATE_FORMAT.format(new Date()),
                Thread.currentThread().getName(),
                Standalone.class.getName(),
                format(format, objects));
    }
}
