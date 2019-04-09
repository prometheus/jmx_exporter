package io.prometheus.jmx;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class JavaAgentIT {
    private List<URL> getClassloaderUrls() {
        return getClassloaderUrls(getClass().getClassLoader());
    }

    private static List<URL> getClassloaderUrls(ClassLoader classLoader) {
        if (classLoader == null) {
            return Collections.emptyList();
        }
        if (!(classLoader instanceof URLClassLoader)) {
            return getClassloaderUrls(classLoader.getParent());
        }
        URLClassLoader u = (URLClassLoader) classLoader;
        List<URL> result = new ArrayList<URL>(Arrays.asList(u.getURLs()));
        result.addAll(getClassloaderUrls(u.getParent()));
        return result;
    }

    private String buildClasspath() {
        StringBuilder sb = new StringBuilder();
        for (URL url : getClassloaderUrls()) {
            if (!url.getProtocol().equals("file")) {
                continue;
            }
            if (sb.length() != 0) {
                sb.append(java.io.File.pathSeparatorChar);
            }
            sb.append(url.getPath());
        }
        return sb.toString();
    }

    @Test
    public void agentLoads() throws IOException, InterruptedException {
        // If not starting the testcase via Maven, set the buildDirectory and finalName system properties manually.
        final String buildDirectory = (String) System.getProperties().get("buildDirectory");
        final String finalName = (String) System.getProperties().get("finalName");
        final int port = Integer.parseInt((String) System.getProperties().get("it.port"));
        final String config = resolveRelativePathToResource("test.yml");
        final String javaagent = "-javaagent:" + buildDirectory + "/" + finalName + ".jar=" + port + ":" + config;

        final String javaHome = System.getenv("JAVA_HOME");
        final String java;
        if (javaHome != null && javaHome.equals("")) {
            java = javaHome + "/bin/java";
        } else {
            java = "java";
        }

        final Process app = new ProcessBuilder()
            .command(java, javaagent, "-cp", buildClasspath(), "io.prometheus.jmx.TestApplication")
            .start();
        try {
            // Wait for application to start
            app.getInputStream().read();

            InputStream stream = new URL("http://localhost:" + port + "/metrics").openStream();
            BufferedReader contents = new BufferedReader(new InputStreamReader(stream));
            boolean found = false;
            while (!found) {
                String line = contents.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains("jmx_scrape_duration_seconds")) {
                    found = true;
                }
            }

            assertThat("Expected metric not found", found);

            // Tell application to stop
            app.getOutputStream().write('\n');
            try {
                app.getOutputStream().flush();
            } catch (IOException ignored) {
            }
        } finally {
            final int exitcode = app.waitFor();
            // Log any errors printed
            int len;
            byte[] buffer = new byte[100];
            while ((len = app.getErrorStream().read(buffer)) != -1) {
                System.out.write(buffer, 0, len);
            }

            assertThat("Application did not exit cleanly", exitcode == 0);
        }
    }

    //trying to avoid the occurrence of any : in the windows path
    private String resolveRelativePathToResource(String resource) {
        final String configwk = new File(getClass().getClassLoader().getResource(resource).getFile()).getAbsolutePath();
        final File workingDir = new File(new File(".").getAbsolutePath());
        return "." + configwk.replace(workingDir.getParentFile().getAbsolutePath(), "");
    }

    /**
     * Test that the agent string argument is parsed properly. We expect the agent argument in one of these forms...
     * <pre>
     * {@code <port>:<yaml configuration file>}
     * </pre>
     * <pre>
     * {@code <host>:<port>:<yaml configuration file>}
     * </pre>
     * Since the ':' character is part of the spec for this arg, Windows-style paths could cause an issue with parsing.
     * See https://github.com/prometheus/jmx_exporter/issues/312.
     */
    @Test
    public void testAgentStringParsing() {
        final String DEFAULT_HOST = "0.0.0.0";

        JavaAgent.Config config = JavaAgent.parseConfig("8080:config.yaml", DEFAULT_HOST);
        Assert.assertEquals(DEFAULT_HOST, config.host);
        Assert.assertEquals("config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        config = JavaAgent.parseConfig("8080:/unix/path/config.yaml", DEFAULT_HOST);
        Assert.assertEquals(DEFAULT_HOST, config.host);
        Assert.assertEquals("/unix/path/config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        config = JavaAgent.parseConfig("google.com:8080:/unix/path/config.yaml", DEFAULT_HOST);
        Assert.assertEquals("google.com", config.host);
        Assert.assertEquals("/unix/path/config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        config = JavaAgent.parseConfig("127.0.0.1:8080:/unix/path/config.yaml", DEFAULT_HOST);
        Assert.assertEquals("127.0.0.1", config.host);
        Assert.assertEquals("/unix/path/config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        config = JavaAgent.parseConfig("8080:\\Windows\\Local\\Drive\\Path\\config.yaml", DEFAULT_HOST);
        Assert.assertEquals(DEFAULT_HOST, config.host);
        Assert.assertEquals("\\Windows\\Local\\Drive\\Path\\config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        // the following check was previously failing to parse the file correctly
        config = JavaAgent.parseConfig("8080:C:\\Windows\\Path\\config.yaml", DEFAULT_HOST);
        Assert.assertEquals(DEFAULT_HOST, config.host);
        Assert.assertEquals("C:\\Windows\\Path\\config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        // the following check was previously failing to parse the file correctly
        config = JavaAgent.parseConfig("google.com:8080:C:\\Windows\\Path\\config.yaml", DEFAULT_HOST);
        Assert.assertEquals("google.com", config.host);
        Assert.assertEquals("C:\\Windows\\Path\\config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        // the following check was previously failing to parse the file correctly
        config = JavaAgent.parseConfig("127.0.0.1:8080:C:\\Windows\\Path\\config.yaml", DEFAULT_HOST);
        Assert.assertEquals("127.0.0.1", config.host);
        Assert.assertEquals("C:\\Windows\\Path\\config.yaml", config.file);
        Assert.assertEquals(8080, config.port);
    }

    /**
     * If someone is specifying an ipv6 address and a host name, this should be rejected.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidInput() {
        JavaAgent.parseConfig("[2001:0db8:0000:0042:0000:8a2e:0370:7334]:localhost:8080:config.yaml", "0.0.0.0");
    }

    /**
     * Similarly to the test above, two host names
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidInput2() {
        JavaAgent.parseConfig("127.0.0.1:localhost:8080:config.yaml", "0.0.0.0");
    }

    /**
     * Exercise the existing Ipv6 parsing logic. The current logic leaves the brackets on the host.
     */
    @Test
    public void testIpv6AddressParsing() {
        final String DEFAULT_HOST = "0.0.0.0";

        JavaAgent.Config config = JavaAgent.parseConfig("[1:2:3:4]:8080:config.yaml", DEFAULT_HOST);
        Assert.assertEquals("[1:2:3:4]", config.host);
        Assert.assertEquals("config.yaml", config.file);
        Assert.assertEquals(8080, config.port);

        config = JavaAgent.parseConfig("[2001:0db8:0000:0042:0000:8a2e:0370:7334]:8080:C:\\Windows\\Path\\config.yaml", DEFAULT_HOST);
        Assert.assertEquals("[2001:0db8:0000:0042:0000:8a2e:0370:7334]", config.host);
        Assert.assertEquals("C:\\Windows\\Path\\config.yaml", config.file);
        Assert.assertEquals(8080, config.port);
    }
}
