package io.prometheus.jmx;

import static java.lang.String.format;

import io.prometheus.jmx.common.http.ConfigurationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Class to implement Arguments */
public class Arguments {

    private static final String DEFAULT_HOST = "0.0.0.0";

    public enum Mode {
        HTTP,
        OPEN_TELEMETRY
    }

    private static final String CONFIGURATION_REGEX =
            "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?"
                    + // host name, or ipv4, or ipv6 address in brackets
                    "(?:(\\d{1,5}):)"
                    // port
                    + "(.+)"; // config file

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
     * @return the mode
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

        Mode mode;
        String host = null;
        Integer port = null;
        String filename;

        if (matcher.matches()) {
            switch (matcher.groupCount()) {
                case 2:
                    {
                        mode = Mode.HTTP;
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
                        mode = Mode.HTTP;
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
                throw new ConfigurationException(format("Malformed arguments [%s]", agentArgument));
            }
        } else {
            mode = Mode.OPEN_TELEMETRY;
            filename = agentArgument;
        }

        return new Arguments(mode, host, port, filename);
    }
}
