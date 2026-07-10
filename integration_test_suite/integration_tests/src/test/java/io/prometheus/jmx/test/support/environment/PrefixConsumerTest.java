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

package io.prometheus.jmx.test.support.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.altcontainers.api.OutputFrame;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PrefixConsumer}.
 */
class PrefixConsumerTest {

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        resetEnabled();
    }

    @Test
    void ofReturnsNoopByDefault() {
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        Consumer<OutputFrame> consumer2 = PrefixConsumer.of("P", "img");

        consumer.accept(frame("hello"));
        assertEquals("", outContent.toString());

        assertSame(consumer, consumer2);
    }

    @Test
    void ofReturnsNoopWhenSystemPropertyAbsent() {
        System.clearProperty("integration.tests.container.logs");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("", outContent.toString());
    }

    @Test
    void ofReturnsNoopWhenSystemPropertyIsFalse() {
        System.setProperty("integration.tests.container.logs", "false");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("", outContent.toString());
    }

    @Test
    void ofPrintsWhenSystemPropertyIsTrue() {
        System.setProperty("integration.tests.container.logs", "true");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        Consumer<OutputFrame> consumer2 = PrefixConsumer.of("P", "img");

        consumer.accept(frame("hello"));
        assertEquals("[P] img | hello" + System.lineSeparator(), outContent.toString());

        assertNotSame(consumer, consumer2);
    }

    @Test
    void ofEnvVarPrecedesSystemProperty() {
        System.setProperty("integration.tests.container.logs", "false");
        setEnv("INTEGRATION_TESTS_CONTAINER_LOGS", "true");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("[P] img | hello" + System.lineSeparator(), outContent.toString());
    }

    @Test
    void ofReturnsNoopWhenEnvVarIsFalse() {
        setEnv("INTEGRATION_TESTS_CONTAINER_LOGS", "false");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("", outContent.toString());
    }

    @Test
    void ofReturnsNoopWhenEnvVarIsNotTrue() {
        setEnv("INTEGRATION_TESTS_CONTAINER_LOGS", "1");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("", outContent.toString());
    }

    @Test
    void ofPrintsWhenEnvVarIsTrue() {
        setEnv("INTEGRATION_TESTS_CONTAINER_LOGS", "true");
        resetEnabled();
        Consumer<OutputFrame> consumer = PrefixConsumer.of("P", "img");
        consumer.accept(frame("hello"));
        assertEquals("[P] img | hello" + System.lineSeparator(), outContent.toString());
    }

    private static OutputFrame frame(String text) {
        return new OutputFrame(OutputFrame.Type.STDOUT, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void resetEnabled() {
        try {
            Field field = PrefixConsumer.class.getDeclaredField("ENABLED");
            field.setAccessible(true);
            field.set(null, invokeResolveEnabled());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean invokeResolveEnabled() {
        try {
            java.lang.reflect.Method method = PrefixConsumer.class.getDeclaredMethod("resolveEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setEnv(String key, String value) {
        try {
            java.util.Map<String, String> env = System.getenv();
            Class<?> clazz = env.getClass();
            java.lang.reflect.Field field = clazz.getDeclaredField("m");
            field.setAccessible(true);
            ((java.util.Map<String, String>) field.get(env)).put(key, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
