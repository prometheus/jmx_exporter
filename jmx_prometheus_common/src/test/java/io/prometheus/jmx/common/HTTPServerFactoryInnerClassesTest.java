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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
}
