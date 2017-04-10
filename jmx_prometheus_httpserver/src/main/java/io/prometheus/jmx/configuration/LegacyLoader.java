package io.prometheus.jmx.configuration;

class LegacyLoader implements Loader {

    public Configuration load(String[] args) {
        if (args == null) throw new IllegalArgumentException("args must be provided");
        return fillFromLegacy(args);
    }

    private Configuration fillFromLegacy(String[] args) {
        try {
            String[] hostnamePort = args[0].split(":");

            int port;
            String hostname = null;
            String configFile = null;

            if (hostnamePort.length == 2) {
                port = Integer.parseInt(hostnamePort[1]);
                hostname = hostnamePort[0];
            } else {
                port = Integer.parseInt(hostnamePort[0]);
            }

            configFile = args[1];

            return ConfigurationFactory.create(port, hostname, configFile);

        }catch (Exception e) {
            return Configuration.anEmptyConfiguration();
        }
    }
}
