package io.prometheus.jmx;

import org.apache.commons.cli.CommandLine;

import static io.prometheus.jmx.Constants.*;
import static io.prometheus.jmx.Constants.HELP_ARG;
import static io.prometheus.jmx.Constants.VERBOSE_ARG;

class ConfigurationFactory {

    private static final String DEFAULT_PATH = "metrics";

    public static Configuration create(CommandLine commandLine) {
        if (commandLine == null) throw new IllegalArgumentException("arguments cannot be null");
        Integer port = Integer.parseInt(commandLine.getOptionValue(PORT_ARG));
        String hostname = commandLine.getOptionValue(HOST_ARG);
        String path = commandLine.getOptionValue(PATH_ARG, DEFAULT_PATH);
        String configFilePath = commandLine.getOptionValue(CONFIG_FILE_ARG);
        Boolean verbose = commandLine.hasOption(VERBOSE_ARG);
        Boolean help = commandLine.hasOption(HELP_ARG);

        return new Configuration(port, hostname, path, configFilePath, verbose, help);
    }
}
