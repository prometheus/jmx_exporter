/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JmxCollectorConstructorTest {

    private PrometheusRegistry prometheusRegistry;
    private PrometheusRegistryUtils prometheusRegistryUtils;

    @BeforeAll
    public static void classSetUp() throws Exception {
        LogManager.getLogManager()
                .readConfiguration(JmxCollectorConstructorTest.class.getResourceAsStream("/logging.properties"));

        TestMBeanRegistry.registerTestMBeans();
    }

    @BeforeEach
    public void setUp() {
        prometheusRegistry = new PrometheusRegistry();
        prometheusRegistryUtils = new PrometheusRegistryUtils(prometheusRegistry);
    }

    @Nested
    class FileConstructorTests {

        @Test
        public void testFileConstructorWithDefaultYaml(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---");
            }

            new JmxCollector(configFile).register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testFileConstructorWithRules(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---\nrules:\n- pattern: \".*\"\n  name: foo\n  value: 1\n  cache: true");
            }

            new JmxCollector(configFile).register(prometheusRegistry);
        }
    }

    @Nested
    class ModeValidationTests {

        @Test
        public void testAgentModeWithJmxUrlThrowsException(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---\njmxUrl: service:jmx:rmi:///jndi/rmi://localhost:1234/jmxrmi");
            }

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector(configFile, JmxCollector.Mode.AGENT));
        }

        @Test
        public void testStandaloneModeWithoutJmxUrlThrowsException(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---");
            }

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector(configFile, JmxCollector.Mode.STANDALONE));
        }

        @Test
        public void testAgentModeWithoutJmxUrlIsValid(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---");
            }

            new JmxCollector(configFile, JmxCollector.Mode.AGENT).register(prometheusRegistry);
        }

        @Test
        public void testStandaloneModeWithJmxUrlIsValid(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---\njmxUrl: service:jmx:rmi:///jndi/rmi://localhost:1234/jmxrmi");
            }

            new JmxCollector(configFile, JmxCollector.Mode.STANDALONE);
        }

        @Test
        public void testAgentModeWithHostPortThrowsException(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---\nhostPort: localhost:1234");
            }

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> new JmxCollector(configFile, JmxCollector.Mode.AGENT));
        }
    }

    @Nested
    class InputStreamConstructorTests {

        @Test
        public void testInputStreamConstructor() throws Exception {
            ByteArrayInputStream inputStream = new ByteArrayInputStream("---".getBytes(StandardCharsets.UTF_8));

            new JmxCollector(inputStream).register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testInputStreamConstructorWithRules() throws Exception {
            String yaml = "---\nrules:\n- pattern: \".*\"\n  name: foo\n  value: 1\n  cache: true";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

            new JmxCollector(inputStream).register(prometheusRegistry);
        }
    }

    @Nested
    class ConfigReloadTests {

        @Test
        public void testConfigReloadOnFileChange(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---");
            }

            new JmxCollector(configFile).register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---\nlowercaseOutputName: true");
            }

            configFile.setLastModified(System.currentTimeMillis() + 10000);

            assertThat(getSampleValue("java_lang_operatingsystem_processcputime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testConfigReloadWithInvalidYaml(@TempDir File tempDir) throws Exception {
            File configFile = new File(tempDir, "config.yaml");
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("---");
            }

            new JmxCollector(configFile).register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();

            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write("invalid: yaml: content: [");
            }

            configFile.setLastModified(System.currentTimeMillis() + 10000);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    @Nested
    class StringConstructorTests {

        @Test
        public void testStringConstructorWithEmptyConfig() throws Exception {
            new JmxCollector("---").register(prometheusRegistry);

            assertThat(getSampleValue("java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }

        @Test
        public void testStringConstructorWithHostPort() throws Exception {
            new JmxCollector("---\nhostPort: localhost:9999");
        }
    }

    @Nested
    class ModeEnumTests {

        @Test
        public void testModeValues() {
            JmxCollector.Mode[] modes = JmxCollector.Mode.values();
            assertThat(modes).hasSize(2);
            assertThat(modes).containsExactly(JmxCollector.Mode.AGENT, JmxCollector.Mode.STANDALONE);
        }

        @Test
        public void testModeValueOf() {
            assertThat(JmxCollector.Mode.valueOf("AGENT")).isEqualTo(JmxCollector.Mode.AGENT);
            assertThat(JmxCollector.Mode.valueOf("STANDALONE")).isEqualTo(JmxCollector.Mode.STANDALONE);
        }
    }

    @Nested
    class SslPropertiesTests {

        @Test
        public void testSslPropertiesDefaultConstructor() {
            JmxCollector.SslProperties props = new JmxCollector.SslProperties();
            assertThat(props.enabled).isFalse();
            assertThat(props.getKeyStoreProperties()).isEmpty();
            assertThat(props.getTrustStoreProperties()).isEmpty();
            assertThat(props.protocols).isEmpty();
            assertThat(props.ciphers).isEmpty();
        }

        @Test
        public void testSslPropertiesEnabledConstructor() {
            JmxCollector.SslProperties props = new JmxCollector.SslProperties(true);
            assertThat(props.enabled).isTrue();
        }

        @Test
        public void testSslPropertiesDisabledConstructor() {
            JmxCollector.SslProperties props = new JmxCollector.SslProperties(false);
            assertThat(props.enabled).isFalse();
        }
    }

    @Nested
    class RegisterTests {

        @Test
        public void testRegisterNoArg() throws Exception {
            new JmxCollector("---").register();

            PrometheusRegistry defaultRegistry = PrometheusRegistry.defaultRegistry;
            PrometheusRegistryUtils utils = new PrometheusRegistryUtils(defaultRegistry);
            assertThat(utils.getSampleValue(
                            "java_lang_OperatingSystem_ProcessCpuTime", new String[] {}, new String[] {}))
                    .isNotNull();
        }
    }

    private Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return prometheusRegistryUtils.getSampleValue(name, labelNames, labelValues);
    }
}
