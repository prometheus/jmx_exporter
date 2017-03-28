package io.prometheus.jmx;

import org.apache.commons.cli.Options;

import static io.prometheus.jmx.Constants.*;

class OptionsFactory {

    public static Options create() {
        Options options = new Options();
        options.addOption(HOST_ARG, HOST_ARG_LONG,    true,  "Hostname");
        options.addOption(PORT_ARG, PORT_ARG_LONG,   true,  "Port");
        options.addOption(PATH_ARG,  true,  "Path");
        options.addOption(CONFIG_FILE_ARG, CONFIG_FILE_LONG,  true,  "Config file path");
        options.addOption(HELP_ARG, HELP_ARG_LONG, false, "Show help message");
        options.addOption(VERBOSE_ARG, false, "Verbose mode");
        return options;
    }

}
