package io.prometheus.jmx;

import static java.lang.String.format;

import io.prometheus.jmx.common.http.ConfigurationException;

/** Class to implement Arguments */
public class Arguments {

    private static final String DEFAULT_HOST = "0.0.0.0";

    public enum Mode {
        HTTP,
        OPEN_TELEMETRY
    }

    private final Mode mode;
    private final String host;
    private final Integer port;
    private final String filename;

    /**
     * Constructor
     *
     * @param mode mode
     * @param host host
     * @param port port
     * @param filename filename
     */
    private Arguments(Mode mode, String host, Integer port, String filename) {
        this.mode = mode;
        this.host = host;
        this.port = port;
        this.filename = filename;
    }

    /**
     * Method to get the mode inferred by the Java agent arguments
     *
     * @return
     */
    public Mode getMode() {
        return mode;
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
            throw new ConfigurationException(
                    format("Malformed arguments [%s]", toString(arguments)));
        }

        for (String argument : arguments) {
            if (argument == null || argument.trim().isEmpty()) {
                throw new ConfigurationException(
                        format("Malformed arguments [%s]", toString(arguments)));
            }
        }

        Mode mode;
        String host = null;
        Integer port = null;
        String filename;

        if (arguments.length == 2) {
            mode = Mode.HTTP;
            filename = arguments[1];
        } else {
            mode = Mode.OPEN_TELEMETRY;
            filename = arguments[0];
        }

        switch (mode) {
            case HTTP:
                {
                    host = DEFAULT_HOST;

                    int colonIndex = arguments[0].lastIndexOf(':');

                    try {
                        if (colonIndex < 0) {
                            port = Integer.parseInt(arguments[0]);
                        } else {
                            port = Integer.parseInt(arguments[0].substring(colonIndex + 1));
                            host = arguments[0].substring(0, colonIndex);
                        }
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                format("Malformed arguments [%s]", toString(arguments)));
                    }

                    break;
                }
            case OPEN_TELEMETRY:
                {
                    // INTENTIONALLY BLANK
                    break;
                }
            default:
                {
                    throw new ConfigurationException(
                            format("Malformed arguments [%s]", toString(arguments)));
                }
        }

        return new Arguments(mode, host, port, filename);
    }

    private static String toString(String[] strings) {
        if (strings == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (String string : strings) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(string);
        }

        return stringBuilder.toString();
    }
}
