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

/** Class to implement Arguments */
public class Arguments {

    private static final String DEFAULT_HOST = "0.0.0.0";

    private final boolean httpEnabled;
    private final String host;
    private final Integer port;
    private final String filename;

    /**
     * Constructor
     *
     * @param httpEnabled httpEnabled
     * @param host host
     * @param port port
     * @param filename filename
     */
    private Arguments(boolean httpEnabled, String host, Integer port, String filename) {
        this.httpEnabled = httpEnabled;
        this.host = host;
        this.port = port;
        this.filename = filename;
    }

    /**
     * Method to return if HTTP is enabled
     *
     * @return true if HTTP is enabled, else false
     */
    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    /**
     * Method to get the host
     *
     * @return the host if it exists, else null
     */
    public String getHost() {
        return host;
    }

    /**
     * Method to get the port
     *
     * @return the port if it exists, else null
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Method to get the filename
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Method to parse the Java Standalone configuration arguments
     *
     * @param arguments the Java arguments
     * @return Arguments
     */
    public static Arguments parse(String[] arguments) {
        if (arguments == null || arguments.length == 0) {
            throw new ConfigurationException("Malformed arguments (none provided)");
        }

        for (String argument : arguments) {
            if (argument == null || argument.trim().isEmpty()) {
                throw new ConfigurationException(
                        format("Malformed arguments [%s]", toString(arguments)));
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
                throw new ConfigurationException(
                        format("Malformed arguments [%s]", toString(arguments)));
            }

            filename = arguments[1];
        } else {
            filename = arguments[0];
        }

        return new Arguments(httpEnabled, hostname, port, filename);
    }

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
