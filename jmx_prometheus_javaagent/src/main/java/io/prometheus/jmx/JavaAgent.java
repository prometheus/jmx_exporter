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

import io.prometheus.jmx.common.http.HTTPServerFactory;
import io.prometheus.jmx.common.opentelemetry.OpenTelemetryExporterFactory;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Class to implement JavaAgent */
public class JavaAgent {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private static final PrometheusRegistry DEFAULT_REGISTRY = PrometheusRegistry.defaultRegistry;

    /** Constructor */
    public JavaAgent() {
        // INTENTIONALLY BLANK
    }

    /**
     * Java agent main
     *
     * @param agentArgument agentArgument
     * @param instrumentation instrumentation
     */
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        premain(agentArgument, instrumentation);
    }

    /**
     * Java agent premain
     *
     * @param agentArgument agentArgument
     * @param instrumentation instrumentation
     */
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        info("Starting ...");

        try {
            Arguments arguments = Arguments.parse(agentArgument);
            File file = new File(arguments.getFilename());
            YamlMapAccessor yamlMapAccessor = new YamlMapAccessor().load(file);
            boolean httpEnabled = arguments.isHttpEnabled();
            boolean openTelemetryEnabled = yamlMapAccessor.containsPath("/openTelemetry");

            new BuildInfoMetrics().register(DEFAULT_REGISTRY);
            JvmMetrics.builder().register(DEFAULT_REGISTRY);
            new JmxCollector(file, JmxCollector.Mode.AGENT).register(DEFAULT_REGISTRY);

            info("HTTP enabled [%b]", httpEnabled);
            if (httpEnabled) {
                info("HTTP host:port [%s:%d]", arguments.getHostname(), arguments.getPort());
            }
            info("OpenTelemetry enabled [%b]", openTelemetryEnabled);

            if (httpEnabled) {
                new HTTPServerFactory()
                        .createHTTPServer(
                                InetAddress.getByName(arguments.getHostname()),
                                arguments.getPort(),
                                PrometheusRegistry.defaultRegistry,
                                file);
            }

            if (openTelemetryEnabled) {
                OpenTelemetryExporterFactory.getInstance()
                        .createOpenTelemetryExporter(PrometheusRegistry.defaultRegistry, file);
            }

            info("Running ...");
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
        }
    }

    private static void info(String format, Object... objects) {
        System.out.printf(
                "%s | %s | INFO | %s | %s%n",
                SIMPLE_DATE_FORMAT.format(new Date()),
                Thread.currentThread().getName(),
                JavaAgent.class.getName(),
                format(format, objects));
    }
}
