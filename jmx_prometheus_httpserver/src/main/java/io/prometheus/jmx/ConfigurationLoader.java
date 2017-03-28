package io.prometheus.jmx;

import org.apache.commons.cli.*;

class ConfigurationLoader {

    private final Options options;
    private final String[] inputArgs;

    public ConfigurationLoader(String[] args) throws ParseException {
        if (args == null) throw new IllegalArgumentException("inputArgs cannot be null");
        options = OptionsFactory.create();
        this.inputArgs = args;
    }

    public Configuration loadConfiguration() throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine = parser.parse(options, inputArgs);

        Configuration config = ConfigurationFactory.create(cmdLine);

        if (mustShowUsage(config)) {
            showUsage(options);
            System.exit(-1);
        }

        verboseMode(config);

        return config;
    }

    private void showUsage(Options options) {
        new HelpFormatter().printHelp(WebServer.class.getCanonicalName(), options );
    }

    private static boolean mustShowUsage(Configuration configuration) {
        return configuration.isNotValid();
    }

    private void verboseMode(Configuration config) {
        if (config.isVerbose()) System.out.println(config);
    }
}
