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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class YamlSupportTest {

    @TempDir
    Path tempDir;

    @Test
    public void testLoadYamlStringValid() {
        String yaml = "key: value\nnested:\n  key1: value1\n  key2: value2";
        Map<Object, Object> result = YamlSupport.loadYaml(yaml);
        assertThat(result).isNotNull();
        assertThat(result.get("key")).isEqualTo("value");
        @SuppressWarnings("unchecked")
        Map<Object, Object> nested = (Map<Object, Object>) result.get("nested");
        assertThat(nested.get("key1")).isEqualTo("value1");
        assertThat(nested.get("key2")).isEqualTo("value2");
    }

    @Test
    public void testLoadYamlStringNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> YamlSupport.loadYaml((String) null));
    }

    @Test
    public void testLoadYamlStringEmpty() {
        Map<Object, Object> result = YamlSupport.loadYaml("");
        assertThat(result).isNull();
    }

    @Test
    public void testLoadYamlStringListThrowsClassCastException() {
        String yaml = "- item1\n- item2\n- item3";
        assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> YamlSupport.loadYaml(yaml));
    }

    @Test
    public void testLoadYamlFileValid() throws IOException {
        Path yamlPath = tempDir.resolve("test.yaml");
        Files.write(yamlPath, "key: value\nlist:\n  - a\n  - b".getBytes());
        Map<Object, Object> result = YamlSupport.loadYaml(yamlPath.toFile());
        assertThat(result).isNotNull();
        assertThat(result.get("key")).isEqualTo("value");
    }

    @Test
    public void testLoadYamlFileNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> YamlSupport.loadYaml((File) null));
    }

    @Test
    public void testLoadYamlFileNonExistent() {
        File nonExistent = tempDir.resolve("nonexistent.yaml").toFile();
        assertThatExceptionOfType(IOException.class).isThrownBy(() -> YamlSupport.loadYaml(nonExistent));
    }

    @Test
    public void testLoadYamlFileInvalid() throws IOException {
        Path invalidPath = tempDir.resolve("invalid.yaml");
        Files.write(invalidPath, "key: {{invalid".getBytes());
        assertThatExceptionOfType(Exception.class).isThrownBy(() -> YamlSupport.loadYaml(invalidPath.toFile()));
    }
}
