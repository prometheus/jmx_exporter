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

package io.prometheus.jmx.common.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CustomServiceTransformerTest {

    private static final String PREFIX = "e1723a08afd7bca35570fd31a7656f59.";
    private static final String SERVICES_DIR = "META-INF/services/";

    @Nested
    class CanTransformResourceTests {

        @Test
        void canTransformServiceResource() {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            assertThat(transformer.canTransformResource("META-INF/services/com.example.MyService"))
                    .isTrue();
        }

        @Test
        void cannotTransformNonServiceResource() {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            assertThat(transformer.canTransformResource("com/example/MyClass.class"))
                    .isFalse();
        }

        @Test
        void cannotTransformRootMetaInf() {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            assertThat(transformer.canTransformResource("META-INF/MANIFEST.MF")).isFalse();
        }
    }

    @Nested
    class HasTransformedResourceTests {

        @Test
        void noTransformedResourcesInitially() {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            assertThat(transformer.hasTransformedResource()).isFalse();
        }

        @Test
        void hasTransformedResourcesAfterProcessing() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "com.example.Impl1\ncom.example.Impl2\n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService", new ByteArrayInputStream(content), new ArrayList<>());
            assertThat(transformer.hasTransformedResource()).isTrue();
        }
    }

    @Nested
    class ProcessResourceTests {

        @Test
        void processResourceAddsPrefix() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "com.example.Impl1\n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            assertThat(transformer.hasTransformedResource()).isTrue();
        }

        @Test
        void processResourceSkipsAlreadyPrefixedEntries() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = (PREFIX + "com.example.Impl1\n").getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            assertThat(transformer.hasTransformedResource()).isTrue();
        }

        @Test
        void processResourceDeduplicatesEntries() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "com.example.Impl1\ncom.example.Impl1\n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JarOutputStream jos = new JarOutputStream(baos)) {
                transformer.modifyOutputStream(jos);
            }

            assertThat(baos.toByteArray().length).isGreaterThan(0);
        }

        @Test
        void processResourceSkipsBlankLines() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "com.example.Impl1\n\n   \ncom.example.Impl2\n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            assertThat(transformer.hasTransformedResource()).isTrue();
        }

        @Test
        void processResourceTrimsLines() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "  com.example.Impl1  \n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            assertThat(transformer.hasTransformedResource()).isTrue();
        }
    }

    @Nested
    class ModifyOutputStreamTests {

        @Test
        void modifyOutputStreamWritesPrefixedServiceName() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content = "com.example.Impl1\n".getBytes(StandardCharsets.UTF_8);
            transformer.processResource(
                    "META-INF/services/com.example.MyService",
                    new ByteArrayInputStream(content),
                    Collections.emptyList());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JarOutputStream jos = new JarOutputStream(baos)) {
                transformer.modifyOutputStream(jos);
            }

            assertThat(baos.toByteArray().length).isGreaterThan(0);
        }

        @Test
        void modifyOutputStreamWritesMultipleServices() throws IOException {
            CustomServiceTransformer transformer = new CustomServiceTransformer();
            byte[] content1 = "com.example.Impl1\n".getBytes(StandardCharsets.UTF_8);
            byte[] content2 = "com.example.Impl2\n".getBytes(StandardCharsets.UTF_8);

            transformer.processResource(
                    "META-INF/services/com.example.ServiceA",
                    new ByteArrayInputStream(content1),
                    Collections.emptyList());
            transformer.processResource(
                    "META-INF/services/com.example.ServiceB",
                    new ByteArrayInputStream(content2),
                    Collections.emptyList());

            assertThat(transformer.hasTransformedResource()).isTrue();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (JarOutputStream jos = new JarOutputStream(baos)) {
                transformer.modifyOutputStream(jos);
            }

            assertThat(baos.toByteArray().length).isGreaterThan(0);
        }
    }
}
