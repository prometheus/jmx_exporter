/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class MapAccessorTest {

    @Test
    public void testValidPaths() throws IOException {
        MapAccessor mapAccessor =
                MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));

        Optional<Object> optional = mapAccessor.get("/");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/httpServer");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/httpServer/authentication");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/httpServer/authentication/basic");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/httpServer/authentication/basic/username");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof String);
        assertEquals("Prometheus", optional.get());

        optional = mapAccessor.get("/httpServer/authentication/basic/password");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof String);
        assertEquals(
                "c6d52fc2733af33e62b45d4525261e35e04f7b0ec227e4feee8fd3fe1401a2a9", optional.get());

        optional = mapAccessor.get("/httpServer/threads");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/httpServer/threads/minimum");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(1, ((Integer) optional.get()).intValue());

        optional = mapAccessor.get("/httpServer/threads/maximum");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(10, ((Integer) optional.get()).intValue());

        optional = mapAccessor.get("/httpServer/threads/keepAlive");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Integer);
        assertEquals(120, ((Integer) optional.get()).intValue());

        /*

        key:
          subkey:
        key2:
          subkey2:
            foo: bar
        key3: bar

         */

        optional = mapAccessor.get("/key");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/key/subkey");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/key2");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());

        optional = mapAccessor.get("/key2/subkey2");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/key2/subkey2/foo");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof String);

        optional = mapAccessor.get("/key2/subkey2/foo/bar");
        assertNotNull(optional);
        assertFalse(optional.isPresent());
    }

    @Test
    public void testInvalidPaths() throws IOException {
        MapAccessor mapAccessor =
                MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));

        try {
            mapAccessor.get("");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // INTENTIONALLY BLANK
        }

        try {
            mapAccessor.get("//");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // INTENTIONALLY BLANK
        }

        try {
            mapAccessor.get("foo");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // INTENTIONALLY BLANK
        }

        try {
            mapAccessor.get("/foo/");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // INTENTIONALLY BLANK
        }
    }

    @Test
    public void testEmpty() {
        MapAccessor mapAccessor = MapAccessor.of(new LinkedHashMap<>());
        Optional<Object> optional = mapAccessor.get("/");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);
        Map<Object, Object> map = (Map<Object, Object>) optional.get();
        assertTrue(map.isEmpty());

        optional = mapAccessor.get("/foo");
        assertNotNull(optional);
        assertFalse(optional.isPresent());

        optional = mapAccessor.get("/foo/value");
        assertNotNull(optional);
        assertFalse(optional.isPresent());
    }

    @Test
    public void testContains() throws IOException {
        MapAccessor mapAccessor =
                MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));

        assertTrue(mapAccessor.contains("/"));

        assertTrue(mapAccessor.contains("/key"));
        assertTrue(mapAccessor.contains("/key/subkey"));
        assertTrue(mapAccessor.get("/key/subkey").isPresent());

        assertTrue(mapAccessor.contains("/key2"));
        assertTrue(mapAccessor.contains("/key2/subkey2"));
        assertTrue(mapAccessor.get("/key2/subkey2").isPresent());

        assertFalse(mapAccessor.contains("/key/foo"));
    }
}
