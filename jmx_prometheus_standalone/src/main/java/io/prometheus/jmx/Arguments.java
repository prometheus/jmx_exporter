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

import static java.lang.String.format;

import io.prometheus.jmx.common.ConfigurationException;
import java.util.Objects;

/**
 * Immutable configuration arguments for the standalone JMX exporter.
 *
 * <p>Parses command-line arguments in the following formats:
 *
 * <ul>
 *   <li>{@code port configFile} - HTTP server on default host (0.0.0.0) with specified port
 *   <li>{@code host:port configFile} - HTTP server on specified host and port
 *   <li>{@code configFile} - No HTTP server (HTTP disabled)
 * </ul>
 *
 * <p>Host can be a hostname or IPv4 address. IPv6 addresses should be specified without brackets.
 *
 * <p>This class is immutable and thread-safe.
 */
public class Arguments {

    /**
     * Minimum valid port number.
     */
    private static final int MIN_PORT = 1;

    /**
     * Maximum valid port number.
     */
    private static final int MAX_PORT = 65535;

    /**
     * Default host address for HTTP server binding when only port is specified.
     */
    private static final String DEFAULT_HOST = "0.0.0.0";

    /**
     * Flag indicating whether the HTTP server is enabled.
     *
     * <p>When {@code true}, the exporter exposes metrics via HTTP. When {@code false}, only the
     * OpenTelemetry exporter may be enabled (if configured).
     */
    private final boolean httpEnabled;

    /**
     * Host address for HTTP server binding.
     *
     * <p>May be {@code null} when HTTP is disabled. When HTTP is enabled without an explicit host,
     * defaults to {@value #DEFAULT_HOST}.
     */
    private final String host;

    /**
     * Port number for HTTP server.
     *
     * <p>May be {@code null} when HTTP is disabled.
     */
    private final Integer port;

    /**
     * Path to the configuration file.
     *
     * <p>Never {@code null}.
     */
    private final String filename;

    /**
     * Constructs an Arguments instance with the specified configuration.
     *
     * @param httpEnabled whether the HTTP server is enabled
     * @param host the host address for HTTP server binding, may be {@code null} when HTTP is
     *     disabled
     * @param port the port number for HTTP server, may be {@code null} when HTTP is disabled
     * @param filename the path to the configuration file, must not be {@code null}
     * @throws NullPointerException if {@code filename} is {@code null}
     */
    private Arguments(boolean httpEnabled, String host, Integer port, String filename) {
        this.httpEnabled = httpEnabled;
        this.host = host;
        this.port = port;
        this.filename = Objects.requireNonNull(filename, "filename cannot be null");
    }

    /**
     * Returns whether the HTTP server is enabled.
     *
     * <p>When {@code true}, the exporter will expose metrics via HTTP on the configured host and
     * port. When {@code false}, the exporter runs without an HTTP server (OpenTelemetry export may
     * still be enabled via configuration).
     *
     * @return {@code true} if HTTP server is enabled, {@code false} otherwise
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    /**
     * Returns the host address for HTTP server binding.
     *
     * <p>When HTTP is enabled and no explicit host was provided in the arguments, returns the
     * default host {@value #DEFAULT_HOST}.
     *
     * <p>When HTTP is disabled, returns {@code null}.
     *
     * @return the host address, or {@code null} if HTTP is disabled
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port number for HTTP server.
     *
     * <p>When HTTP is disabled, returns {@code null}.
     *
     * @return the port number, or {@code null} if HTTP is disabled
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Returns the path to the configuration file.
     *
     * <p>The configuration file contains JMX exporter settings including rules for metric naming,
     * filtering, and export options.
     *
     * @return the configuration file path, never {@code null}
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Parses command-line arguments into an Arguments instance.
     *
     * <p>Supports the following argument formats:
     *
     * <ul>
     *   <li>{@code port configFile} - Two arguments: enables HTTP on default host (0.0.0.0) and
     *       specified port
     *   <li>{@code host:port configFile} - Two arguments: enables HTTP on specified host and port
     *   <li>{@code configFile} - Single argument: disables HTTP (only OpenTelemetry export possible
     *       via config)
     * </ul>
     *
     * <p>Host can be a hostname or IPv4 address.
     *
     * @param arguments the command-line arguments to parse, must not be {@code null} or empty
     * @return the parsed Arguments instance
     * @throws ConfigurationException if arguments are {@code null}, empty, contain {@code null} or
     *     empty strings, or have invalid format
     */
    public static Arguments parse(String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            throw new ConfigurationException("Malformed arguments (none provided)");
        }

        for (String argument : arguments) {
            if (argument == null || argument.trim().isEmpty()) {
                throw new ConfigurationException(format("Malformed arguments [%s]", toString(arguments)));
            }
        }

        boolean httpEnabled = false;
        String hostname = null;
        Integer port = null;
        String filename;

        if (arguments.length == 2) {
            httpEnabled = true;
            hostname = DEFAULT_HOST;

            int colonIndex = arguments[0].lastIndexOf(':');

            try {
                if (colonIndex < 0) {
                    port = Integer.parseInt(arguments[0]);
                } else {
                    port = Integer.parseInt(arguments[0].substring(colonIndex + 1));
                    hostname = arguments[0].substring(0, colonIndex);
                }
            } catch (NumberFormatException e) {
                throw new ConfigurationException(format("Malformed arguments [%s]", toString(arguments)));
            }

            if (port < MIN_PORT || port > MAX_PORT) {
                throw new ConfigurationException(
                        format("Port must be between %d and %d [%d]", MIN_PORT, MAX_PORT, port));
            }

            filename = arguments[1];
        } else {
            filename = arguments[0];
        }

        return new Arguments(httpEnabled, hostname, port, filename);
    }

    /**
     * Converts the arguments array to a string representation for error messages.
     *
     * <p>Handles {@code null} and empty strings specially, representing them as
     * {@code "(null)"} and {@code ""} respectively.
     *
     * @param arguments the arguments array to convert, may be {@code null}
     * @return a space-separated string representation of the arguments
     */
    private static String toString(String[] arguments) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String argument : arguments) {
            stringBuilder.append(" ");

            if (argument == null) {
                stringBuilder.append("(null)");
            } else if (argument.trim().isEmpty()) {
                stringBuilder.append("\"").append(argument.trim()).append("\"");
            }
        }

        return stringBuilder.toString().trim();
    }
}
