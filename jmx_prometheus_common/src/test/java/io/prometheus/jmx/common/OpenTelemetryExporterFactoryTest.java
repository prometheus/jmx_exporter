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

package io.prometheus.jmx.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.prometheus.metrics.exporter.opentelemetry.OpenTelemetryExporter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class OpenTelemetryExporterFactoryTest {

    @TempDir
    Path tempDir;

    private File createTempYamlFile(String content) throws IOException {
        Path yamlPath = tempDir.resolve("config.yaml");
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(yamlPath))) {
            writer.print(content);
        }
        return yamlPath.toFile();
    }

    @Test
    public void testNullExporterYamlFile() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), null));
    }

    @Test
    public void testMissingOpenTelemetrySection() throws IOException {
        File configFile = createTempYamlFile("httpServer:\n  port: 8080\n");
        OpenTelemetryExporter result =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(new PrometheusRegistry(), configFile);
        assertThat(result).isNull();
    }

    @Test
    public void testEmptyOpenTelemetrySection() throws IOException {
        File configFile = createTempYamlFile("openTelemetry:\n");
        OpenTelemetryExporter result =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(new PrometheusRegistry(), configFile);
        assertThat(result).isNotNull();
        result.close();
    }

    @Test
    public void testEndpointOnly() throws IOException {
        File configFile = createTempYamlFile("openTelemetry:\n  endpoint: http://localhost:4317\n");
        OpenTelemetryExporter exporter =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(new PrometheusRegistry(), configFile);
        assertThat(exporter).isNotNull();
        exporter.close();
    }

    @Test
    public void testFullConfig() throws IOException {
        File configFile = new File(getClass()
                .getResource("/OpenTelemetryExporterFactoryTest/full.yaml")
                .getFile());
        OpenTelemetryExporter exporter =
                OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(new PrometheusRegistry(), configFile);
        assertThat(exporter).isNotNull();
        exporter.close();
    }

    @Test
    public void testInvalidInterval() throws IOException {
        File configFile = new File(getClass()
                .getResource("/OpenTelemetryExporterFactoryTest/invalidInterval.yaml")
                .getFile());
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }

    @Test
    public void testInvalidTimeout() throws IOException {
        File configFile = new File(getClass()
                .getResource("/OpenTelemetryExporterFactoryTest/invalidTimeout.yaml")
                .getFile());
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }

    @Test
    public void testInvalidHeaderKey() throws IOException {
        File configFile = new File(getClass()
                .getResource("/OpenTelemetryExporterFactoryTest/invalidHeaderKey.yaml")
                .getFile());
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }

    @Test
    public void testInvalidResourceAttributeKey() throws IOException {
        File configFile = new File(getClass()
                .getResource("/OpenTelemetryExporterFactoryTest/invalidResourceAttributeKey.yaml")
                .getFile());
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }

    @Test
    public void testMalformedEndpoint() throws IOException {
        File configFile = createTempYamlFile("openTelemetry:\n  endpoint: not a valid url\n");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }

    @Test
    public void testIoException() throws IOException {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.yaml");
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), nonExistentFile))
                .withMessageContaining("Exception loading file");
    }

    @Test
    public void testInvalidIntervalNonInteger() throws IOException {
        File configFile =
                createTempYamlFile("openTelemetry:\n  endpoint: http://localhost:4317\n  interval: notANumber\n");
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> OpenTelemetryExporterFactory.createAndStartOpenTelemetryExporter(
                        new PrometheusRegistry(), configFile));
    }
}
