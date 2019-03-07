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

	@Test
	public void shouldParseWhenIPv6() {
		String host = "[2001:0db8:0000:0000:0000:ff00:0042:8329]";
		int port = 1234;
		String settingsFile = "/home/user/data/monitoring.yml";

		AgentParameters params = JavaAgent.parseParameters(host + ":" + port + ":" + settingsFile);

		Assert.assertEquals(host, params.getHost());
		Assert.assertEquals(port, params.getPort());
		Assert.assertEquals(settingsFile, params.getSettingsFilePath());
	}

	@Test
	public void shouldParseWhenDns() {
		String host = "my.service.com";
		int port = 1234;
		String settingsFile = "/home/user/data/monitoring.yml";

		AgentParameters params = JavaAgent.parseParameters(host + ":" + port + ":" + settingsFile);

		Assert.assertEquals(host, params.getHost());
		Assert.assertEquals(port, params.getPort());
		Assert.assertEquals(settingsFile, params.getSettingsFilePath());
	}

	@Test
	public void shouldParseWhenIp4() {
		String host = "127.0.0.1";
		int port = 1234;
		String settingsFile = "/home/user/data/monitoring.yml";

		AgentParameters params = JavaAgent.parseParameters(host + ":" + port + ":" + settingsFile);

		Assert.assertEquals(host, params.getHost());
		Assert.assertEquals(port, params.getPort());
		Assert.assertEquals(settingsFile, params.getSettingsFilePath());
	}

	@Test
	public void shouldParseWhenNoAddress() {
		int port = 1234;
		String settingsFile = "/home/user/data/monitoring.yml";

		AgentParameters params = JavaAgent.parseParameters(port + ":" + settingsFile);

		Assert.assertEquals(JavaAgent.DEFAULT_HOST, params.getHost());
		Assert.assertEquals(port, params.getPort());
		Assert.assertEquals(settingsFile, params.getSettingsFilePath());
	}

	@Test
	public void shouldParseWhenWindowsPath() {
		int port = 1234;
		String settingsFile = "C:\\Program Files\\monitoring.yml";

		AgentParameters params = JavaAgent.parseParameters(port + ":" + settingsFile);

		Assert.assertEquals(JavaAgent.DEFAULT_HOST, params.getHost());
		Assert.assertEquals(port, params.getPort());
		Assert.assertEquals(settingsFile, params.getSettingsFilePath());
	}

	//trying to avoid the occurrence of any : in the windows path
	private String resolveRelativePathToResource(String resource) {
		final String configwk = new File(getClass().getClassLoader().getResource(resource).getFile()).getAbsolutePath();
		final File workingDir = new File(new File(".").getAbsolutePath());
		return "." + configwk.replace(workingDir.getParentFile().getAbsolutePath(), "");
	}
}
