package io.prometheus.jmx;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

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
                sb.append(':');
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
        File f = new File(getClass().getClassLoader().getResource("test-reload.yml").getFile());
        final String config = f.getCanonicalPath();
        final String javaagent = "-javaagent:" + buildDirectory + "/" + finalName + ".jar=" + port + ":" + config + ":5";

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
            System.out.println("Before read");
            // Wait for application to start
            app.getInputStream().read();
            System.out.println("After read");
            Map<String, Boolean> expectedMetric = new HashMap<>();
            expectedMetric.put("jmx_scrape_duration_seconds", true);
            expectedMetric.put("java_lang_MemoryPool_UsageThresholdCount", true);

            assertThat("Expected metric not found", findMetrics("http://localhost:" + port + "/metrics", "java_lang_MemoryPool_UsageThresholdCount",
                    "jmx_scrape_duration_seconds"), CoreMatchers.equalTo(expectedMetric));

            System.out.println("Before rename");

            assertTrue("rename", new File(f.getParentFile(), "test.yml").renameTo(f));

            System.out.println("Before sleep");
            Thread.sleep(TimeUnit.SECONDS.toMillis(15));
            System.out.println("After sleep sleep");

            Map<String, Boolean> expectedWithLowerCase = new HashMap<>();
            expectedWithLowerCase.put("jmx_scrape_duration_seconds", true);
            expectedWithLowerCase.put("java_lang_memorypool_usagethresholdcount", true);

            assertThat("Expected metric not found", findMetrics("http://localhost:" + port + "/metrics", "jmx_scrape_duration_seconds",
                    "java_lang_memorypool_usagethresholdcount"), CoreMatchers.equalTo(expectedWithLowerCase));


        } finally {
            final int exitcode = terminate(app);
            // Log any errors printed
            int len;
            byte[] buffer = new byte[100];
            while ((len = app.getErrorStream().read(buffer)) != -1) {
                System.out.write(buffer, 0, len);
            }
            while ((len = app.getInputStream().read(buffer)) != -1) {
                System.out.write(buffer, 0, len);
            }

            assertThat("Application did not exit cleanly", exitcode == 0);
        }
    }

    int terminate(Process p) {
        // Tell application to stop
        try {
            p.getOutputStream().write('\n');
            p.getOutputStream().flush();
        } catch (IOException ignored) {
        }
        try {
            return p.waitFor();
        } catch (InterruptedException e) {
            return -1;
        }
    }

    private Map<String, Boolean> findMetrics(String url, String... metricNames) throws IOException {
        Map<String, Boolean> result = new HashMap<>(metricNames.length);
        for (String name : metricNames) {
            result.put(name, false);
        }
        int lineNo = 0;
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader contents = new BufferedReader(new InputStreamReader(is));
            for (String line = contents.readLine(); line != null; line = contents.readLine()) {
                if (lineNo % 100 == 0) {
                    System.out.println("Processing line " + lineNo);
                }
                for (String name : metricNames) {
                    if (line.contains(name)) {
                        result.put(name, true);
                    }
                }
                lineNo++;
            }
        }

        System.out.println("Results: " + result);
        return result;
    }
}
