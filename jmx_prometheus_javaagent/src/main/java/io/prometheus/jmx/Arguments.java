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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable configuration arguments for the Java agent.
 *
 * <p>Parses agent arguments in the following formats:
 *
 * <ul>
 *   <li>{@code port:configFile} - HTTP server on default host (0.0.0.0) with specified port
 *   <li>{@code host:port:configFile} - HTTP server on specified host and port
 *   <li>{@code configFile} - No HTTP server (HTTP disabled)
 * </ul>
 *
 * <p>Host can be a hostname, IPv4 address, or IPv6 address in square brackets.
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
     * Prefix that enables graceful error handling for startup errors.
     */
    private static final String GRACEFUL_PREFIX = "graceful:";

    /**
     * Regular expression pattern for parsing agent arguments.
     *
     * <p>Captures groups:
     *
     * <ol>
     *   <li>Optional host (hostname, IPv4, or IPv6 in brackets)
     *   <li>Port number
     *   <li>Configuration file path
     * </ol>
     */
    private static final String CONFIGURATION_REGEX = "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?"
            + // host name, or ipv4, or ipv6 address in brackets
            "(?:(\\d{1,5}):)"
            // port
            + "(.+)"; // config file

    /**
     * Flag indicating whether the HTTP server is enabled.
     *
     * <p>When {@code true}, the agent exposes metrics via HTTP. When {@code false}, only the
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
     * Flag indicating whether graceful error handling is enabled.
     *
     * <p>When {@code true}, startup errors are logged and resources are cleaned up without calling
     * {@code System.exit(1)}. When {@code false}, the agent fails fast and exits the JVM on startup
     * errors.
     */
    private final boolean gracefulErrorHandling;

    /**
     * Constructs an Arguments instance with the specified configuration.
     *
     * @param httpEnabled whether the HTTP server is enabled
     * @param host the host address for HTTP server binding, may be {@code null} when HTTP is
     *     disabled
     * @param port the port number for HTTP server, may be {@code null} when HTTP is disabled
     * @param filename the path to the configuration file, must not be {@code null}
     * @param gracefulErrorHandling whether graceful error handling is enabled
     * @throws NullPointerException if {@code filename} is {@code null}
     */
    private Arguments(boolean httpEnabled, String host, Integer port, String filename, boolean gracefulErrorHandling) {
        this.httpEnabled = httpEnabled;
        this.host = host;
        this.port = port;
        this.filename = Objects.requireNonNull(filename, "filename cannot be null");
        this.gracefulErrorHandling = gracefulErrorHandling;
    }

    /**
     * Returns whether the HTTP server is enabled.
     *
     * <p>When {@code true}, the agent will expose metrics via HTTP on the configured host and port.
     * When {@code false}, the agent runs without an HTTP server (OpenTelemetry export may still be
     * enabled via configuration).
     *
     * @return {@code true} if HTTP server is enabled, {@code false} otherwise
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    /**
     * Returns the host address for HTTP server binding.
     *
     * <p>When HTTP is enabled and no explicit host was provided in the agent arguments, returns the
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
     * Returns whether graceful error handling is enabled.
     *
     * <p>When {@code true}, startup errors are logged and resources are cleaned up without calling
     * {@code System.exit(1)}.
     *
     * @return {@code true} if graceful error handling is enabled, {@code false} otherwise
     */
    public boolean isGracefulErrorHandling() {
        return gracefulErrorHandling;
    }

    /**
     * Parses the Java agent argument string into an Arguments instance.
     *
     * <p>Supports the following argument formats:
     *
     * <ul>
     *   <li>{@code port:configFile} - Enables HTTP on default host (0.0.0.0) and specified port
     *   <li>{@code host:port:configFile} - Enables HTTP on specified host and port
     *   <li>{@code configFile} - Disables HTTP (only OpenTelemetry export possible via config)
     *   <li>{@code graceful:port:configFile} - Enables HTTP and graceful error handling
     *   <li>{@code graceful:host:port:configFile} - Enables HTTP on specified host/port and graceful error handling
     *   <li>{@code graceful:configFile} - Disables HTTP and enables graceful error handling
     * </ul>
     *
     * <p>Host can be a hostname, IPv4 address, or IPv6 address enclosed in square brackets.
     *
     * @param agentArgument the agent argument string to parse, must not be {@code null} or empty
     * @return the parsed Arguments instance
     * @throws ConfigurationException if the argument is {@code null}, empty, or malformed
     */
    public static Arguments parse(String agentArgument) {
        if (agentArgument == null || agentArgument.trim().isEmpty()) {
            throw new ConfigurationException(format("Malformed arguments [%s]", agentArgument));
        }

        boolean gracefulErrorHandling = parseGracefulPrefix(agentArgument);
        String remainingArgument = stripGracefulPrefix(agentArgument);

        ParsedHttpArguments parsedHttpArguments = parseHttpArguments(remainingArgument);

        return new Arguments(
                parsedHttpArguments.httpEnabled,
                parsedHttpArguments.host,
                parsedHttpArguments.port,
                parsedHttpArguments.filename,
                gracefulErrorHandling);
    }

    /**
     * Determines whether the agent argument starts with the graceful error handling prefix.
     *
     * @param agentArgument the agent argument string to check, must not be {@code null}
     * @return {@code true} if the argument starts with {@code graceful:}, {@code false} otherwise
     */
    private static boolean parseGracefulPrefix(String agentArgument) {
        return agentArgument.startsWith(GRACEFUL_PREFIX);
    }

    /**
     * Strips the optional graceful error handling prefix from the agent argument.
     *
     * @param agentArgument the agent argument string, must not be {@code null}
     * @return the argument string with the graceful prefix removed, if present
     * @throws ConfigurationException if only the graceful prefix was provided
     */
    private static String stripGracefulPrefix(String agentArgument) {
        if (agentArgument.startsWith(GRACEFUL_PREFIX)) {
            String remaining = agentArgument.substring(GRACEFUL_PREFIX.length());
            if (remaining.isEmpty()) {
                throw new ConfigurationException(format("Malformed arguments [%s]", remaining));
            }
            return remaining;
        }
        return agentArgument;
    }

    /**
     * Parses the host, port, and filename from the agent argument after any graceful prefix has been
     * removed.
     *
     * @param agentArgument the agent argument string with graceful prefix already stripped
     * @return a {@link ParsedHttpArguments} containing the parsed values
     * @throws ConfigurationException if the argument is malformed
     */
    private static ParsedHttpArguments parseHttpArguments(String agentArgument) {
        boolean httpEnabled = false;
        String host = null;
        Integer port = null;
        String filename;

        Pattern pattern = Pattern.compile(CONFIGURATION_REGEX);
        Matcher matcher = pattern.matcher(agentArgument);

        if (matcher.matches()) {
            httpEnabled = true;

            String group1 = matcher.group(1);
            host = group1 != null ? group1 : DEFAULT_HOST;

            if (host.startsWith("[") && host.endsWith("]") && host.length() > 3) {
                host = host.substring(1, host.length() - 1);
            }

            port = Integer.parseInt(matcher.group(2));

            if (port < MIN_PORT || port > MAX_PORT) {
                throw new ConfigurationException(
                        format("Port must be between %d and %d [%d]", MIN_PORT, MAX_PORT, port));
            }

            filename = matcher.group(3);

            if (host.trim().isEmpty()) {
                throw new ConfigurationException(format("Malformed arguments for [%s]", agentArgument));
            }
        } else {
            filename = agentArgument;
        }

        return new ParsedHttpArguments(httpEnabled, host, port, filename);
    }

    /**
     * Simple holder for HTTP-related parsed arguments.
     */
    private static class ParsedHttpArguments {

        final boolean httpEnabled;
        final String host;
        final Integer port;
        final String filename;

        ParsedHttpArguments(boolean httpEnabled, String host, Integer port, String filename) {
            this.httpEnabled = httpEnabled;
            this.host = host;
            this.port = port;
            this.filename = filename;
        }
    }
}
