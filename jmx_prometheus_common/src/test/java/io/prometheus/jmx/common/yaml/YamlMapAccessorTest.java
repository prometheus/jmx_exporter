/*
 * Copyright (C) 2018-2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

@SuppressWarnings("unchecked")
public class YamlMapAccessorTest {

    @Test
    public void testValidPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        Optional<Object> optional = yamlMapAccessor.get("/");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = yamlMapAccessor.get("/httpServer");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = yamlMapAccessor.get("/httpServer/authentication");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = yamlMapAccessor.get("/httpServer/authentication/basic");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = yamlMapAccessor.get("/httpServer/authentication/basic/username");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof String);
        assertEquals("Prometheus", optional.get());

        optional = yamlMapAccessor.get("/httpServer/authentication/basic/password");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof String);
        assertEquals(
                "c6d52fc2733af33e62b45d4525261e35e04f7b0ec227e4feee8fd3fe1401a2a9", optional.get());

        optional = yamlMapAccessor.get("/httpServer/threads");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = yamlMapAccessor.get("/httpServer/threads/minimum");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());

        optional = yamlMapAccessor.get("/httpServer/threads/maximum");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(10, ((Integer) optional.get()).intValue());

        optional = yamlMapAccessor.get("/httpServer/threads/keepAlive");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(120, ((Integer) optional.get()).intValue());
    }

    @Test
    public void testInvalidPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        try {
            yamlMapAccessor.get("");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // DO NOTHING
        }

        try {
            yamlMapAccessor.get("//");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // DO NOTHING
        }

        try {
            yamlMapAccessor.get("foo");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // DO NOTHING
        }

        try {
            yamlMapAccessor.get("/foo/");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // DO NOTHING
        }
    }

    @Test
    public void testOtherPaths() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        Optional<Object> optional = yamlMapAccessor.get("/foo");
        assertNotNull(optional);
        assertFalse(optional.isPresent());

        optional = yamlMapAccessor.getOrCreate("/foo", () -> new LinkedHashMap<>());
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        try {
            Map<Object, Object> map = (Map<Object, Object>) optional.get();
            assertNotNull(map);
            assertEquals(0, map.size());
        } catch (ClassCastException e) {
            fail("Expected Map<Object, Object>");
        }

        optional = yamlMapAccessor.get("/foo");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        Map<Object, Object> map = (Map<Object, Object>) optional.get();
        map.put("value", 1);

        optional = yamlMapAccessor.get("/foo/value");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());

        try {
            yamlMapAccessor.getOrThrow(
                    "/foo/value2", () -> new IllegalArgumentException("path doesn't exist"));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("path doesn't exist", e.getMessage());
        }

        yamlMapAccessor = YamlMapAccessor.empty();

        optional = yamlMapAccessor.getOrCreate("/foo/bar/value", () -> 1);
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());

        optional = yamlMapAccessor.get("/foo/bar/value");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());
    }

    @Test
    public void testEmpty() {
        YamlMapAccessor yamlMapAccessor = YamlMapAccessor.empty();
        Optional<Object> optional = yamlMapAccessor.get("/");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);
        Map<Object, Object> map = (Map<Object, Object>) optional.get();
        assertTrue(map.isEmpty());

        optional = yamlMapAccessor.getOrCreate("/foo", () -> new LinkedHashMap<>());
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);
        map = (Map<Object, Object>) optional.get();
        map.put("value", 1);

        optional = yamlMapAccessor.get("/foo/value");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());

        optional = yamlMapAccessor.get("/foo");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);
        map = (Map<Object, Object>) optional.get();
        assertTrue(map.get("value") instanceof Integer);
        assertTrue(((Integer) map.get("value")) == 1);
    }

    @Test
    public void testContainsPath() throws IOException {
        YamlMapAccessor yamlMapAccessor = createYamlMapAccessor("/YamlMapAccessorTest.yaml");

        assertTrue(yamlMapAccessor.containsPath("/"));

        assertTrue(yamlMapAccessor.containsPath("/key"));
        assertTrue(yamlMapAccessor.containsPath("/key/subkey"));
        assertFalse(yamlMapAccessor.get("/key/subkey").isPresent());

        assertTrue(yamlMapAccessor.containsPath("/key2"));
        assertTrue(yamlMapAccessor.containsPath("/key2/subkey2"));
        assertTrue(yamlMapAccessor.get("/key2/subkey2").isPresent());

        assertFalse(yamlMapAccessor.containsPath("/key/foo"));
    }

    private static YamlMapAccessor createYamlMapAccessor(String resource) throws IOException {
        try (InputStream inputStream = YamlMapAccessorTest.class.getResourceAsStream(resource)) {
            Map<Object, Object> map = new Yaml().load(inputStream);
            return new YamlMapAccessor(map);
        }
    }
}
