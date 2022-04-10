package io.prometheus.jmx;

import io.prometheus.client.Counter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

public class DynamicConfigReloader implements Supplier<Config> {

    private static final Counter configReloadSuccess = Counter.build()
        .name("jmx_config_reload_success_total")
        .help("Number of times configuration have successfully been reloaded.").register();

    private static final Counter configReloadFailure = Counter.build()
        .name("jmx_config_reload_failure_total")
        .help("Number of times configuration have failed to be reloaded.").register();

    private static final Logger LOGGER = Logger.getLogger(DynamicConfigReloader.class.getName());

    private File configFile;
    private Config config;
    private long configFileLastModified;
    private final Consumer<Config> reloadCallback;

    public DynamicConfigReloader(File configFile, Consumer<Config> reloadCallback) throws FileNotFoundException, Config.ConfigException {
        this.reloadCallback = reloadCallback;
        this.configFile = configFile;
        this.configFileLastModified = configFile.lastModified();
        this.config = Config.load((Map<?, ?>) new Yaml().load(new FileReader(configFile)));
    }

    @Override
    public synchronized Config get() {
        if (configFile != null) {
            long mtime = configFile.lastModified();
            if (mtime > configFileLastModified) {
                LOGGER.fine("Configuration file changed, reloading...");
                reloadConfig();
            }
        }
        return config;
    }

    private void reloadConfig() {
        try {
            FileReader fr = new FileReader(configFile);
            try {
                config = Config.load((Map<?, ?>) new Yaml().load(fr));
                configFileLastModified = configFile.lastModified();
                configReloadSuccess.inc();
                reloadCallback.accept(config);
            } catch (Exception e) {
                LOGGER.severe("Configuration reload failed: " + e.getMessage());
                configReloadFailure.inc();
            } finally {
                fr.close();
            }

        } catch (IOException e) {
            LOGGER.severe("Configuration reload failed: " + e.getMessage());
            configReloadFailure.inc();
        }
    }
}