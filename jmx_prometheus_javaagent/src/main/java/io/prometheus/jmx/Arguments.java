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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Class to implement Arguments */
public class Arguments {

    private static final String DEFAULT_HOST = "0.0.0.0";

    private static final String CONFIGURATION_REGEX =
            "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?"
                    + // host name, or ipv4, or ipv6 address in brackets
                    "(?:(\\d{1,5}):)"
                    // port
                    + "(.+)"; // config file

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
     * Method to parse the Java Agent configuration arguments
     *
     * @param agentArgument the Java agent argument
     * @return Arguments
     */
    public static Arguments parse(String agentArgument) {
        if (agentArgument == null || agentArgument.trim().isEmpty()) {
            throw new ConfigurationException(format("Malformed arguments [%s]", agentArgument));
        }

        Pattern pattern = Pattern.compile(CONFIGURATION_REGEX);
        Matcher matcher = pattern.matcher(agentArgument);

        boolean httpEnabled = false;
        String host = null;
        Integer port = null;
        String filename;

        if (matcher.matches()) {
            switch (matcher.groupCount()) {
                case 2:
                    {
                        httpEnabled = true;
                        host = DEFAULT_HOST;

                        try {
                            port = Integer.parseInt(matcher.group(1));
                        } catch (NumberFormatException e) {
                            throw new ConfigurationException(
                                    format("Malformed arguments [%s]", agentArgument));
                        }
                        filename = matcher.group(2);
                        break;
                    }
                case 3:
                    {
                        httpEnabled = true;
                        host = matcher.group(1) != null ? matcher.group(1) : DEFAULT_HOST;

                        if (host.startsWith("[") && host.endsWith("]") && host.length() > 3) {
                            host = host.substring(1, host.length() - 1);
                        }

                        port = Integer.parseInt(matcher.group(2));
                        filename = matcher.group(3);
                        break;
                    }
                default:
                    {
                        throw new ConfigurationException(
                                format("Malformed arguments [%s]", agentArgument));
                    }
            }

            if (host.trim().isEmpty()) {
                throw new ConfigurationException(
                        format("Malformed arguments for [%s]", agentArgument));
            }
        } else {
            filename = agentArgument;
        }

        return new Arguments(httpEnabled, host, port, filename);
    }
}
