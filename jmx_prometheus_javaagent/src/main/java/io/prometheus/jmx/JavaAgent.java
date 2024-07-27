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

import io.prometheus.jmx.common.http.HTTPServerFactory;
import io.prometheus.jmx.common.opentelemetry.OpenTelemetryExporterFactory;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetAddress;

public class JavaAgent {

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
        try {
            Arguments arguments = Arguments.parse(agentArgument);
            File file = new File(arguments.getFilename());

            new BuildInfoMetrics().register(PrometheusRegistry.defaultRegistry);
            JvmMetrics.builder().register(PrometheusRegistry.defaultRegistry);
            new JmxCollector(new File(arguments.getFilename()), JmxCollector.Mode.AGENT)
                    .register(PrometheusRegistry.defaultRegistry);

            switch (arguments.getMode()) {
                case HTTP:
                    {
                        HTTPServerFactory.getInstance()
                                .createHTTPServer(
                                        InetAddress.getByName(arguments.getHost()),
                                        arguments.getPort(),
                                        PrometheusRegistry.defaultRegistry,
                                        file);

                        break;
                    }
                case OPEN_TELEMETRY:
                    {
                        OpenTelemetryExporterFactory.getInstance()
                                .createOpenTelemetryExporter(
                                        PrometheusRegistry.defaultRegistry, file);

                        break;
                    }
                default:
                    {
                        throw new RuntimeException("Undefined mode");
                    }
            }
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("Failed to start Prometheus JMX Exporter");
                System.err.println();
                t.printStackTrace(System.err);
                System.err.println();
                System.err.println("Prometheus JMX Exporter exiting");
                System.err.flush();
            }

            System.exit(1);
        }
    }
}
