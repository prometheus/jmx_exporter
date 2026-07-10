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

package io.prometheus.jmx.common.http;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

import io.prometheus.jmx.common.ConfigurationException;
import io.prometheus.jmx.common.HTTPServerFactory;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class HTTPServerFactorySSLTest {

    @TempDir
    File temporaryFolder;

    HTTPServer httpServer;

    @AfterEach
    void stopServer() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
        }
    }

    private HTTPServer startServer(File config) throws IOException {
        return HTTPServerFactory.createAndStartHTTPServer(
                PrometheusRegistry.defaultRegistry, InetAddress.getByName("0.0.0.0"), 0, config);
    }

    @Test
    void authenticationMissingUsernameThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "no_username.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      password: secret");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationMissingPasswordThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "no_password.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      username: prometheus");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationSHAWithMissingSaltThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "sha_no_salt.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      username: prometheus");
            writer.println("      passwordHash: somehash");
            writer.println("      algorithm: SHA-256");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationSHAWithInvalidHexThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "sha_invalid_hex.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      username: prometheus");
            writer.println("      passwordHash: GG");
            writer.println("      algorithm: SHA-256");
            writer.println("      salt: salt");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> httpServer = startServer(config))
                .withMessageContaining("non-hexadecimal");
    }

    @Test
    void authenticationPBKDF2WithMissingSaltThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "pbkdf2_no_salt.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      username: prometheus");
            writer.println("      passwordHash: somehash");
            writer.println("      algorithm: PBKDF2WithHmacSHA256");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationPBKDF2WithWeakConfigurationWarningPathIsAcceptedForBackwardCompatibility() throws Exception {
        Method method = HTTPServerFactory.class.getDeclaredMethod(
                "warnIfWeakPBKDF2Configuration", String.class, String.class, int.class, int.class);
        method.setAccessible(true);

        method.invoke(null, "PBKDF2WithHmacSHA256", "00", 1, 8);
    }

    @Test
    void authenticationPBKDF2WithLegacyByteKeyLengthDoesNotReduceEffectiveKeyLength() throws Exception {
        Method method =
                HTTPServerFactory.class.getDeclaredMethod("getEffectivePBKDF2KeyLengthBits", String.class, int.class);
        method.setAccessible(true);

        Object effectiveKeyLengthBits = method.invoke(null, "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9", 16);

        assertThat(effectiveKeyLengthBits).isEqualTo(128);
    }

    @Test
    void authenticationBasicNotAMapThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "basic_not_map.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    basic: not_a_map");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationPluginNotAMapThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "plugin_not_map.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    plugin: not_a_map");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void authenticationPluginClassNotAuthenticatorThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "plugin_not_authenticator.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  authentication:");
            writer.println("    plugin:");
            writer.println("      class: java.lang.String");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void threadMinimumNotIntegerThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "thread_min_not_int.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  threads:");
            writer.println("    minimum: not_an_int");
            writer.println("    maximum: 20");
            writer.println("    keepAliveTime: 60");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void threadMaximumNotIntegerThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "thread_max_not_int.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  threads:");
            writer.println("    minimum: 2");
            writer.println("    maximum: not_an_int");
            writer.println("    keepAliveTime: 60");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void threadKeepAliveNotIntegerThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "thread_ka_not_int.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  threads:");
            writer.println("    minimum: 2");
            writer.println("    maximum: 20");
            writer.println("    keepAliveTime: not_an_int");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void threadMinimumOutOfRangeThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "thread_min_out_of_range.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  threads:");
            writer.println("    minimum: 0");
            writer.println("    maximum: 20");
            writer.println("    keepAliveTime: 60");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void metricsPathBlankThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "metrics_blank.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  metrics:");
            writer.println("    path: \"\"");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void sslWithMissingKeyStoreFileThrowsConfigurationException() throws Exception {
        File config = new File(temporaryFolder, "ssl_missing_ks.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  ssl:");
            writer.println("    keyStore:");
            writer.println("      type: PKCS12");
            writer.println("      filename: /nonexistent/path/keystore.pkcs12");
            writer.println("      password: changeit");
            writer.println("    certificate:");
            writer.println("      alias: localhost");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void sslWithMissingCertificateAliasThrowsConfigurationException() throws Exception {
        File tempFile = new File(temporaryFolder, "temp_keystore.bin");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(new byte[] {0x00, 0x01, 0x02, 0x03});
        }

        File config = new File(temporaryFolder, "ssl_no_alias.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  ssl:");
            writer.println("    keyStore:");
            writer.println("      type: PKCS12");
            writer.println("      filename: " + tempFile.getAbsolutePath());
            writer.println("      password: changeit");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void sslWithUnsupportedAlgorithmThrowsConfigurationException() throws Exception {
        File tempFile = new File(temporaryFolder, "temp_keystore2.bin");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(new byte[] {0x00, 0x01, 0x02, 0x03});
        }

        File config = new File(temporaryFolder, "ssl_bad_algo.yaml");
        try (PrintWriter writer = new PrintWriter(config)) {
            writer.println("httpServer:");
            writer.println("  ssl:");
            writer.println("    keyStore:");
            writer.println("      type: PKCS12");
            writer.println("      filename: " + tempFile.getAbsolutePath());
            writer.println("      password: changeit");
            writer.println("    certificate:");
            writer.println("      alias: localhost");
            writer.println("  authentication:");
            writer.println("    basic:");
            writer.println("      username: prometheus");
            writer.println("      passwordHash: somehash");
            writer.println("      algorithm: BOGUS-ALGO");
            writer.println("      salt: testsalt");
            writer.println("hostPort: application:9999");
            writer.println("rules:");
            writer.println("  - pattern: \".*\"");
        }

        assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> httpServer = startServer(config));
    }

    @Test
    void toHexViaReflection() throws Exception {
        Method toHex = HTTPServerFactory.class.getDeclaredMethod("toHex", byte[].class);
        toHex.setAccessible(true);

        byte[] input = new byte[] {(byte) 0xff, 0x00, 0x0a, 0x10};
        String result = (String) toHex.invoke(null, input);
        assertThat(result).isEqualTo("ff000a10");

        byte[] empty = new byte[0];
        String emptyResult = (String) toHex.invoke(null, empty);
        assertThat(emptyResult).isEmpty();
    }

    @Test
    void getContentHashValidFile() throws Exception {
        Method getContentHash = HTTPServerFactory.class.getDeclaredMethod("getContentHash", String.class);
        getContentHash.setAccessible(true);

        File tempFile = new File(temporaryFolder, "hash_test.bin");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(new byte[] {0x00, 0x01, 0x02, 0x03});
        }

        String hash = (String) getContentHash.invoke(null, tempFile.getAbsolutePath());
        assertThat(hash).isNotBlank();
        assertThat(hash.length()).isEqualTo(64);
    }

    @Test
    void getContentHashNonExistentFileThrowsException() throws Exception {
        Method getContentHash = HTTPServerFactory.class.getDeclaredMethod("getContentHash", String.class);
        getContentHash.setAccessible(true);

        try {
            getContentHash.invoke(null, "/nonexistent/file.pkcs12");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getCause()).isInstanceOf(ConfigurationException.class);
        }
    }

    @Test
    void getContentHashPathOverloadReturnsEmptyForNonExistent() throws Exception {
        Method getContentHashPath = HTTPServerFactory.class.getDeclaredMethod("getContentHash", Path.class);
        getContentHashPath.setAccessible(true);

        Optional<?> result = (Optional<?>) getContentHashPath.invoke(null, Paths.get("/nonexistent/file.pkcs12"));
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    void getContentHashPathOverloadReturnsPresentForValidFile() throws Exception {
        Method getContentHashPath = HTTPServerFactory.class.getDeclaredMethod("getContentHash", Path.class);
        getContentHashPath.setAccessible(true);

        File tempFile = new File(temporaryFolder, "hash_test2.bin");
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(new byte[] {0x00, 0x01, 0x02, 0x03});
        }

        Optional<?> result = (Optional<?>) getContentHashPath.invoke(null, tempFile.toPath());
        assertThat(result.isPresent()).isTrue();
    }

    @Test
    void addSecurityHeadersWithSslEnabled() throws Exception {
        Method addSecurityHeaders = HTTPServerFactory.class.getDeclaredMethod(
                "addSecurityHeaders", com.sun.net.httpserver.Headers.class, boolean.class);
        addSecurityHeaders.setAccessible(true);

        com.sun.net.httpserver.Headers headers = new com.sun.net.httpserver.Headers();
        addSecurityHeaders.invoke(null, headers, true);

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Strict-Transport-Security")).isEqualTo("max-age=31536000");
    }

    @Test
    void addSecurityHeadersWithoutSsl() throws Exception {
        Method addSecurityHeaders = HTTPServerFactory.class.getDeclaredMethod(
                "addSecurityHeaders", com.sun.net.httpserver.Headers.class, boolean.class);
        addSecurityHeaders.setAccessible(true);

        com.sun.net.httpserver.Headers headers = new com.sun.net.httpserver.Headers();
        addSecurityHeaders.invoke(null, headers, false);

        assertThat(headers.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(headers.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(headers.getFirst("Strict-Transport-Security")).isNull();
    }

    @Test
    void wrapAuthenticatorWithNullReturnsNull() throws Exception {
        Method wrapAuthenticator = HTTPServerFactory.class.getDeclaredMethod(
                "wrapAuthenticator", com.sun.net.httpserver.Authenticator.class, boolean.class);
        wrapAuthenticator.setAccessible(true);

        Object result = wrapAuthenticator.invoke(null, null, false);
        assertThat(result).isNull();
    }

    @Test
    void wrapAuthenticatorWithNonNullReturnsWrapper() throws Exception {
        Method wrapAuthenticator = HTTPServerFactory.class.getDeclaredMethod(
                "wrapAuthenticator", com.sun.net.httpserver.Authenticator.class, boolean.class);
        wrapAuthenticator.setAccessible(true);

        com.sun.net.httpserver.Authenticator delegate = new com.sun.net.httpserver.Authenticator() {
            @Override
            public Result authenticate(com.sun.net.httpserver.HttpExchange exchange) {
                return new Success(null);
            }
        };

        Object result = wrapAuthenticator.invoke(null, delegate, true);
        assertThat(result).isNotNull();
    }

    @Test
    void securityHeadersHandlerWithNullSubjectDelegates() throws Exception {
        Class<?> handlerClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SecurityHeadersHandler");
        Constructor<?> constructor = handlerClass.getDeclaredConstructor(
                com.sun.net.httpserver.HttpHandler.class,
                boolean.class,
                String.class,
                io.prometheus.metrics.core.metrics.Counter.class,
                Integer.class);
        constructor.setAccessible(true);

        io.prometheus.metrics.core.metrics.Counter rejectedCounter =
                io.prometheus.metrics.core.metrics.Counter.builder()
                        .name("test_rejected")
                        .register();
        com.sun.net.httpserver.HttpHandler delegate = exchange -> exchange.sendResponseHeaders(200, -1);
        Object handler = constructor.newInstance(delegate, false, null, rejectedCounter, null);
        assertThat(handler).isNotNull();
    }

    @Test
    void securityHeadersHandlerWithSubjectAttribute() throws Exception {
        Class<?> handlerClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SecurityHeadersHandler");
        Constructor<?> constructor = handlerClass.getDeclaredConstructor(
                com.sun.net.httpserver.HttpHandler.class,
                boolean.class,
                String.class,
                io.prometheus.metrics.core.metrics.Counter.class,
                Integer.class);
        constructor.setAccessible(true);

        io.prometheus.metrics.core.metrics.Counter rejectedCounter =
                io.prometheus.metrics.core.metrics.Counter.builder()
                        .name("test_rejected_2")
                        .register();
        com.sun.net.httpserver.HttpHandler delegate = exchange -> exchange.sendResponseHeaders(200, -1);
        Object handler = constructor.newInstance(delegate, true, "custom.subject", rejectedCounter, null);
        assertThat(handler).isNotNull();
    }

    @Test
    void securityHeadersAuthenticatorInjectsHeaders() throws Exception {
        Class<?> authClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SecurityHeadersAuthenticator");
        Constructor<?> constructor =
                authClass.getDeclaredConstructor(com.sun.net.httpserver.Authenticator.class, boolean.class);
        constructor.setAccessible(true);

        com.sun.net.httpserver.Authenticator delegate = new com.sun.net.httpserver.Authenticator() {
            @Override
            public Result authenticate(com.sun.net.httpserver.HttpExchange exchange) {
                return new Failure(401);
            }
        };

        Object authenticator = constructor.newInstance(delegate, true);
        assertThat(authenticator).isNotNull();
    }

    @Test
    void authenticationConfigurationGetters() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$AuthenticationConfiguration");
        Constructor<?> constructor =
                clazz.getDeclaredConstructor(com.sun.net.httpserver.Authenticator.class, String.class);
        constructor.setAccessible(true);

        com.sun.net.httpserver.Authenticator auth = new com.sun.net.httpserver.Authenticator() {
            @Override
            public Result authenticate(com.sun.net.httpserver.HttpExchange exchange) {
                return new Success(null);
            }
        };

        Object instance = constructor.newInstance(auth, "test.subject");
        Method getAuthenticator = clazz.getDeclaredMethod("getAuthenticator");
        Method getSubjectAttributeName = clazz.getDeclaredMethod("getSubjectAttributeName");
        getAuthenticator.setAccessible(true);
        getSubjectAttributeName.setAccessible(true);

        assertThat(getAuthenticator.invoke(instance)).isSameAs(auth);
        assertThat(getSubjectAttributeName.invoke(instance)).isEqualTo("test.subject");
    }

    @Test
    void authenticationConfigurationWithNulls() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$AuthenticationConfiguration");
        Constructor<?> constructor =
                clazz.getDeclaredConstructor(com.sun.net.httpserver.Authenticator.class, String.class);
        constructor.setAccessible(true);

        Object instance = constructor.newInstance(null, null);
        Method getAuthenticator = clazz.getDeclaredMethod("getAuthenticator");
        Method getSubjectAttributeName = clazz.getDeclaredMethod("getSubjectAttributeName");
        getAuthenticator.setAccessible(true);
        getSubjectAttributeName.setAccessible(true);

        assertThat(getAuthenticator.invoke(instance)).isNull();
        assertThat(getSubjectAttributeName.invoke(instance)).isNull();
    }

    @Test
    void namedDaemonThreadFactoryCreatesDaemonThread() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$NamedDaemonThreadFactory");
        Constructor<?> constructor = clazz.getDeclaredConstructor(ThreadFactory.class, boolean.class);
        constructor.setAccessible(true);

        Object factory = constructor.newInstance(Executors.defaultThreadFactory(), true);
        Method newThread = clazz.getDeclaredMethod("newThread", Runnable.class);
        newThread.setAccessible(true);

        Thread thread = (Thread) newThread.invoke(factory, (Runnable) () -> {});
        assertThat(thread.isDaemon()).isTrue();
        assertThat(thread.getName()).startsWith("prometheus-http-");
    }

    @Test
    void namedDaemonThreadFactoryCreatesNonDaemonThread() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$NamedDaemonThreadFactory");
        Constructor<?> constructor = clazz.getDeclaredConstructor(ThreadFactory.class, boolean.class);
        constructor.setAccessible(true);

        Object factory = constructor.newInstance(Executors.defaultThreadFactory(), false);
        Method newThread = clazz.getDeclaredMethod("newThread", Runnable.class);
        newThread.setAccessible(true);

        Thread thread = (Thread) newThread.invoke(factory, (Runnable) () -> {});
        assertThat(thread.isDaemon()).isFalse();
    }

    @Test
    void defaultThreadFactoryMethod() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$NamedDaemonThreadFactory");
        Method defaultThreadFactory = clazz.getDeclaredMethod("defaultThreadFactory", boolean.class);
        defaultThreadFactory.setAccessible(true);

        Object factory = defaultThreadFactory.invoke(null, true);
        assertThat(factory).isNotNull();
    }

    @Test
    void blockingRejectedExecutionHandlerOnNonShutdownExecutor() throws Exception {
        Class<?> clazz = Class.forName("io.prometheus.jmx.common.util.BlockingRejectedExecutionHandler");
        Constructor<?> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object handler = constructor.newInstance();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(true));

        Method rejectedExecution =
                clazz.getDeclaredMethod("rejectedExecution", Runnable.class, ThreadPoolExecutor.class);
        rejectedExecution.setAccessible(true);

        Runnable task = () -> {};
        executor.shutdownNow();
        rejectedExecution.invoke(handler, task, executor);
    }
}
