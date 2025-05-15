package io.prometheus.jmx;

import static java.lang.String.format;

import io.prometheus.jmx.common.http.ConfigurationException;

/** Class to implement Arguments */
public class Arguments {

    private static final String DEFAULT_HOST = "0.0.0.0";

    private final boolean httpEnabled;
    private final String hostname;
    private final Integer port;
    private final String filename;

    /**
     * Constructor
     *
     * @param httpEnabled httpEnabled
     * @param hostname hostname
     * @param port port
     * @param filename filename
     */
    private Arguments(boolean httpEnabled, String hostname, Integer port, String filename) {
        this.httpEnabled = httpEnabled;
        this.hostname = hostname;
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
    public String getHostname() {
        return hostname;
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
