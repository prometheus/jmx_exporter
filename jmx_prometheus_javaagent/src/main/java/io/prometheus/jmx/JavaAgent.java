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

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.common.http.ConfigurationException;
import io.prometheus.jmx.common.http.HTTPServerFactory;
import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaAgent {

    public static final String CONFIGURATION_REGEX =
            "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?"
                    + // host name, or ipv4, or ipv6 address in brackets
                    "(\\d{1,5}):"
                    + // port
                    "(.+)"; // config file

    static HTTPServer server;

    public static void agentmain(String agentArgument, Instrumentation instrumentation)
            throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation)
            throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        try {
            Config config = parseConfig(agentArgument, host);

            new BuildInfoCollector().register();
            new JmxCollector(new File(config.file), JmxCollector.Mode.AGENT).register();
            DefaultExports.initialize();

            server =
                    new HTTPServerFactory()
                            .createHTTPServer(
                                    config.socket,
                                    CollectorRegistry.defaultRegistry,
                                    true,
                                    new File(config.file));
        } catch (BindException e) {
            System.err.println("Jmx-exporter listen port bind failed : " + e.getMessage());
            System.exit(1);
        }  catch (ConfigurationException e) {
            System.err.println("Configuration Exception : " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println(
                    "Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration"
                            + " file> "
                            + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a
     * javaagent as {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code
     * <CONFIG>} portion.
     *
     * @param args provided agent args
     * @param ifc default bind interface
     * @return configuration to use for our application
     */
    public static Config parseConfig(String args, String ifc) {
        Pattern pattern = Pattern.compile(CONFIGURATION_REGEX);

        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed arguments - " + args);
        }

        String givenHost = matcher.group(1);
        String givenPort = matcher.group(2);
        String givenConfigFile = matcher.group(3);

        int port = Integer.parseInt(givenPort);

        InetSocketAddress socket;
        if (givenHost != null && !givenHost.isEmpty()) {
            socket = new InetSocketAddress(givenHost, port);
        } else {
            socket = new InetSocketAddress(ifc, port);
            givenHost = ifc;
        }

        return new Config(givenHost, port, givenConfigFile, socket);
    }

    static class Config {
        String host;
        int port;
        String file;
        InetSocketAddress socket;

        Config(String host, int port, String file, InetSocketAddress socket) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
        }
    }
}
