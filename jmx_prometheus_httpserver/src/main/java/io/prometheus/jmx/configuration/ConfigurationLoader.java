package io.prometheus.jmx.configuration;

import io.prometheus.jmx.WebServer;
import org.apache.commons.cli.*;

class ConfigurationLoader implements Loader{

    private final Options options;

    public ConfigurationLoader() {
        options = OptionsFactory.create();
    }

    public Configuration load(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmdLine = parser.parse(options, args);

            Configuration config = ConfigurationFactory.create(cmdLine);

            if (mustShowUsage(config)) {
                showUsage(options);
            }

            verboseMode(config);

            return config;
        }catch (Exception e) {
            return Configuration.anEmptyConfiguration();
        }
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
