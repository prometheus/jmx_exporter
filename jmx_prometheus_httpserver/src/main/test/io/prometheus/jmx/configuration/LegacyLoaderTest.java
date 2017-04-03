package io.prometheus.jmx.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LegacyLoaderTest {

    private static final Integer PORT = 5556;
    private static final String CONFIG_FILE = "example_configs/httpserver_sample_config.yml";
    private static final String HOSTNAME = "localhost";
    private static final String HOSTNAME_WITH_PORT = HOSTNAME + ":" + PORT;
    private Loader loader;

    @Before
    public void setUp() throws Exception {
        loader = new LegacyLoader();
    }

    @Test
    public void loadWithLegacyParams() throws Exception {
        String[] args = retrieveInputArguments(PORT+"", CONFIG_FILE);
        Configuration loadedConfig = loader.load(args);
        Configuration expectedConfiguration = retrieveExpectedConfiguration(PORT, null, CONFIG_FILE);

        Assert.assertEquals(loadedConfig, expectedConfiguration);
    }

    @Test
    public void loadWithFullLegacyParams() throws Exception {
        String[] args = retrieveInputArguments(HOSTNAME_WITH_PORT, CONFIG_FILE);
        Configuration loadedConfig = loader.load(args);
        Configuration expectedConfiguration = retrieveExpectedConfiguration(PORT, HOSTNAME, CONFIG_FILE);

        Assert.assertEquals(loadedConfig, expectedConfiguration);
    }

    @Test
    public void loadNotValidWithWrongParams() throws Exception {
        String[] args = new String[0];
        Configuration loadedConfig = loader.load(args);

        Assert.assertTrue(loadedConfig.isNotValid());
    }

    private Configuration retrieveExpectedConfiguration(Integer port, String hostname, String configFile) {
        Configuration configuration = new Configuration(port, hostname, configFile);
        return configuration;
    }

    private String[] retrieveInputArguments(String ... params) {
        return params;
    }

}