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

package io.prometheus.jmx.logger;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LoggerTest {

    private java.util.logging.Logger julLogger;
    private TestLogHandler testLogHandler;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = LoggerFactory.getLogger(LoggerTest.class);
        julLogger = java.util.logging.Logger.getLogger(LoggerTest.class.getName());
        testLogHandler = new TestLogHandler();
        julLogger.addHandler(testLogHandler);
        julLogger.setLevel(java.util.logging.Level.ALL);
    }

    @AfterEach
    void tearDown() {
        julLogger.removeHandler(testLogHandler);
    }

    @Nested
    class IsEnabledTests {

        @Test
        void isInfoEnabled() {
            assertThat(logger.isInfoEnabled()).isTrue();
        }

        @Test
        void isWarnEnabled() {
            assertThat(logger.isWarnEnabled()).isTrue();
        }

        @Test
        void isErrorEnabled() {
            assertThat(logger.isErrorEnabled()).isTrue();
        }

        @Test
        void isTraceEnabledReturnsFalseWhenLevelIsInfo() {
            julLogger.setLevel(java.util.logging.Level.INFO);
            assertThat(logger.isTraceEnabled()).isFalse();
        }

        @Test
        void isInfoEnabledReturnsFalseWhenLevelIsWarning() {
            julLogger.setLevel(java.util.logging.Level.WARNING);
            assertThat(logger.isInfoEnabled()).isFalse();
        }

        @Test
        void isWarnEnabledReturnsFalseWhenLevelIsSevere() {
            julLogger.setLevel(java.util.logging.Level.SEVERE);
            assertThat(logger.isWarnEnabled()).isFalse();
        }
    }

    @Nested
    class InfoTests {

        @Test
        void infoWithString() {
            logger.info("test info message");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("test info message");
        }

        @Test
        void infoWithFormatAndObject() {
            logger.info("value is %s", 42);
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("value is 42");
        }

        @Test
        void infoWithFormatAndVarargs() {
            logger.info("a=%s b=%s", "hello", "world");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("a=hello b=world");
        }
    }

    @Nested
    class WarnTests {

        @Test
        void warnWithString() {
            logger.warn("test warn message");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("test warn message");
            assertThat(testLogHandler.getRecords().get(0).getLevel()).isEqualTo(java.util.logging.Level.WARNING);
        }

        @Test
        void warnWithFormatAndObject() {
            logger.warn("warning: %s", "deprecation");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("warning: deprecation");
        }

        @Test
        void warnWithFormatAndVarargs() {
            logger.warn("x=%s y=%s", 1, 2);
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("x=1 y=2");
        }
    }

    @Nested
    class ErrorTests {

        @Test
        void errorWithString() {
            logger.error("test error message");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("test error message");
            assertThat(testLogHandler.getRecords().get(0).getLevel()).isEqualTo(java.util.logging.Level.SEVERE);
        }

        @Test
        void errorWithFormatAndVarargs() {
            logger.error("error code=%s msg=%s", 500, "internal");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("error code=500 msg=internal");
        }
    }

    @Nested
    class LogNotLoggableTests {

        @Test
        void infoNotLoggableWhenLevelIsWarning() {
            julLogger.setLevel(java.util.logging.Level.WARNING);
            logger.info("should not log");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }

        @Test
        void warnNotLoggableWhenLevelIsSevere() {
            julLogger.setLevel(java.util.logging.Level.SEVERE);
            logger.warn("should not log");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }

        @Test
        void errorNotLoggableWhenLevelIsOff() {
            julLogger.setLevel(java.util.logging.Level.OFF);
            logger.error("should not log");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }
    }

    @Nested
    class LogVarargsNotLoggableTests {

        @Test
        void infoFormatNotLoggableWhenLevelIsWarning() {
            julLogger.setLevel(java.util.logging.Level.WARNING);
            logger.info("should not log %s", "arg");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }

        @Test
        void warnFormatNotLoggableWhenLevelIsSevere() {
            julLogger.setLevel(java.util.logging.Level.SEVERE);
            logger.warn("should not log %s", "arg");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }

        @Test
        void errorFormatNotLoggableWhenLevelIsOff() {
            julLogger.setLevel(java.util.logging.Level.OFF);
            logger.error("should not log %s %s", "a", "b");
            assertThat(testLogHandler.getRecords()).isEmpty();
        }
    }

    @Nested
    class TraceLoggingTests {

        @Test
        void traceWithMessage() {
            logger.trace("trace message");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("trace message");
            assertThat(testLogHandler.getRecords().get(0).getLevel()).isEqualTo(java.util.logging.Level.FINEST);
        }

        @Test
        void traceWithFormatAndObject() {
            logger.trace("value is %s", 99);
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("value is 99");
        }

        @Test
        void traceWithFormatAndVarargs() {
            logger.trace("a=%s b=%s", "x", "y");
            assertThat(testLogHandler.getRecords()).hasSize(1);
            assertThat(testLogHandler.getRecords().get(0).getMessage()).isEqualTo("a=x b=y");
        }
    }

    @Nested
    class ConstructorFormatterTests {

        @Test
        void constructorWithNullFormatterHandlerDoesNotThrow() {
            Class<?> testClass = ConstructorFormatterTests.class;
            java.util.logging.Logger testJulLogger = java.util.logging.Logger.getLogger(testClass.getName());
            Handler nullFormatterHandler = new NullFormatterHandler();
            testJulLogger.addHandler(nullFormatterHandler);
            try {
                Logger constructed = createLoggerViaReflection(testClass);
                assertThat(constructed).isNotNull();
            } finally {
                testJulLogger.removeHandler(nullFormatterHandler);
            }
        }

        @Test
        void constructorWithNonSimpleFormatterHandlerDoesNotReplaceFormatter() {
            Class<?> testClass = ConstructorFormatterTests.class;
            java.util.logging.Logger testJulLogger = java.util.logging.Logger.getLogger(testClass.getName());
            LoggerFormatter customFormatter = new LoggerFormatter();
            Handler nonSimpleHandler = new StreamHandler(new ByteArrayOutputStream(), customFormatter);
            testJulLogger.addHandler(nonSimpleHandler);
            try {
                createLoggerViaReflection(testClass);
                assertThat(nonSimpleHandler.getFormatter()).isSameAs(customFormatter);
            } finally {
                testJulLogger.removeHandler(nonSimpleHandler);
            }
        }

        @Test
        void constructorWithSimpleFormatterHandlerReplacesWithLoggerFormatter() {
            Class<?> testClass = ConstructorFormatterTests.class;
            java.util.logging.Logger testJulLogger = java.util.logging.Logger.getLogger(testClass.getName());
            SimpleFormatter simpleFormatter = new SimpleFormatter();
            Handler simpleHandler = new StreamHandler(new ByteArrayOutputStream(), simpleFormatter);
            testJulLogger.addHandler(simpleHandler);
            try {
                createLoggerViaReflection(testClass);
                assertThat(simpleHandler.getFormatter()).isInstanceOf(LoggerFormatter.class);
            } finally {
                testJulLogger.removeHandler(simpleHandler);
            }
        }

        private Logger createLoggerViaReflection(Class<?> clazz) {
            try {
                Constructor<Logger> constructor = Logger.class.getDeclaredConstructor(Class.class);
                constructor.setAccessible(true);
                return constructor.newInstance(clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class NullFormatterHandler extends Handler {

        @Override
        public void publish(LogRecord record) {
            // Intentionally empty
        }

        @Override
        public void flush() {
            // Intentionally empty
        }

        @Override
        public void close() throws SecurityException {
            // Intentionally empty
        }

        @Override
        public Formatter getFormatter() {
            return null;
        }
    }

    @Nested
    class DecodeMappingTests {

        @Test
        void decodeReturnsNonNullForAllLevelValues() throws Exception {
            Method decodeMethod = Logger.class.getDeclaredMethod("decode", Level.class);
            decodeMethod.setAccessible(true);

            for (Level level : Level.values()) {
                Object result = decodeMethod.invoke(null, level);
                assertThat(result).isNotNull();
            }
        }

        @Test
        void decodeMapsTraceToFinest() throws Exception {
            Method decodeMethod = Logger.class.getDeclaredMethod("decode", Level.class);
            decodeMethod.setAccessible(true);

            Object result = decodeMethod.invoke(null, Level.TRACE);
            assertThat(result).isEqualTo(java.util.logging.Level.FINEST);
        }

        @Test
        void decodeMapsInfoToInfo() throws Exception {
            Method decodeMethod = Logger.class.getDeclaredMethod("decode", Level.class);
            decodeMethod.setAccessible(true);

            Object result = decodeMethod.invoke(null, Level.INFO);
            assertThat(result).isEqualTo(java.util.logging.Level.INFO);
        }

        @Test
        void decodeMapsWarnToWarning() throws Exception {
            Method decodeMethod = Logger.class.getDeclaredMethod("decode", Level.class);
            decodeMethod.setAccessible(true);

            Object result = decodeMethod.invoke(null, Level.WARN);
            assertThat(result).isEqualTo(java.util.logging.Level.WARNING);
        }

        @Test
        void decodeMapsErrorToSevere() throws Exception {
            Method decodeMethod = Logger.class.getDeclaredMethod("decode", Level.class);
            decodeMethod.setAccessible(true);

            Object result = decodeMethod.invoke(null, Level.ERROR);
            assertThat(result).isEqualTo(java.util.logging.Level.SEVERE);
        }
    }

    @Nested
    class DeveloperDebugTests {

        private String originalProp;

        @BeforeEach
        void setDebugProperty() {
            originalProp = System.getProperty("jmx.prometheus.exporter.developer.debug");
            System.setProperty("jmx.prometheus.exporter.developer.debug", "true");
        }

        @AfterEach
        void clearDebugProperty() {
            if (originalProp != null) {
                System.setProperty("jmx.prometheus.exporter.developer.debug", originalProp);
            } else {
                System.clearProperty("jmx.prometheus.exporter.developer.debug");
            }
        }

        @Test
        void developerDebugWritesToStdout() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));
            try {
                Logger debugLogger = createFreshLogger(DeveloperDebugTests.class);

                debugLogger.info("debug test message");

                String output = baos.toString();
                assertThat(output).contains("debug test message");
                assertThat(output).contains(DeveloperDebugTests.class.getName());
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        void developerDebugWithFormatString() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));
            try {
                Logger debugLogger = createFreshLogger(DeveloperDebugTests.class);

                debugLogger.info("format %s %s", "hello", "world");

                String output = baos.toString();
                assertThat(output).contains("hello");
                assertThat(output).contains("world");
                assertThat(output).contains(DeveloperDebugTests.class.getName());
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        void developerDebugWithFormatWhenNotLoggable() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));
            try {
                Logger debugLogger = createFreshLogger(DeveloperDebugTests.class);

                java.util.logging.Logger julDebugLogger =
                        java.util.logging.Logger.getLogger(DeveloperDebugTests.class.getName());
                julDebugLogger.setLevel(java.util.logging.Level.OFF);

                debugLogger.info("only stdout %s", "output");

                String output = baos.toString();
                assertThat(output).contains("only stdout");
                assertThat(output).contains("output");
            } finally {
                System.setOut(originalOut);
            }
        }

        @Test
        void developerDebugIncludesTimestampThreadAndLevel() {
            PrintStream originalOut = System.out;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true));
            try {
                Logger debugLogger = createFreshLogger(DeveloperDebugTests.class);

                debugLogger.warn("warn message");

                String output = baos.toString();
                assertThat(output).contains(" | ");
                assertThat(output).contains(Thread.currentThread().getName());
                assertThat(output).contains(DeveloperDebugTests.class.getName());
            } finally {
                System.setOut(originalOut);
            }
        }

        private Logger createFreshLogger(Class<?> clazz) {
            try {
                Constructor<Logger> constructor = Logger.class.getDeclaredConstructor(Class.class);
                constructor.setAccessible(true);
                return constructor.newInstance(clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class TestLogHandler extends Handler {

        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
            // Intentionally empty
        }

        @Override
        public void close() throws SecurityException {
            // Intentionally empty
        }

        List<LogRecord> getRecords() {
            return records;
        }
    }
}
