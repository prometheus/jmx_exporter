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

/**
 * Standalone JMX exporter application entry point.
 *
 * <p>Provides the main entry point and startup logic for running the JMX exporter as a standalone
 * process that connects to a remote JVM via JMX. The exporter collects JMX metrics from the remote
 * JVM and exposes them via HTTP (Prometheus text format) or OpenTelemetry protocol.
 *
 * <p>Configuration is provided via a YAML file specified as a command-line argument. The exporter
 * supports HTTP server for Prometheus scraping and OpenTelemetry exporter configuration.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. The main thread blocks indefinitely after successful
 * startup.
 */
public class Standalone {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Standalone.class);

    /**
     * Default Prometheus registry for metric collection.
     *
     * <p>All JMX metrics are registered to this shared registry.
     */
    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private Standalone() {
        // INTENTIONALLY BLANK
    }

    /**
     * Main entry point for the standalone JMX exporter.
     *
     * <p>Parses command-line arguments and starts the JMX exporter. The process runs indefinitely
     * until terminated (e.g., via SIGTERM or Ctrl+C).
     *
     * <p>Usage:
     *
     * <ul>
     *   <li>{@code java -jar jmx_prometheus_standalone.jar &lt;configFile&gt;}
     *   <li>{@code java -jar jmx_prometheus_standalone.jar &lt;port&gt; &lt;configFile&gt;}
     *   <li>{@code java -jar jmx_prometheus_standalone.jar &lt;host:port&gt; &lt;configFile&gt;}
     * </ul>
     *
     * <p>Exit codes:
     *
     * <ul>
     *   <li>0 - Normal termination (not currently used)
     *   <li>1 - Invalid arguments or startup failure
     * </ul>
     *
     * @param args command-line arguments specifying configuration file and optional HTTP port/host
     * @throws Exception if a fatal error occurs during startup (though the method typically calls
     *     {@code System.exit(1)} instead of throwing)
     */
    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting ...");

        String usage = ResourceSupport.load("/usage.txt");

        if (args == null || args.length < 1 || args.length > 2) {
            System.err.println(usage);
            System.err.println();
            System.exit(1);
        }

        try {
            Arguments arguments = Arguments.parse(args);
            start(arguments);
        } catch (Throwable t) {
            handleError(t);
            System.exit(1);
        }
    }

    /**
     * Starts the JMX exporter with the parsed arguments.
     *
     * <p>Initializes the JMX collector, optionally starts the HTTP server and/or OpenTelemetry
     * exporter, and blocks the main thread indefinitely. The JMX collector operates in STANDALONE
     * mode, connecting to a remote JVM via JMX as configured in the configuration file.
     *
     * <p>This method does not return; it blocks the calling thread indefinitely via
     * {@link Thread#join()}.
     *
     * @param arguments the parsed command-line arguments, must not be {@code null}
     * @throws Exception if initialization or startup fails
     */
    static void start(Arguments arguments) throws Exception {
        File file = new File(arguments.getFilename());

        new BuildInfoMetrics().register(DEFAULT_REGISTRY);
        new JmxCollector(file, JmxCollector.Mode.STANDALONE).register(DEFAULT_REGISTRY);

        MapAccessor mapAccessor = MapAccessor.of(YamlSupport.loadYaml(file));
        boolean httpEnabled = arguments.isHttpEnabled();
        boolean openTelemetryEnabled = mapAccessor.containsPath("/openTelemetry");

        LOGGER.info("HTTP enabled [%b]", httpEnabled);

        if (httpEnabled) {
            startHttpServer(arguments, file);
        }

        LOGGER.info("OpenTelemetry enabled [%b]", openTelemetryEnabled);

        if (openTelemetryEnabled) {
            startOpenTelemetryExporter(file);
        }

        LOGGER.info("Running ...");

        Thread.currentThread().join();
    }

    /**
     * Creates and starts the HTTP server for Prometheus metric scraping.
     *
     * <p>The HTTP server is bound to the host and port specified in the arguments. A shutdown
     * hook is registered to ensure the server is closed cleanly on JVM shutdown.
     *
     * @param arguments the parsed arguments containing host and port, must not be {@code null}
     * @param file the configuration file, must not be {@code null}
     * @throws Exception if the HTTP server fails to start
     */
    private static void startHttpServer(Arguments arguments, File file) throws Exception {
        LOGGER.info("HTTP host:port [%s:%d]", arguments.getHost(), arguments.getPort());
        LOGGER.info("Starting HTTPServer ...");

        HTTPServer httpServer = HTTPServerFactory.createAndStartHTTPServer(
                DEFAULT_REGISTRY, InetAddress.getByName(arguments.getHost()), arguments.getPort(), file);

        LOGGER.info("HTTPServer started");

        Runtime.getRuntime().addShutdownHook(new AutoClosableShutdownHook(httpServer));
    }

    /**
     * Creates and starts the OpenTelemetry exporter.
     *
     * <p>The exporter configuration is read from the configuration file. A shutdown hook is
     * registered to ensure the exporter is closed cleanly on JVM shutdown.
     *
     * @param file the configuration file containing OpenTelemetry settings, must not be
     *     {@code null}
     * @throws Exception if the exporter fails to start
     */
    private static void startOpenTelemetryExporter(File file) throws Exception {
        LOGGER.info("Starting OpenTelemetry ...");

        OpenTelemetryExporter openTelemetryExporter =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(DEFAULT_REGISTRY, file);

        LOGGER.info("OpenTelemetry started");

        Runtime.getRuntime().addShutdownHook(new AutoClosableShutdownHook(openTelemetryExporter));
    }

    /**
     * Handles a startup failure by logging the error.
     *
     * <p>This method prints the error stack trace to stderr in a synchronized block to prevent
     * interleaving when multiple errors occur. Unlike {@code JavaAgent#handleError()}, this method
     * does not exit the JVM; the caller should handle process termination.
     *
     * @param t the throwable that caused the failure, may be {@code null}
     */
    private static void handleError(Throwable t) {
        synchronized (System.err) {
            System.err.println("Failed to start Prometheus JMX Exporter ...");
            System.err.println();
            t.printStackTrace(System.err);
            System.err.println();
            System.err.println("Prometheus JMX Exporter exiting");
            System.err.flush();
        }
    }
}
