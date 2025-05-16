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

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class MapAccessorTest {

    private MapAccessor mapAccessor;

    @Before
    public void setUp() throws IOException {
        mapAccessor =
                MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));
    }

    @Test
    public void testContainsPath() {
        assertTrue(mapAccessor.containsPath("/"));

        assertTrue(mapAccessor.containsPath("/key"));
        assertTrue(mapAccessor.containsPath("/key/subkey"));
        assertFalse(mapAccessor.get("/key/subkey").isPresent());

        assertTrue(mapAccessor.containsPath("/key2"));
        assertTrue(mapAccessor.containsPath("/key2/subkey2"));
        assertTrue(mapAccessor.get("/key2/subkey2").isPresent());

        assertFalse(mapAccessor.containsPath("/key/foo"));
    }

    @Test
    public void testGet() {
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

        optional = mapAccessor.get("/key");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());
        assertTrue(optional.get() instanceof Map);

        optional = mapAccessor.get("/key/subkey");
        assertNotNull(optional);
        assertFalse(optional.isPresent());

        optional = mapAccessor.get("/key2");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertNotNull(optional.get());

        assertTrue(mapAccessor.containsPath("/key2/subkey2"));
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

        optional = mapAccessor.get("/key3");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof String);

        assertTrue(mapAccessor.containsPath("/key4"));
        optional = mapAccessor.get("/key4");
        assertNotNull(optional);
        assertFalse(optional.isPresent());

        assertFalse(mapAccessor.containsPath("/key5"));
    }

    @Test
    public void testInvalidPaths() {
        String[] paths =
                new String[] {
                    null,
                    "",
                    " ",
                    "foo",
                    "//",
                    "/ /",
                    " /",
                    "/ ",
                    "/foo/",
                    "/ foo/",
                    "/ foo / ",
                    "/foo /",
                    "/foo/ /",
                };

        for (String path : paths) {
            try {
                mapAccessor.get(path);
                fail(format("Expected IllegalArgumentException for path [%s]", path));
            } catch (IllegalArgumentException e) {
                // INTENTIONALLY BLANK
            }
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
    public void testUnmodifiable() {
        Optional<Object> optional = mapAccessor.get("/");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);

        Map<Object, Object> map = (Map<Object, Object>) optional.get();

        try {
            map.put("abc", "123");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // INTENTIONALLY BLANK
        }

        optional = mapAccessor.get("/key");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof Map);

        try {
            map.put("abc", "123");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // INTENTIONALLY BLANK
        }

        List<String> list = new ArrayList<>();
        list.add("abc");

        Map<Object, Object> map2 = new LinkedHashMap<>();
        map2.put("list", list);

        MapAccessor mapAccessor2 = MapAccessor.of(map2);
        optional = mapAccessor2.get("/list");
        assertNotNull(optional);
        assertTrue(optional.isPresent());
        assertTrue(optional.get() instanceof List);

        List<String> list2 = (List<String>) optional.get();
        assertFalse(list2.isEmpty());
        try {
            list2.add("123");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // INTENTIONALLY BLANK
        }
    }
}
