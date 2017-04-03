package io.prometheus.jmx.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InputArgumentsLoaderTest {

    private static final Integer PORT = 5556;
    private static final String CONFIG_FILE = "example_configs/httpserver_sample_config.yml";
    private static final String HOSTNAME = "localhost";
    private static final String PATH = "metrics2";

    private static final String VALID_LEGACY_INPUT = HOSTNAME + ":" + PORT + " " + CONFIG_FILE;
    private static final String NOT_VALID_LEGACY_INPUT = "localhost:5556";
    private static final String VALID_INPUT = "-port " + PORT + " -config " + CONFIG_FILE + " -path " + PATH + " -hostname " + HOSTNAME + " -v";
    private static final String NOT_VALID_INPUT = "-port 5556";
    public static final String WHITESPACE = " ";

    private Loader loader;

    @Before
    public void setUp() throws Exception {
        loader = new InputArgumentsLoader();
    }

    @Test
    public void loadWithLegacyArguments() throws Exception {
        String[] args = VALID_LEGACY_INPUT.split(WHITESPACE);
        Configuration configuration = loader.load(args);
        Configuration expected = new Configuration(PORT, HOSTNAME, CONFIG_FILE);

        Assert.assertFalse(configuration.isNotValid());
        Assert.assertEquals(configuration, expected);
    }

    @Test
    public void loadNotValidConfigWithLegacyArguments() throws Exception {
        String[] args = NOT_VALID_LEGACY_INPUT.split(WHITESPACE);
        Configuration configuration = loader.load(args);

        Assert.assertTrue(configuration.isNotValid());
    }

    @Test
    public void loadWithNewArguments() throws Exception {
        String[] args = VALID_INPUT.split(WHITESPACE);
        Configuration configuration = loader.load(args);
        Configuration expected = new Configuration(PORT, HOSTNAME, PATH, CONFIG_FILE, Boolean.TRUE, Boolean.FALSE);

        Assert.assertFalse(configuration.isNotValid());
        Assert.assertEquals(configuration, expected);
    }

    @Test
    public void loadNotValidConfigWithNewArguments() throws Exception {
        String[] args = NOT_VALID_INPUT.split(WHITESPACE);
        Configuration configuration = loader.load(args);

        Assert.assertTrue(configuration.isNotValid());
    }
}