package io.prometheus.jmx;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public class JavaAgentTest {

    @Test
    public void testConfig() throws IOException, InterruptedException {
        JavaAgent.ConfigArgs config4Params = new JavaAgent.ConfigArgs(
                "myhost:9876:/servlet/path:myconfig.yaml".split(":"));
        assertThat(config4Params.hostname(), CoreMatchers.equalTo("myhost"));
        assertThat(config4Params.port(), CoreMatchers.equalTo(9876));
        assertThat(config4Params.path(), CoreMatchers.equalTo("/servlet/path"));
        assertThat(config4Params.file(), CoreMatchers.equalTo("myconfig.yaml"));

        JavaAgent.ConfigArgs configNoHost = new JavaAgent.ConfigArgs(
                "9876:/servlet/path:myconfig.yaml".split(":"));
        assertThat(configNoHost.port(), CoreMatchers.equalTo(9876));
        assertThat(configNoHost.path(), CoreMatchers.equalTo("/servlet/path"));
        assertThat(configNoHost.file(), CoreMatchers.equalTo("myconfig.yaml"));

        JavaAgent.ConfigArgs configNoPath = new JavaAgent.ConfigArgs(
                "myhost:9876:myconfig.yaml".split(":"));
        assertThat(configNoPath.hostname(), CoreMatchers.equalTo("myhost"));
        assertThat(configNoPath.port(), CoreMatchers.equalTo(9876));
        assertThat(configNoPath.path(), CoreMatchers.equalTo(JavaAgent.DEFAULT_SERVLET_PATH));
        assertThat(configNoPath.file(), CoreMatchers.equalTo("myconfig.yaml"));

        JavaAgent.ConfigArgs config2Params = new JavaAgent.ConfigArgs(
                "9876:myconfig.yaml".split(":"));
        assertThat(config2Params.port(), CoreMatchers.equalTo(9876));
        assertThat(config2Params.path(), CoreMatchers.equalTo(JavaAgent.DEFAULT_SERVLET_PATH));
        assertThat(config2Params.file(), CoreMatchers.equalTo("myconfig.yaml"));
    }
}
