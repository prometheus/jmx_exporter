package io.prometheus.jmx.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationLoaderTest {

    private static final int PORT = 5556;
    private static final String CONFIG_FILE_PATH = "example_configs/httpserver_sample_config.yml";
    private static final String HOSTNAME = "localhost";
    private static final String PATH = "metrics2";
    private Loader loader;

    @Before
    public void setUp() throws Exception {
        loader = new ConfigurationLoader();
    }

    @Test
    public void loadWithMandatoryParams() throws Exception {
        String[] inputArgs = retrieveBasicInputArgs();
        Configuration loadedConfiguration = loader.load(inputArgs);
        Configuration expectedConfiguration = retrieveExpectedConfiguration();

        Assert.assertEquals(loadedConfiguration, expectedConfiguration);
    }

    @Test
    public void loadWithAllParams() throws Exception {
        String[] inputArgs = retrieveAllInputArgs();
        Configuration loadedConfiguration = loader.load(inputArgs);
        Configuration expectedConfiguration = new Configuration(PORT, HOSTNAME, PATH, CONFIG_FILE_PATH, Boolean.FALSE, Boolean.FALSE);

        Assert.assertEquals(loadedConfiguration, expectedConfiguration);
    }

    @Test
    public void loadNotValidConfigWithWrongParams() throws Exception {
        String[] input = new String[0];
        Configuration configuration = loader.load(input);

        Assert.assertTrue(configuration.isNotValid());
    }

    private String[] retrieveBasicInputArgs() {
        return new String[]{"-port", PORT + "", "-config", CONFIG_FILE_PATH} ;
    }

    private String[] retrieveAllInputArgs() {
        return new String[]{"-port", PORT + "", "-config", CONFIG_FILE_PATH, "-path", PATH, "-hostname" , HOSTNAME} ;
    }

    private Configuration retrieveExpectedConfiguration() {
        Configuration config = new Configuration(PORT, CONFIG_FILE_PATH);
        return config;
    }

}