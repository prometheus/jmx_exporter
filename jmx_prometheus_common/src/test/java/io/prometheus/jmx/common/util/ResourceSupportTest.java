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

package io.prometheus.jmx.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourceSupportTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExistsWithLeadingSlash() {
        assertThat(ResourceSupport.exists("/MapAccessorTest.yaml")).isTrue();
    }

    @Test
    public void testExistsWithoutLeadingSlash() {
        assertThat(ResourceSupport.exists("MapAccessorTest.yaml")).isTrue();
    }

    @Test
    public void testExistsNonExistent() {
        assertThat(ResourceSupport.exists("/nonExistentResource.txt")).isFalse();
    }

    @Test
    public void testExistsNonExistentWithoutLeadingSlash() {
        assertThat(ResourceSupport.exists("nonExistentResource.txt")).isFalse();
    }

    @Test
    public void testLoadValidWithLeadingSlash() throws IOException {
        String content = ResourceSupport.load("/MapAccessorTest.yaml");
        assertThat(content).isNotNull();
        assertThat(content).contains("httpServer");
    }

    @Test
    public void testLoadValidWithoutLeadingSlash() throws IOException {
        String content = ResourceSupport.load("MapAccessorTest.yaml");
        assertThat(content).isNotNull();
        assertThat(content).contains("httpServer");
    }

    @Test
    public void testLoadNull() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> ResourceSupport.load(null));
    }

    @Test
    public void testLoadNonExistent() {
        assertThatExceptionOfType(IOException.class).isThrownBy(() -> ResourceSupport.load("/nonExistentResource.txt"));
    }

    @Test
    public void testExportValid() throws IOException {
        String resource = "/MapAccessorTest.yaml";
        Path exportPath = tempDir.resolve("exported.yaml");
        File exportFile = exportPath.toFile();
        ResourceSupport.export(resource, exportFile);
        assertThat(exportFile).exists();
        String exported = readFile(exportPath);
        assertThat(exported).contains("httpServer");
    }

    @Test
    public void testExportWithoutLeadingSlash() throws IOException {
        String resource = "MapAccessorTest.yaml";
        Path exportPath = tempDir.resolve("exported.yaml");
        File exportFile = exportPath.toFile();
        ResourceSupport.export(resource, exportFile);
        assertThat(exportFile).exists();
    }

    @Test
    public void testExportNullResource() {
        File exportFile = tempDir.resolve("export.yaml").toFile();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ResourceSupport.export(null, exportFile));
    }

    @Test
    public void testExportEmptyResource() {
        File exportFile = tempDir.resolve("export.yaml").toFile();
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ResourceSupport.export("", exportFile));
    }

    @Test
    public void testExportNullFile() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> ResourceSupport.export("/MapAccessorTest.yaml", null));
    }

    @Test
    public void testExportNonExistentResource() {
        File exportFile = tempDir.resolve("export.yaml").toFile();
        assertThatExceptionOfType(IOException.class)
                .isThrownBy(() -> ResourceSupport.export("/nonExistentResource.txt", exportFile));
    }

    private static String readFile(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
