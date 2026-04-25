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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
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

        Map<Object, Object> keyStoreConfig = new HashMap<Object, Object>();
        keyStoreConfig.put("filename", keyStore.toString());
        keyStoreConfig.put("password", "secret");
        keyStoreConfig.put("type", "JKS");

        Map<Object, Object> certificateConfig = new HashMap<Object, Object>();
        certificateConfig.put("alias", "localhost");

        Map<Object, Object> sslConfig = new HashMap<Object, Object>();
        sslConfig.put("keyStore", keyStoreConfig);
        sslConfig.put("certificate", certificateConfig);

        Map<Object, Object> httpServerConfig = new HashMap<Object, Object>();
        httpServerConfig.put("ssl", sslConfig);

        Map<Object, Object> config = new HashMap<Object, Object>();
        config.put("httpServer", httpServerConfig);

        MapAccessor rootMapAccessor = MapAccessor.of(config);

        Object keyStoreProperties =
                invokePrivateMethod("getKeyStoreProperties", new Class<?>[] {MapAccessor.class}, rootMapAccessor);
        Field keyStorePropertiesField = HTTPServerFactory.class.getDeclaredField("keyStoreProperties");
        keyStorePropertiesField.setAccessible(true);
        keyStorePropertiesField.set(null, keyStoreProperties);

        Files.delete(keyStore);

        invokePrivateMethod("reloadSsl", new Class<?>[] {SSLFactory.class, MapAccessor.class}, null, rootMapAccessor);

        assertThat(keyStorePropertiesField.get(null)).isSameAs(keyStoreProperties);
    }

    private static Object invokePrivateMethod(String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = HTTPServerFactory.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static void write(Path path, String value) throws Exception {
        Files.write(path, value.getBytes("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
