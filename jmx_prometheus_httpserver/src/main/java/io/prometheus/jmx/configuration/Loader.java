package io.prometheus.jmx.configuration;

public interface Loader {

    Configuration load(String[] args);

}
