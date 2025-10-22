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

import io.prometheus.jmx.common.ConfigurationException;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.jmx.common.OpenTelemetryExporterFactory;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.jmx.common.util.functions.ToBoolean;
import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;

/** Class to implement JavaAgent */
public class JavaAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaAgent.class);

    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    static {
        // Get the platform MBean server to ensure that
        // it's initialized prior to the application
        ManagementFactory.getPlatformMBeanServer();
    }

    /** Constructor */
    public JavaAgent() {
        // INTENTIONALLY BLANK
    }

    /**
     * Java agent main
     *
     * @param agentArgument the agent argument
     * @param instrumentation the instrumentation
     */
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        premain(agentArgument, instrumentation);
    }

    /**
     * Java agent premain
     *
     * @param agentArgument the agent argument
     * @param instrumentation the instrumentation
     */
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        LOGGER.info("Starting ...");

        HTTPServer httpServer = null;
        OpenTelemetryExporter openTelemetryExporter = null;

        try {
            Arguments arguments = Arguments.parse(agentArgument);
            File file = new File(arguments.getFilename());
            MapAccessor mapAccessor = MapAccessor.of(YamlSupport.loadYaml(file));
            boolean httpEnabled = arguments.isHttpEnabled();
            boolean openTelemetryEnabled = mapAccessor.containsPath("/openTelemetry");

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);

            boolean excludeJvmMetrics =
                    mapAccessor
                            .get("/excludeJvmMetrics")
                            .map(
                                    new ToBoolean(
                                            ConfigurationException.supplier(
                                                    "/excludeJvmMetrics must be a boolean")))
                            .orElse(false);

            if (!excludeJvmMetrics) {
                JvmMetrics.builder().register(DEFAULT_REGISTRY);
            }

            new JmxCollector(file, JmxCollector.Mode.AGENT).register(DEFAULT_REGISTRY);

            LOGGER.info("HTTP enabled [%b]", httpEnabled);

            if (httpEnabled) {
                LOGGER.info("HTTP host:port [%s:%d]", arguments.getHost(), arguments.getPort());
                LOGGER.info("Starting HTTPServer ...");

                // Create and start the HTTP server
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

                // Create and start the OpenTelemetry exporter
                openTelemetryExporter =
                        OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                                PrometheusRegistry.defaultRegistry, file);

                LOGGER.info("OpenTelemetry started");

                // Add shutdown hook
                Runtime.getRuntime()
                        .addShutdownHook(new AutoClosableShutdownHook(openTelemetryExporter));
            }

            LOGGER.info("Running ...");
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
