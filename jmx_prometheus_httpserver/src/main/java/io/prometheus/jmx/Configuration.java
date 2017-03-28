package io.prometheus.jmx;

class Configuration {

    private static final String BASE_CONTEXT_PATH = "/";

    private Integer port;

    private String hostname;

    private String path;

    private String configFilePath;

    private Boolean verbose;

    private Boolean help;

    public Configuration(Integer port, String hostname, String path, String configFilePath, Boolean verbose, Boolean help) {
        this.port = port;
        this.hostname = hostname;
        this.path = path;
        this.configFilePath = configFilePath;
        this.verbose = verbose;
        this.help = help;
    }

    public Integer retrievePort() {
        return port;
    }

    public String retrieveHostname() {
        return hostname;
    }

    public String retrievePath() {
        return BASE_CONTEXT_PATH + path;
    }

    public String retrieveConfigFilePath() {
        return configFilePath;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    public boolean isNotValid() {
        return help || !basicConfigurationisValid();
    }

    private boolean basicConfigurationisValid() {
        return port != null || configFilePath != null;
    }

    public boolean hasHostname() {
        return hostname != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Configuration:");
        sb.append("\n\tport=").append(port);
        sb.append("\n\thostname='").append(hostname).append('\'');
        sb.append("\n\tpath='").append(path).append('\'');
        sb.append("\n\tconfigFilePath='").append(configFilePath).append('\'');
        sb.append("\n\tverbose=").append(verbose);
        sb.append("\n\thelp=").append(help);
        return sb.toString();
    }
}
