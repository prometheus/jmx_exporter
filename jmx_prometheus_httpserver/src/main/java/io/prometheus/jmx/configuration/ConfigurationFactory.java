package io.prometheus.jmx.configuration;

import org.apache.commons.cli.CommandLine;

class ConfigurationFactory {

    static Configuration create(CommandLine commandLine) {
        if (commandLine == null) throw new IllegalArgumentException("arguments cannot be null");

        Integer port = null;
        if (commandLine.hasOption(Constants.PORT_ARG)) port = Integer.parseInt(commandLine.getOptionValue(Constants.PORT_ARG));

        return Configuration.aBuilder()
                .port(port)
                .hostname(commandLine.getOptionValue(Constants.HOST_ARG))
                .path(commandLine.getOptionValue(Constants.PATH_ARG))
                .configFilePath(commandLine.getOptionValue(Constants.CONFIG_FILE_ARG))
                .verboseMode(commandLine.hasOption(Constants.VERBOSE_ARG))
                .showHelp(commandLine.hasOption(Constants.HELP_ARG))
                .build();

    }

    static Configuration create(Integer port, String hostname, String configFilePath) {

        return Configuration.aBuilder()
                .port(port)
                .hostname(hostname)
                .configFilePath(configFilePath)
                .build();
    }
}
