package io.prometheus.jmx;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Runs the JmxExampleApplication on different Java Docker images with the jmx_exporter agent attached,
 * and checks if a couple of example metrics are exposed.
 * <p/>
 * Run with
 * <pre>mvn verify</pre>
 */
@RunWith(Parameterized.class)
public class JavaVersionsIT {

  private final String baseImage;
  private final OkHttpClient client = new OkHttpClient();

  @Rule
  public JavaContainer javaContainer;

  @Parameterized.Parameters(name="{0}")
  public static String[][] images() {
    return new String[][] {

        // HotSpot
        { "openjdk:8-jre", "jmx_prometheus_javaagent" },
        { "openjdk:8-jre","jmx_prometheus_javaagent_java6" },

        { "openjdk:11-jre", "jmx_prometheus_javaagent_java6" },
        { "openjdk:11-jre", "jmx_prometheus_javaagent" },

        { "ticketfly/java:6",  "jmx_prometheus_javaagent_java6" },

        { "openjdk:7", "jmx_prometheus_javaagent_java6" },
        { "openjdk:7", "jmx_prometheus_javaagent" },

        { "adoptopenjdk/openjdk16:ubi-minimal-jre", "jmx_prometheus_javaagent_java6" },
        { "adoptopenjdk/openjdk16:ubi-minimal-jre", "jmx_prometheus_javaagent" },

        // OpenJ9
        { "ibmjava:8-jre", "jmx_prometheus_javaagent_java6" },
        { "ibmjava:8-jre", "jmx_prometheus_javaagent" },

        { "ibmjava:8-jre", "jmx_prometheus_javaagent_java6" },
        { "ibmjava:8-jre", "jmx_prometheus_javaagent" },

        { "adoptopenjdk/openjdk11-openj9", "jmx_prometheus_javaagent_java6" },
        { "adoptopenjdk/openjdk11-openj9", "jmx_prometheus_javaagent" },
    };
  }

  public JavaVersionsIT(String baseImage, String agent) throws IOException {
    this.baseImage = baseImage;
    Path agentJar = getAgentJar(agent);
    Path exampleApplicationJar = getExampleApplicationJar();
    String dockerfileContent = loadDockerfile(agentJar, exampleApplicationJar);
    javaContainer = new JavaContainer(dockerfileContent, agentJar, getExampleApplicationJar(),
       "jmx_exporter_test_" + baseImage.replaceAll("[:/-]", "_"))
        .withExposedPorts(9000)
        .waitingFor(Wait.forLogMessage(".*registered.*", 1))
        .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
  }

  private static class JavaContainer extends GenericContainer<JavaContainer> {
    JavaContainer(String dockerFileContent, Path agentJar, Path exampleApplicationJar, String imageName) {
      super(new ImageFromDockerfile( imageName)
          .withFileFromPath(exampleApplicationJar.getFileName().toString(), exampleApplicationJar)
          .withFileFromPath(agentJar.getFileName().toString(), agentJar)
          .withFileFromClasspath("config.yml", "/config.yml")
          .withFileFromString("Dockerfile", dockerFileContent));
    }
  }

  private Path getAgentJar(String agent) throws IOException {
    Path path = Paths.get("../../" + agent + "/target/" + agent + "-" + loadProjectVersion() + ".jar");
    Assert.assertTrue(path + ": File not found.", Files.exists(path));
    return path;
  }

  private Path getExampleApplicationJar() throws IOException {
    return Paths.get("../jmx_example_application/target/jmx_example_application.jar");
  }

  @Test
  public void testJvmMetric() throws Exception {
    String metricName = "java_lang_Memory_NonHeapMemoryUsage_committed";
    String metric = scrapeMetrics(10 * 1000).stream()
        .filter(line -> line.startsWith(metricName))
        .findAny()
        .orElseThrow(() -> new AssertionError("Metric " + metricName + " not found."));
    double value = Double.parseDouble(metric.split(" ")[1]);
    Assert.assertTrue(metricName + " should be > 0", value > 0);
  }

  @Test
  public void testTabularMetric() throws Exception {
    String[] expectedMetrics = new String[] {
        "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size{source=\"/dev/sda1\"} 7.516192768E9",
        "io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size{source=\"/dev/sda2\"} 1.5032385536E10",
        "io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_size{source=\"/dev/sda1\"} 2.5769803776E10",
        "io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_size{source=\"/dev/sda2\"} 1.073741824E11"
    };
    List<String> metrics = scrapeMetrics(10 * 1000);
    for (String expectedMetric : expectedMetrics) {
      metrics.stream()
          .filter(line -> line.startsWith(expectedMetric))
          .findAny()
          .orElseThrow(() -> new AssertionError("Metric " + expectedMetric + " not found."));
    }
  }

  private List<String> scrapeMetrics(long timeoutMillis) {
    long start = System.currentTimeMillis();
    Exception exception = null;
    String host = javaContainer.getHost();
    Integer port = javaContainer.getMappedPort(9000);
    String metricsUrl = "http://" + host + ":" + port + "/metrics";
    while (System.currentTimeMillis() - start < timeoutMillis) {
      try {
        Request request = new Request.Builder()
            .header("Accept", "application/openmetrics-text; version=1.0.0; charset=utf-8")
            .url(metricsUrl)
            .build();
        try (Response response = client.newCall(request).execute()) {
          return Arrays.asList(response.body().string().split("\\n"));
        }
      } catch (Exception e) {
        exception = e;
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
      }
    }
    if (exception != null) {
      exception.printStackTrace();
    }
    Assert.fail("Timeout while getting metrics from " + metricsUrl + " (orig port: " + 9000 + ")");
    return null; // will not happen
  }

  private String loadDockerfile(Path agentJar, Path exampleApplicationJar) throws IOException {
    InputStream in = JavaVersionsIT.class.getResourceAsStream("/Dockerfile");
    StringBuilder result = new StringBuilder();
    try (Reader reader = new BufferedReader(new InputStreamReader(in, Charset.forName(UTF_8.name())))) {
      int c;
      while ((c = reader.read()) != -1) {
        result.append((char) c);
      }
    }
    return result.toString()
        .replace("${base.image}", baseImage)
        .replace("${agent.jar}", agentJar.getFileName().toString())
        .replace("${example_application.jar}", exampleApplicationJar.getFileName().toString());
  }

  private static String loadProjectVersion() throws IOException {
    Properties properties = new Properties();
    properties.load(JavaVersionsIT.class.getResourceAsStream("/project_version.properties"));
    return properties.getProperty("project.version");
  }
}
