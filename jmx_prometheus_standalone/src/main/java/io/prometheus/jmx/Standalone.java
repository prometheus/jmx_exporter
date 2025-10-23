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

package io.prometheus.jmx;

import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.OpenTelemetryExporterFactory;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.ResourceSupport;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.net.InetAddress;

/** Class to implement Standalone */
public class Standalone {

    private static final Logger LOGGER = LoggerFactory.getLogger(Standalone.class);

    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    /** Constructor */
    private Standalone() {
        // INTENTIONALLY BLANK
    }

    /**
     * Main method
     *
     * @param args args
     * @throws Exception Exception
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting ...");

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

            MapAccessor mapAccessor = MapAccessor.of(YamlSupport.loadYaml(file));
            boolean httpEnabled = arguments.isHttpEnabled();
            boolean openTelemetryEnabled = mapAccessor.containsPath("/openTelemetry");

            LOGGER.info("HTTP enabled [%b]", httpEnabled);

            if (httpEnabled) {
                LOGGER.info("HTTP host:port [%s:%d]", arguments.getHost(), arguments.getPort());
                LOGGER.info("Starting HTTPServer ...");

                httpServer =
                        HTTPServerFactory.createAndStartHTTPServer(
                                PrometheusRegistry.defaultRegistry,
                                InetAddress.getByName(arguments.getHost()),
                                arguments.getPort(),
                                file);

                LOGGER.info("HTTPServer started");

                // Add shutdown hook
                Runtime.getRuntime().addShutdownHook(new AutoClosableShutdownHook(httpServer));
            }

            LOGGER.info("OpenTelemetry enabled [%b]", openTelemetryEnabled);

            if (openTelemetryEnabled) {
                LOGGER.info("Starting OpenTelemetry ...");

                openTelemetryExporter =
                        OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                                PrometheusRegistry.defaultRegistry, file);

                LOGGER.info("OpenTelemetry started");

                // Add shutdown hook
                Runtime.getRuntime()
                        .addShutdownHook(new AutoClosableShutdownHook(openTelemetryExporter));
            }

            LOGGER.info("Running ...");

            Thread.currentThread().join();
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("Failed to start Prometheus JMX Exporter ...");
                System.err.println();
                t.printStackTrace(System.err);
                System.err.println();
                System.err.println("Prometheus JMX Exporter exiting");
                System.err.flush();
            }

            close(openTelemetryExporter);
            close(httpServer);

            System.exit(1);
        }
    }

    /**
     * Close the given AutoCloseable resource.
     *
     * @param autoCloseable The AutoCloseable resource to close
     */
    private static void close(AutoCloseable autoCloseable) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (Throwable t) {
                // INTENTIONALLY BLANK
            }
        }
    }
}
