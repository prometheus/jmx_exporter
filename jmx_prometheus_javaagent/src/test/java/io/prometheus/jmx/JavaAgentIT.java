package io.prometheus.jmx;

import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

import org.junit.Test;

public class JavaAgentIT {

    public static final String TEST_YML_CONFIG = "test.yml";

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
    public void agentLoadsAbsolutePathToConfig() throws IOException, InterruptedException {
        // If not starting the testcase via Maven, set the buildDirectory and finalName system properties manually.
        // If not starting the testcase via Maven, set the buildDirectory and finalName system properties manually.
        String config = getClass().getClassLoader().getResource(TEST_YML_CONFIG).getFile();
        testAgent(config);
    }

    @Test
    public void agentLoadsRelativePathToConfig() throws IOException, InterruptedException {
        // If not starting the testcase via Maven, set the buildDirectory and finalName system properties manually.
        String config = getClass().getClassLoader().getResource(TEST_YML_CONFIG).getFile();
        File configFile = new File(TEST_YML_CONFIG);
        copyFileUsingFileChannels(new File(config), configFile);
        testAgent(TEST_YML_CONFIG);
        configFile.delete();
    }

    private void testAgent(String config) throws IOException, InterruptedException {
        final String buildDirectory = (String) System.getProperties().get("buildDirectory");
        final String finalName = (String) System.getProperties().get("finalName");
        final int port = Integer.parseInt((String) System.getProperties().get("it.port"));

        final String javaagent = "-javaagent:" + buildDirectory + File.separator + finalName + ".jar=" + port + ":" + config;

        final String javaHome = System.getenv("JAVA_HOME");
        final String java;
        if (javaHome != null && javaHome.equals("")) {
            java = javaHome + File.separator + "bin" + File.separator + "java";
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

    private static void copyFileUsingFileChannels(File source, File dest)
            throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            if (inputChannel != null) {
                inputChannel.close();
            }
            if (outputChannel != null) {
                outputChannel.close();
            }
        }
    }
}
