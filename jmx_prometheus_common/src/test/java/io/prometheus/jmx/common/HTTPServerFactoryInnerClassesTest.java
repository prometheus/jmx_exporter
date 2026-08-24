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

import io.prometheus.jmx.common.http.ssl.IdentityMaterial;
import io.prometheus.jmx.common.util.MapAccessor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import nl.altindag.ssl.SSLFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class HTTPServerFactoryInnerClassesTest {

    @Nested
    class KeyStorePropertiesTests {

        @Test
        void gettersReturnExpectedValues() throws Exception {
            Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreProperties");
            Constructor<?> constructor =
                    clazz.getDeclaredConstructor(String.class, String.class, char[].class, String.class, String.class);
            constructor.setAccessible(true);

            char[] password = "secret".toCharArray();
            Object instance = constructor.newInstance("/path/to/keystore", "hash123", password, "JKS", "myalias");

            Method getFilename = clazz.getDeclaredMethod("getFilename");
            Method getContentHash = clazz.getDeclaredMethod("getContentHash");
            Method getPassword = clazz.getDeclaredMethod("getPassword");
            Method getType = clazz.getDeclaredMethod("getType");
            Method getCertificateAlias = clazz.getDeclaredMethod("getCertificateAlias");

            assertThat(getFilename.invoke(instance)).isEqualTo(Paths.get("/path/to/keystore"));
            assertThat(getContentHash.invoke(instance)).isEqualTo("hash123");
            assertThat((char[]) getPassword.invoke(instance)).containsExactly('s', 'e', 'c', 'r', 'e', 't');
            assertThat(getType.invoke(instance)).isEqualTo("JKS");
            Object aliasResult = getCertificateAlias.invoke(instance);
            assertThat(aliasResult).isNotNull();
            assertThat(((Optional<?>) aliasResult).isPresent()).isTrue();
        }

        @Test
        void certificateAliasIsNullForTruststore() throws Exception {
            Class<?> clazz = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreProperties");
            Constructor<?> constructor =
                    clazz.getDeclaredConstructor(String.class, String.class, char[].class, String.class, String.class);
            constructor.setAccessible(true);

            Object instance =
                    constructor.newInstance("/path/to/truststore", "hash456", "pass".toCharArray(), "JKS", null);

            Method getCertificateAlias = clazz.getDeclaredMethod("getCertificateAlias");
            Object aliasResult = getCertificateAlias.invoke(instance);
            assertThat(aliasResult).isNotNull();
            assertThat(((Optional<?>) aliasResult).isPresent()).isFalse();
        }
    }

    @Nested
    class BlockingRejectedExecutionHandlerTests {

        @Test
        void rejectedExecutionBlocksWhenNotShutdown() throws Exception {
            Class<?> clazz = Class.forName("io.prometheus.jmx.common.util.BlockingRejectedExecutionHandler");
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object handler = constructor.newInstance();

            ThreadPoolExecutor executor =
                    new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(true));

            Method rejectedExecution =
                    clazz.getDeclaredMethod("rejectedExecution", Runnable.class, ThreadPoolExecutor.class);

            Runnable task = () -> {};

            executor.shutdownNow();

            rejectedExecution.invoke(handler, task, executor);
        }
    }

    @Nested
    class PrivateConstructorTest {

        @Test
        void privateConstructorCannotBeInstantiated() throws Exception {
            Constructor<?> constructor = HTTPServerFactory.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            assertThat(instance).isNotNull();
        }
    }

    @Nested
    class KeyStoreIdentityMaterialTests {

        @Test
        void constructorAcceptsKeyStoreProperties() throws Exception {
            Class<?> keyStorePropertiesClass =
                    Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreProperties");
            Constructor<?> keyStoreConstructor = keyStorePropertiesClass.getDeclaredConstructor(
                    String.class, String.class, char[].class, String.class, String.class);
            keyStoreConstructor.setAccessible(true);
            Object keyStoreProps =
                    keyStoreConstructor.newInstance("/path/to/keystore", "hash", "pass".toCharArray(), "JKS", "alias");

            Class<?> identityMaterialClass =
                    Class.forName("io.prometheus.jmx.common.HTTPServerFactory$KeyStoreIdentityMaterial");
            Constructor<?> identityMaterialConstructor =
                    identityMaterialClass.getDeclaredConstructor(keyStorePropertiesClass);
            identityMaterialConstructor.setAccessible(true);
            Object identityMaterial = identityMaterialConstructor.newInstance(keyStoreProps);

            assertThat(identityMaterial).isNotNull();
            assertThat(identityMaterial).isInstanceOf(IdentityMaterial.class);
        }
    }

    @Nested
    class SslReloadStateTests {

        @Test
        void constructorAcceptsIdentityMaterialAndTrustStoreProperties() throws Exception {
            Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
            Constructor<?> constructor =
                    sslReloadStateClass.getDeclaredConstructor(IdentityMaterial.class, Optional.class);
            constructor.setAccessible(true);

            Object reloadState = constructor.newInstance(null, Optional.empty());

            Field identityMaterialField = sslReloadStateClass.getDeclaredField("identityMaterial");
            identityMaterialField.setAccessible(true);
            assertThat(identityMaterialField.get(reloadState)).isNull();

            Field trustStorePropertiesField = sslReloadStateClass.getDeclaredField("trustStoreProperties");
            trustStorePropertiesField.setAccessible(true);
            assertThat((Optional<?>) trustStorePropertiesField.get(reloadState)).isEmpty();
        }

        @Test
        void lockFieldIsPresent() throws Exception {
            Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
            Field lockField = sslReloadStateClass.getDeclaredField("lock");
            lockField.setAccessible(true);

            assertThat(lockField.getType()).isEqualTo(Object.class);
        }
    }

    @Nested
    class ConfiguredSslFactoryTests {

        @Test
        void constructorAcceptsAllFields() throws Exception {
            Class<?> sslReloadStateClass = Class.forName("io.prometheus.jmx.common.HTTPServerFactory$SslReloadState");
            Constructor<?> reloadStateConstructor =
                    sslReloadStateClass.getDeclaredConstructor(IdentityMaterial.class, Optional.class);
            reloadStateConstructor.setAccessible(true);
            Object reloadState = reloadStateConstructor.newInstance(null, Optional.empty());

            Map<Object, Object> config = new HashMap<>();
            config.put("httpServer", new HashMap<>());
            MapAccessor rootMapAccessor = MapAccessor.of(config);

            Class<?> configuredSslFactoryClass =
                    Class.forName("io.prometheus.jmx.common.HTTPServerFactory$ConfiguredSslFactory");
            Constructor<?> configuredConstructor = configuredSslFactoryClass.getDeclaredConstructor(
                    SSLFactory.class, sslReloadStateClass, MapAccessor.class);
            configuredConstructor.setAccessible(true);

            Object configured = configuredConstructor.newInstance(null, reloadState, rootMapAccessor);

            Field sslFactoryField = configuredSslFactoryClass.getDeclaredField("sslFactory");
            sslFactoryField.setAccessible(true);
            assertThat(sslFactoryField.get(configured)).isNull();

            Field reloadStateField = configuredSslFactoryClass.getDeclaredField("reloadState");
            reloadStateField.setAccessible(true);
            assertThat(reloadStateField.get(configured)).isSameAs(reloadState);

            Field rootMapAccessorField = configuredSslFactoryClass.getDeclaredField("rootMapAccessor");
            rootMapAccessorField.setAccessible(true);
            assertThat(rootMapAccessorField.get(configured)).isSameAs(rootMapAccessor);
        }
    }
}
