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

import io.prometheus.jmx.common.util.MapAccessor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import nl.altindag.ssl.SSLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HTTPServerFactorySslReloadTest {

    @TempDir
    Path tempDir;

    @Test
    void getContentHashUsesFileContents() throws Exception {
        Path keyStore = tempDir.resolve("keystore.jks");

        write(keyStore, "alpha");
        String firstHash =
                (String) invokePrivateMethod("getContentHash", new Class<?>[] {String.class}, keyStore.toString());

        write(keyStore, "alpha");
        String secondHash =
                (String) invokePrivateMethod("getContentHash", new Class<?>[] {String.class}, keyStore.toString());

        write(keyStore, "beta");
        String thirdHash =
                (String) invokePrivateMethod("getContentHash", new Class<?>[] {String.class}, keyStore.toString());

        assertThat(secondHash).isEqualTo(firstHash);
        assertThat(thirdHash).isNotEqualTo(firstHash);
    }

    @Test
    void reloadSslSkipsReloadWhenRuntimeKeyStoreCannotBeRead() throws Exception {
        Path keyStore = tempDir.resolve("keystore.jks");
        write(keyStore, "alpha");

        Map<Object, Object> keyStoreConfig = new HashMap<>();
        keyStoreConfig.put("filename", keyStore.toString());
        keyStoreConfig.put("password", "secret");
        keyStoreConfig.put("type", "JKS");

        Map<Object, Object> certificateConfig = new HashMap<>();
        certificateConfig.put("alias", "localhost");

        Map<Object, Object> sslConfig = new HashMap<>();
        sslConfig.put("keyStore", keyStoreConfig);
        sslConfig.put("certificate", certificateConfig);

        Map<Object, Object> httpServerConfig = new HashMap<>();
        httpServerConfig.put("ssl", sslConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", httpServerConfig);

        MapAccessor rootMapAccessor = MapAccessor.of(config);

        Object keyStoreProperties =
                invokePrivateMethod("getKeyStoreProperties", new Class<?>[] {MapAccessor.class}, rootMapAccessor);

        // Create KeyStoreIdentityMaterial wrapping the keyStoreProperties
        Class<?> identityMaterialClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreIdentityMaterial");
        Class<?> keyStorePropertiesClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreProperties");
        java.lang.reflect.Constructor<?> identityMaterialConstructor =
                identityMaterialClass.getDeclaredConstructor(keyStorePropertiesClass);
        identityMaterialConstructor.setAccessible(true);
        Object identityMaterial = identityMaterialConstructor.newInstance(keyStoreProperties);

        // Create SslReloadState
        Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
        java.lang.reflect.Constructor<?> reloadStateConstructor = sslReloadStateClass.getDeclaredConstructor(
                io.prometheus.jmx.common.http.ssl.IdentityMaterial.class, Optional.class);
        reloadStateConstructor.setAccessible(true);
        Object reloadState = reloadStateConstructor.newInstance(identityMaterial, Optional.empty());

        // Create ConfiguredSslFactory
        Class<?> configuredSslFactoryClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$ConfiguredSslFactory");
        java.lang.reflect.Constructor<?> configuredConstructor = configuredSslFactoryClass.getDeclaredConstructor(
                SSLFactory.class, sslReloadStateClass, MapAccessor.class);
        configuredConstructor.setAccessible(true);
        Object configured = configuredConstructor.newInstance(null, reloadState, rootMapAccessor);

        Files.delete(keyStore);

        invokePrivateMethod("reloadSsl", new Class<?>[] {configuredSslFactoryClass}, configured);

        // Verify the identity material was not changed (same instance retained)
        Field identityMaterialField = sslReloadStateClass.getDeclaredField("identityMaterial");
        identityMaterialField.setAccessible(true);
        assertThat(identityMaterialField.get(reloadState)).isSameAs(identityMaterial);
    }

    @Test
    void reloadSslPemNoChangeSkipsReload() throws Exception {
        Path certFile = tempDir.resolve("cert.pem");
        Path keyFile = tempDir.resolve("key.pem");
        copyTestResource("test-cert.pem", certFile);
        copyTestResource("test-key-pkcs8.pem", keyFile);

        MapAccessor config = createPemConfig(certFile, keyFile);

        io.prometheus.jmx.common.http.ssl.IdentityMaterial material =
                io.prometheus.jmx.common.http.ssl.PemIdentityLoader.load(config);

        Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
        java.lang.reflect.Constructor<?> reloadStateConstructor = sslReloadStateClass.getDeclaredConstructor(
                io.prometheus.jmx.common.http.ssl.IdentityMaterial.class, Optional.class);
        reloadStateConstructor.setAccessible(true);
        Object reloadState = reloadStateConstructor.newInstance(material, Optional.empty());

        Class<?> configuredSslFactoryClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$ConfiguredSslFactory");
        java.lang.reflect.Constructor<?> configuredConstructor = configuredSslFactoryClass.getDeclaredConstructor(
                SSLFactory.class, sslReloadStateClass, MapAccessor.class);
        configuredConstructor.setAccessible(true);
        Object configured = configuredConstructor.newInstance(null, reloadState, config);

        invokePrivateMethod("reloadSsl", new Class<?>[] {configuredSslFactoryClass}, configured);

        Field identityMaterialField = sslReloadStateClass.getDeclaredField("identityMaterial");
        identityMaterialField.setAccessible(true);
        assertThat(identityMaterialField.get(reloadState)).isSameAs(material);
    }

    @Test
    void reloadSslPemIdentityChangedTriggersReload() throws Exception {
        Path certFile = tempDir.resolve("cert.pem");
        Path keyFile = tempDir.resolve("key.pem");
        copyTestResource("test-cert.pem", certFile);
        copyTestResource("test-key-pkcs8.pem", keyFile);

        MapAccessor config = createPemConfig(certFile, keyFile);

        io.prometheus.jmx.common.http.ssl.IdentityMaterial material =
                io.prometheus.jmx.common.http.ssl.PemIdentityLoader.load(config);

        Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
        java.lang.reflect.Constructor<?> reloadStateConstructor = sslReloadStateClass.getDeclaredConstructor(
                io.prometheus.jmx.common.http.ssl.IdentityMaterial.class, Optional.class);
        reloadStateConstructor.setAccessible(true);
        Object reloadState = reloadStateConstructor.newInstance(material, Optional.empty());

        Class<?> configuredSslFactoryClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$ConfiguredSslFactory");
        java.lang.reflect.Constructor<?> configuredConstructor = configuredSslFactoryClass.getDeclaredConstructor(
                SSLFactory.class, sslReloadStateClass, MapAccessor.class);
        configuredConstructor.setAccessible(true);
        Object configured = configuredConstructor.newInstance(null, reloadState, config);

        // Modify key file content to trigger change detection
        write(keyFile, "modified key content");

        invokePrivateMethod("reloadSsl", new Class<?>[] {configuredSslFactoryClass}, configured);

        // Identity material should be different since file was changed and reload should have failed
        // (invalid content), so prior identity should be retained
        Field identityMaterialField = sslReloadStateClass.getDeclaredField("identityMaterial");
        identityMaterialField.setAccessible(true);
        assertThat(identityMaterialField.get(reloadState)).isSameAs(material);
    }

    @Test
    void reloadSslPemRetainsPriorOnFileDisappears() throws Exception {
        Path certFile = tempDir.resolve("cert.pem");
        Path keyFile = tempDir.resolve("key.pem");
        copyTestResource("test-cert.pem", certFile);
        copyTestResource("test-key-pkcs8.pem", keyFile);

        MapAccessor config = createPemConfig(certFile, keyFile);

        io.prometheus.jmx.common.http.ssl.IdentityMaterial material =
                io.prometheus.jmx.common.http.ssl.PemIdentityLoader.load(config);

        Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
        java.lang.reflect.Constructor<?> reloadStateConstructor = sslReloadStateClass.getDeclaredConstructor(
                io.prometheus.jmx.common.http.ssl.IdentityMaterial.class, Optional.class);
        reloadStateConstructor.setAccessible(true);
        Object reloadState = reloadStateConstructor.newInstance(material, Optional.empty());

        Class<?> configuredSslFactoryClass =
                Class.forName("io.prometheus.jmx.common.HTTPServerFactory$ConfiguredSslFactory");
        java.lang.reflect.Constructor<?> configuredConstructor = configuredSslFactoryClass.getDeclaredConstructor(
                SSLFactory.class, sslReloadStateClass, MapAccessor.class);
        configuredConstructor.setAccessible(true);
        Object configured = configuredConstructor.newInstance(null, reloadState, config);

        // Delete key file to make it unreadable
        Files.delete(keyFile);

        invokePrivateMethod("reloadSsl", new Class<?>[] {configuredSslFactoryClass}, configured);

        // Identity material should be retained (same instance) since file is missing
        Field identityMaterialField = sslReloadStateClass.getDeclaredField("identityMaterial");
        identityMaterialField.setAccessible(true);
        assertThat(identityMaterialField.get(reloadState)).isSameAs(material);
    }

    private static Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = HTTPServerFactory.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static void write(Path path, String value) throws Exception {
        Files.write(
                path,
                value.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void copyTestResource(String resourceName, Path target) throws Exception {
        Path source = Paths.get("src/test/resources/io/prometheus/jmx/common/http/ssl/", resourceName);
        Files.copy(source, target);
    }

    private static MapAccessor createPemConfig(Path certPath, Path keyPath) {
        Map<Object, Object> certConfig = new HashMap<>();
        certConfig.put("filename", certPath.toString());

        Map<Object, Object> keyConfig = new HashMap<>();
        keyConfig.put("filename", keyPath.toString());

        Map<Object, Object> pemConfig = new HashMap<>();
        pemConfig.put("certificate", certConfig);
        pemConfig.put("privateKey", keyConfig);

        Map<Object, Object> sslConfig = new HashMap<>();
        sslConfig.put("pem", pemConfig);

        Map<Object, Object> httpServerConfig = new HashMap<>();
        httpServerConfig.put("ssl", sslConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", httpServerConfig);

        return MapAccessor.of(config);
    }
}
