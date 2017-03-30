package io.prometheus.jmx.configuration;

import org.apache.commons.cli.Options;

class OptionsFactory {

    public static Options create() {
        Options options = new Options();
        options.addOption(Constants.HOST_ARG, Constants.HOST_ARG_LONG,    true,  "Hostname");
        options.addOption(Constants.PORT_ARG, Constants.PORT_ARG_LONG,   true,  "Port");
        options.addOption(Constants.PATH_ARG,  true,  "Path");
        options.addOption(Constants.CONFIG_FILE_ARG, Constants.CONFIG_FILE_LONG,  true,  "Config file path");
        options.addOption(Constants.HELP_ARG, Constants.HELP_ARG_LONG, false, "Show help message");
        options.addOption(Constants.VERBOSE_ARG, false, "Verbose mode");
        return options;
    }

}
