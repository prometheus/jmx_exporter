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

package io.prometheus.jmx.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class MapAccessorTest {

    private MapAccessor mapAccessor;

    @BeforeEach
    public void setUp() throws IOException {
        mapAccessor =
                MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));
    }

    @Test
    public void testContainsPath() {
        assertThat(mapAccessor.containsPath("/")).isTrue();

        assertThat(mapAccessor.containsPath("/key")).isTrue();
        assertThat(mapAccessor.containsPath("/key/subkey")).isTrue();
        assertThat(mapAccessor.get("/key/subkey")).isNotPresent();

        assertThat(mapAccessor.containsPath("/key2")).isTrue();
        assertThat(mapAccessor.containsPath("/key2/subkey2")).isTrue();
        assertThat(mapAccessor.get("/key2/subkey2")).isPresent();

        assertThat(mapAccessor.containsPath("/key/foo")).isFalse();
    }

    @Test
    public void testGet() {
        Optional<Object> optional = mapAccessor.get("/");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/httpServer");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/httpServer/authentication");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/httpServer/authentication/basic");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/httpServer/authentication/basic/username");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value)
                                    .isNotNull()
                                    .isInstanceOf(String.class)
                                    .isEqualTo("Prometheus");
                        });

        optional = mapAccessor.get("/httpServer/authentication/basic/password");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value)
                                    .isNotNull()
                                    .isInstanceOf(String.class)
                                    .isEqualTo(
                                            "c6d52fc2733af33e62b45d4525261e35e04f7b0ec227e4feee8fd3fe1401a2a9");
                        });

        optional = mapAccessor.get("/httpServer/threads");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/httpServer/threads/minimum");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Integer.class).isEqualTo(1);
                        });

        optional = mapAccessor.get("/httpServer/threads/maximum");
        assertThat(optional)
                .isNotNull()
                .isPresent()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Integer.class).isEqualTo(10);
                        });

        optional = mapAccessor.get("/httpServer/threads/keepAlive");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value)
                                    .isNotNull()
                                    .isInstanceOf(Integer.class)
                                    .isEqualTo(120);
                        });

        optional = mapAccessor.get("/key");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/key/subkey");
        assertThat(optional).isNotNull().isNotPresent();

        optional = mapAccessor.get("/key2");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull();
                        });

        assertThat(mapAccessor.containsPath("/key2/subkey2")).isTrue();
        optional = mapAccessor.get("/key2/subkey2");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(Map.class);
                        });

        optional = mapAccessor.get("/key2/subkey2/foo");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isNotNull().isInstanceOf(String.class);
                        });

        optional = mapAccessor.get("/key2/subkey2/foo/bar");
        assertThat(optional).isNotNull().isNotPresent();

        optional = mapAccessor.get("/key3");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isInstanceOf(String.class);
                        });

        assertThat(mapAccessor.containsPath("/key4")).isTrue();
        optional = mapAccessor.get("/key4");
        assertThat(optional).isNotNull().isNotPresent();

        assertThat(mapAccessor.containsPath("/key5")).isFalse();
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
            String finalPath = path;
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.get(finalPath))
                    .withMessageContaining("path");
        }
    }

    @Test
    public void testEmpty() {
        MapAccessor mapAccessor = MapAccessor.of(new LinkedHashMap<>());

        Optional<Object> optional = mapAccessor.get("/");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isInstanceOf(Map.class);
                            Map<Object, Object> map = (Map<Object, Object>) value;
                            assertThat(map).isEmpty();
                        });

        optional = mapAccessor.get("/foo");
        assertThat(optional).isNotNull().isNotPresent();

        optional = mapAccessor.get("/foo/value");
        assertThat(optional).isNotNull().isNotPresent();
    }

    @Test
    public void testUnmodifiable() {
        Optional<Object> optional = mapAccessor.get("/");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isInstanceOf(Map.class);
                        });

        Map<Object, Object> map = (Map<Object, Object>) optional.get();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> map.put("abc", "123"));

        optional = mapAccessor.get("/key");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isInstanceOf(Map.class);
                        });

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> map.put("abc", "123"));

        List<String> list = new ArrayList<>();
        list.add("abc");

        Map<Object, Object> map2 = new LinkedHashMap<>();
        map2.put("list", list);

        MapAccessor mapAccessor2 = MapAccessor.of(map2);
        optional = mapAccessor2.get("/list");
        assertThat(optional)
                .isNotNull()
                .hasValueSatisfying(
                        value -> {
                            assertThat(value).isInstanceOf(List.class);
                        });

        List<String> list2 = (List<String>) optional.get();
        assertThat(list2).isNotEmpty();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> list2.add("123"));
    }
}
