package io.prometheus.jmx.configuration;

public class Configuration {

    private static final String BASE_CONTEXT_PATH = "/";
    private static final String DEFAULT_PATH = "metrics";

    private Integer port;

    private String hostname;

    private String path = DEFAULT_PATH;

    private String configFilePath;

    private Boolean verbose;

    private Boolean help;

    private Configuration() {
        help = Boolean.FALSE;
        verbose = Boolean.FALSE;
    }

    Configuration(Integer port, String configFilePath) {
        this.port = port;
        this.path = DEFAULT_PATH;
        this.configFilePath = configFilePath;
        this.verbose = Boolean.FALSE;
        this.help = Boolean.FALSE;
    }

    Configuration(Integer port, String hostname, String configFilePath) {
        this(port, configFilePath);
        this.hostname = hostname;
    }

    Configuration(Integer port, String hostname, String path, String configFilePath, Boolean verbose, Boolean help) {
        this(port, hostname, configFilePath);
        setPath(path);
        this.verbose = verbose;
        this.help = help;
    }

    public Integer obtainPort() {
        return port;
    }

    public String obtainHostname() {
        return hostname;
    }

    public String obtainPath() {
        return BASE_CONTEXT_PATH + path;
    }

    public String obtainConfigFilePath() {
        return configFilePath;
    }

    public Boolean isVerbose() {
        return verbose;
    }

    private Configuration setPort(Integer port) {
        this.port = port;
        return this;
    }

    private Configuration setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    private Configuration setPath(String path) {
        if (path == null) return this;
        this.path = path;
        return this;
    }

    private Configuration setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
        return this;
    }

    private Configuration setVerbose(Boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    private Configuration setHelp(Boolean help) {
        this.help = help;
        return this;
    }

    public boolean isNotValid() {
        return help || !basicConfigurationisValid();
    }

    private boolean basicConfigurationisValid() {
        return port != null && configFilePath != null;
    }

    public boolean hasHostname() {
        return hostname != null;
    }

    static Builder aBuilder() {
        return new Builder();
    }

    static class Builder {

        private Configuration configuration;

        private Builder() {
            configuration = new Configuration();
        }

        Builder port(Integer port) {
            configuration.setPort(port);
            return this;
        }

        Builder hostname(String hostname) {
            configuration.setHostname(hostname);
            return this;
        }

        Builder path(String path) {
            configuration.setPath(path);
            return this;
        }

        Builder configFilePath(String configFilePath) {
            configuration.setConfigFilePath(configFilePath);
            return this;
        }

        Builder verboseMode(Boolean verbose) {
            Boolean flag = verbose;
            if (verbose == null) flag = Boolean.FALSE;
            configuration.setVerbose(flag);
            return this;
        }

        Builder showHelp(Boolean help) {
            Boolean flag = help;
            if (help == null) flag = Boolean.FALSE;
            configuration.setHelp(help);
            return this;
        }

        Configuration build() {
            return configuration;
        }

    }

    static Configuration anEmptyConfiguration() {
        return new Configuration();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Configuration)) return false;

        Configuration that = (Configuration) o;

        if (port != null ? !port.equals(that.port) : that.port != null) return false;
        if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) return false;
        if (path != null ? !path.equals(that.path) : that.path != null) return false;
        if (configFilePath != null ? !configFilePath.equals(that.configFilePath) : that.configFilePath != null)
            return false;
        if (verbose != null ? !verbose.equals(that.verbose) : that.verbose != null) return false;
        return help != null ? help.equals(that.help) : that.help == null;
    }

    @Override
    public int hashCode() {
        int result = port != null ? port.hashCode() : 0;
        result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (configFilePath != null ? configFilePath.hashCode() : 0);
        result = 31 * result + (verbose != null ? verbose.hashCode() : 0);
        result = 31 * result + (help != null ? help.hashCode() : 0);
        return result;
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
