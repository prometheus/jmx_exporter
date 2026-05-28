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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourceSupportExportTest {

    @TempDir
    File tempDir;

    @Test
    void exportWritesResourceContentToFile() throws IOException {
        File outputFile = new File(tempDir, "output.txt");
        ResourceSupport.export("/MapAccessorTest.yaml", outputFile);

        assertThat(outputFile).exists();
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        assertThat(content).contains("httpServer");
    }

    @Test
    void exportThrowsForMissingResource() {
        File outputFile = new File(tempDir, "output.txt");
        assertThatThrownBy(() -> ResourceSupport.export("/nonexistent.resource", outputFile))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void exportThrowsForNullOrEmptyResource() {
        File outputFile = new File(tempDir, "output.txt");
        assertThatThrownBy(() -> ResourceSupport.export("", outputFile)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ResourceSupport.export(null, outputFile)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportThrowsForNullFile() {
        assertThatThrownBy(() -> ResourceSupport.export("/MapAccessorTest.yaml", (File) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportWorksWithoutLeadingSlash() throws IOException {
        File outputFile = new File(tempDir, "output2.txt");
        ResourceSupport.export("MapAccessorTest.yaml", outputFile);

        assertThat(outputFile).exists();
    }
}
