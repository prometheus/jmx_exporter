package io.prometheus.jmx;

import org.junit.Assert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Create a temporary directory with the agent, example application, and config.yml.
 * The directory will be mounted as a volume in the test container.
 * This allows us to easily write a test where we update config.yml at runtime.
 */
public class Volume implements Closeable {

    private final Path tmpDir; // will be created in the target/ directory

    private Volume(Path tmpDir) {
        this.tmpDir = tmpDir;
    }

    public static Volume create(String prefix) throws IOException, URISyntaxException {
        Path targetDir = Paths.get(Volume.class.getResource("/").toURI()).getParent();
        Assert.assertEquals("failed to locate target directory", "target", targetDir.getFileName().toString());
        return new Volume(Files.createTempDirectory(targetDir, prefix));
    }

    public void copyAgentJar(String module) throws IOException {
        Path agentJar = tmpDir
                .getParent() // ./integration_tests/agent_smoke_test/target/
                .getParent() // ./integration_tests/agent_smoke_test/
                .getParent() // ./integration_tests/
                .getParent() // ./
                .resolve(module)
                .resolve("target")
                .resolve(module + "-" + loadProjectVersion() + ".jar");
        Assert.assertTrue(agentJar + ": File not found.", Files.exists(agentJar));
        Files.copy(agentJar, tmpDir.resolve("agent.jar"));
    }

    public void copyHttpServer(String module) throws IOException {
        Path httpServerJar = tmpDir
                .getParent() // ./integration_tests/agent_smoke_test/target/
                .getParent() // ./integration_tests/agent_smoke_test/
                .getParent() // ./integration_tests/
                .getParent() // ./
                .resolve(module)
                .resolve("target")
                .resolve(module + "-" + loadProjectVersion() + ".jar");
        Assert.assertTrue(httpServerJar + ": File not found.", Files.exists(httpServerJar));
        Files.copy(httpServerJar, tmpDir.resolve("jmx_prometheus_httpserver.jar"));
    }

    public void copyConfigYaml(String filename) throws IOException, URISyntaxException {
        Path configYaml = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        Assert.assertTrue(filename + ": File not found.", Files.exists(configYaml));
        Files.copy(configYaml, tmpDir.resolve("config.yaml"));
    }

    public void copyExampleApplication() throws IOException {
        Path exampleApplicationJar = tmpDir
                .getParent() // ./integration_tests/agent_smoke_test/target/
                .getParent() // ./integration_tests/agent_smoke_test/
                .getParent() // ./integration_tests/
                .resolve("jmx_example_application")
                .resolve("target")
                .resolve("jmx_example_application.jar");
        Assert.assertTrue("jmx_example_application.jar: File not found.", Files.exists(exampleApplicationJar));
        Files.copy(exampleApplicationJar, tmpDir.resolve("jmx_example_application.jar"));
    }

    private static String loadProjectVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(Volume.class.getResourceAsStream("/project_version.properties"));
        return properties.getProperty("project.version");
    }

    public String getHostPath() {
        return tmpDir.toString();
    }

    @Override
    public void close() throws IOException {
        if (!deleteRecursively(tmpDir.toFile())) {
            throw new IOException(tmpDir + ": Failed to remove temporary test directory.");
        }
    }

    private boolean deleteRecursively(File file) {
        File[] allContents = file.listFiles();
        if (allContents != null) {
            for (File child : allContents) {
                deleteRecursively(child);
            }
        }
        return file.delete();
    }
}
