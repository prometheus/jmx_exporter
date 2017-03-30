package io.prometheus.jmx.configuration;

import java.util.ArrayList;
import java.util.List;

public class InputArgumentsLoader implements Loader {

    private List<Loader> loaders;

    public InputArgumentsLoader() {
        loaders = new ArrayList<Loader>();
        fillLoaders();
    }

    private void fillLoaders() {
        loaders.add(new LegacyLoader());
        loaders.add(new ConfigurationLoader());
    }

    public Configuration load(String[] args) {
        Configuration config = Configuration.anEmptyConfiguration();
        for (Loader loader : loaders) {
            config = loader.load(args);
            if (!config.isNotValid()) break;
        }
        return config;
    }


}
