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
import io.prometheus.jmx.common.util.functions.IntegerInRange;
import io.prometheus.jmx.common.util.functions.ToBoolean;
import io.prometheus.jmx.common.util.functions.ToInteger;
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

/**
 * Java agent for the Prometheus JMX exporter.
 *
 * <p>Provides the agent entry points ({@code premain} and {@code agentmain}) for running the JMX
 * exporter as a Java agent attached to a target JVM. The agent collects JMX metrics from the
 * target JVM and exposes them via HTTP (Prometheus text format) or OpenTelemetry protocol.
 *
 * <p>The agent can be started in two ways:
 *
 * <ul>
 *   <li>At JVM startup via the {@code -javaagent} flag (calls {@code premain})
 *   <li>At runtime via attach API (calls {@code agentmain})
 * </ul>
 *
 * <p>Configuration is provided via a YAML file specified in the agent arguments. The agent
 * supports optional startup delay, HTTP server for Prometheus scraping, and OpenTelemetry
 * exporter configuration.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. The agent startup is synchronized to prevent
 * concurrent initialization.
 */
public class JavaAgent {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaAgent.class);

    /**
     * Default Prometheus registry for metric collection.
     *
     * <p>All JMX metrics are registered to this shared registry.
     */
    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    /**
     * Thread name for asynchronous startup when a delay is configured.
     */
    private static final String THREAD_NAME = "jmx-exporter-startup";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private JavaAgent() {
        // INTENTIONALLY BLANK
    }

    /**
     * Java agent entry point for runtime attachment.
     *
     * <p>Called when the agent is attached to a running JVM via the attach API. Delegates to
     * {@link #premain(String, Instrumentation)} for actual initialization.
     *
     * @param agentArgument the agent argument string containing configuration, may be {@code null}
     *     or empty to use defaults
     * @param instrumentation the instrumentation instance provided by the JVM, may be {@code null}
     *     in some environments
     */
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        premain(agentArgument, instrumentation);
    }

    /**
     * Java agent entry point for JVM startup.
     *
     * <p>Called by the JVM when the agent is loaded at startup via the {@code -javaagent} flag.
     * Parses the configuration file, optionally delays startup, and initializes the JMX collector
     * and HTTP server or OpenTelemetry exporter.
     *
     * <p>Startup behavior:
     *
     * <ul>
     *   <li>Force-initializes the platform MBean server to avoid classloader issues
     *   <li>Registers build info and JVM metrics (unless excluded in configuration)
     *   <li>Creates and registers JMX collector in AGENT mode
     *   <li>Starts HTTP server if enabled in arguments
     *   <li>Starts OpenTelemetry exporter if configured
     *   <li>Registers shutdown hooks for clean resource cleanup
     * </ul>
     *
     * @param agentArgument the agent argument string containing host, port, and configuration file,
     *     must not be {@code null} or empty
     * @param instrumentation the instrumentation instance provided by the JVM, may be {@code null}
     *     in some environments
     * @throws ConfigurationException if the agent argument is malformed or configuration is
     *     invalid
     */
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        try {
            Arguments arguments = Arguments.parse(agentArgument);
            File file = new File(arguments.getFilename());
            MapAccessor mapAccessor = MapAccessor.of(YamlSupport.loadYaml(file));

            int startDelaySeconds = mapAccessor
                    .get("/startDelaySeconds")
                    .map(new ToInteger(ConfigurationException.supplier("/startDelaySeconds must be an integer")))
                    .map(new IntegerInRange(
                            0,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("/startDelaySeconds must be non-negative")))
                    .orElse(0);

            if (startDelaySeconds > 0) {
                LOGGER.info("Start delay [%d] seconds", startDelaySeconds);
                startAsync(startDelaySeconds, arguments, file, mapAccessor);
            } else {
                start(arguments, file, mapAccessor);
            }
        } catch (Throwable t) {
            handleError(t, null, null);
        }
    }

    /**
     * Starts the JMX exporter asynchronously after a configured delay.
     *
     * <p>Creates a daemon thread that sleeps for the specified delay before initializing the
     * exporter. This is useful when the target application needs time to initialize its MBeans
     * before the exporter starts collecting metrics.
     *
     * @param startDelaySeconds the delay in seconds before starting, must be non-negative
     * @param arguments the parsed agent arguments, must not be {@code null}
     * @param file the configuration file, must not be {@code null}
     * @param mapAccessor the map accessor for accessing configuration values, must not be
     *     {@code null}
     */
    private static void startAsync(int startDelaySeconds, Arguments arguments, File file, MapAccessor mapAccessor) {
        Thread thread = new Thread(
                () -> {
                    try {
                        Thread.sleep(startDelaySeconds * 1000L);
                        start(arguments, file, mapAccessor);
                    } catch (Throwable t) {
                        handleError(t, null, null);
                    }
                },
                THREAD_NAME);

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Starts the JMX exporter synchronously.
     *
     * <p>Initializes the JMX collector, optionally starts the HTTP server and/or OpenTelemetry
     * exporter, and registers metrics with the default Prometheus registry. Forces initialization
     * of the platform MBean server early to avoid classloader issues.
     *
     * <p>On failure, this method logs the error, closes any started resources, and exits the JVM
     * with status code 1.
     *
     * @param arguments the parsed agent arguments, must not be {@code null}
     * @param file the configuration file, must not be {@code null}
     * @param mapAccessor the map accessor for accessing configuration values, must not be
     *     {@code null}
     */
    static void start(Arguments arguments, File file, MapAccessor mapAccessor) {
        HTTPServer httpServer = null;
        OpenTelemetryExporter openTelemetryExporter = null;

        try {
            LOGGER.info("Starting ...");

            // Force the ManagementFactory to get the platform MBean server
            // now to work around potential classloader issues later when
            // the JmxCollector tries to access it.
            ManagementFactory.getPlatformMBeanServer();

            boolean httpEnabled = arguments.isHttpEnabled();
            boolean openTelemetryEnabled = mapAccessor.containsPath("/openTelemetry");

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);

            boolean excludeJvmMetrics = mapAccessor
                    .get("/excludeJvmMetrics")
                    .map(new ToBoolean(ConfigurationException.supplier("/excludeJvmMetrics must be a boolean")))
                    .orElse(false);

            if (!excludeJvmMetrics) {
                JvmMetrics.builder().register(DEFAULT_REGISTRY);
            }

            new JmxCollector(file, JmxCollector.Mode.AGENT).register(DEFAULT_REGISTRY);

            if (httpEnabled) {
                httpServer = startHttpServer(arguments, file);
            }

            LOGGER.info("HTTP enabled [%b]", httpEnabled);

            LOGGER.info("OpenTelemetry enabled [%b]", openTelemetryEnabled);

            if (openTelemetryEnabled) {
                openTelemetryExporter = startOpenTelemetryExporter(file);
            }

            LOGGER.info("Running ...");
        } catch (Throwable t) {
            handleError(t, openTelemetryExporter, httpServer);
        }
    }

    /**
     * Creates and starts the HTTP server for Prometheus metric scraping.
     *
     * <p>The HTTP server is bound to the host and port specified in the agent arguments. A shutdown
     * hook is registered to ensure the server is closed cleanly on JVM shutdown.
     *
     * @param arguments the parsed agent arguments containing host and port, must not be {@code null}
     * @param file the configuration file, must not be {@code null}
     * @return the started HTTP server instance
     * @throws Exception if the HTTP server fails to start
     */
    private static HTTPServer startHttpServer(Arguments arguments, File file) throws Exception {
        LOGGER.info("HTTP host:port [%s:%d]", arguments.getHost(), arguments.getPort());
        LOGGER.info("Starting HTTPServer ...");
        if (arguments.getPath() != null) LOGGER.info("Metrics path: %s", arguments.getPath());

        HTTPServer httpServer = HTTPServerFactory.createAndStartHTTPServer(
                DEFAULT_REGISTRY,
                InetAddress.getByName(arguments.getHost()),
                arguments.getPort(),
                arguments.getPath(),
                file);

        LOGGER.info("HTTPServer started");

        Runtime.getRuntime().addShutdownHook(new AutoClosableShutdownHook(httpServer));

        return httpServer;
    }

    /**
     * Creates and starts the OpenTelemetry exporter.
     *
     * <p>The exporter configuration is read from the configuration file. A shutdown hook is
     * registered to ensure the exporter is closed cleanly on JVM shutdown.
     *
     * @param file the configuration file containing OpenTelemetry settings, must not be
     *     {@code null}
     * @return the started OpenTelemetry exporter instance
     * @throws Exception if the exporter fails to start
     */
    private static OpenTelemetryExporter startOpenTelemetryExporter(File file) throws Exception {
        LOGGER.info("Starting OpenTelemetry ...");

        OpenTelemetryExporter openTelemetryExporter =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(DEFAULT_REGISTRY, file);

        LOGGER.info("OpenTelemetry started");

        Runtime.getRuntime().addShutdownHook(new AutoClosableShutdownHook(openTelemetryExporter));

        return openTelemetryExporter;
    }

    /**
     * Handles a startup failure by logging the error and cleaning up resources.
     *
     * <p>This method:
     *
     * <ul>
     *   <li>Prints the error stack trace to stderr (synchronized to prevent interleaving)
     *   <li>Closes any started resources (OpenTelemetry exporter, HTTP server)
     *   <li>Exits the JVM with status code 1
     * </ul>
     *
     * <p>This method never returns; it always calls {@code System.exit(1)}.
     *
     * @param t the throwable that caused the failure, may be {@code null}
     * @param openTelemetryExporter the OpenTelemetry exporter to close, may be {@code null}
     * @param httpServer the HTTP server to close, may be {@code null}
     */
    private static void handleError(Throwable t, OpenTelemetryExporter openTelemetryExporter, HTTPServer httpServer) {
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

    /**
     * Closes an AutoCloseable resource, suppressing any exceptions.
     *
     * <p>This method is safe to call with {@code null} and will silently ignore any exceptions
     * thrown during closure. Used for cleanup during error handling where we want to proceed
     * with shutdown regardless of cleanup failures.
     *
     * @param autoCloseable the resource to close, may be {@code null}
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
