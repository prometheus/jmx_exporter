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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class MapAccessorTest {

    private MapAccessor mapAccessor;

    @BeforeEach
    public void setUp() throws IOException {
        mapAccessor = MapAccessor.of(YamlSupport.loadYaml(ResourceSupport.load("/MapAccessorTest.yaml")));
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
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer/authentication");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer/authentication/basic");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer/authentication/basic/username");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(String.class).isEqualTo("Prometheus");
        });

        optional = mapAccessor.get("/httpServer/authentication/basic/password");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value)
                    .isNotNull()
                    .isInstanceOf(String.class)
                    .isEqualTo("c6d52fc2733af33e62b45d4525261e35e04f7b0ec227e4feee8fd3fe1401a2a9");
        });

        optional = mapAccessor.get("/httpServer/metrics");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer/metrics/path");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(String.class).isEqualTo("/custom/metrics");
        });

        optional = mapAccessor.get("/httpServer/threads");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/httpServer/threads/minimum");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Integer.class).isEqualTo(1);
        });

        optional = mapAccessor.get("/httpServer/threads/maximum");
        assertThat(optional).isNotNull().isPresent().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Integer.class).isEqualTo(10);
        });

        optional = mapAccessor.get("/httpServer/threads/keepAlive");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Integer.class).isEqualTo(120);
        });

        optional = mapAccessor.get("/key");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/key/subkey");
        assertThat(optional).isNotNull().isNotPresent();

        optional = mapAccessor.get("/key2");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull();
        });

        assertThat(mapAccessor.containsPath("/key2/subkey2")).isTrue();
        optional = mapAccessor.get("/key2/subkey2");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(Map.class);
        });

        optional = mapAccessor.get("/key2/subkey2/foo");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isNotNull().isInstanceOf(String.class);
        });

        optional = mapAccessor.get("/key2/subkey2/foo/bar");
        assertThat(optional).isNotNull().isNotPresent();

        optional = mapAccessor.get("/key3");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isInstanceOf(String.class);
        });

        assertThat(mapAccessor.containsPath("/key4")).isTrue();
        optional = mapAccessor.get("/key4");
        assertThat(optional).isNotNull().isNotPresent();

        assertThat(mapAccessor.containsPath("/key5")).isFalse();
    }

    @Test
    public void testInvalidPaths() {
        String[] paths = new String[] {
            null, "", " ", "foo", "//", "/ /", " /", "/ ", "/foo/", "/ foo/", "/ foo / ", "/foo /", "/foo/ /",
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
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
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
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isInstanceOf(Map.class);
        });

        Map<Object, Object> map = (Map<Object, Object>) optional.get();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("abc", "123"));

        optional = mapAccessor.get("/key");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isInstanceOf(Map.class);
        });

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> map.put("abc", "123"));

        List<String> list = new ArrayList<>();
        list.add("abc");

        Map<Object, Object> map2 = new LinkedHashMap<>();
        map2.put("list", list);

        MapAccessor mapAccessor2 = MapAccessor.of(map2);
        optional = mapAccessor2.get("/list");
        assertThat(optional).isNotNull().hasValueSatisfying(value -> {
            assertThat(value).isInstanceOf(List.class);
        });

        List<String> list2 = (List<String>) optional.get();
        assertThat(list2).isNotEmpty();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list2.add("123"));
    }

    @Nested
    class OfNullTests {

        @Test
        public void ofNullMapThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> MapAccessor.of(null))
                    .withMessageContaining("map is null");
        }
    }

    @Nested
    class ContainsPathNullAndBlankTests {

        @Test
        public void containsPathNullThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.containsPath(null))
                    .withMessageContaining("path");
        }

        @Test
        public void containsPathBlankThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.containsPath(""))
                    .withMessageContaining("path");
        }

        @Test
        public void containsPathWhitespaceThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.containsPath("   "))
                    .withMessageContaining("path");
        }
    }

    @Nested
    class ContainsPathThroughNonMapValueTests {

        @Test
        public void containsPathReturnsFalseWhenTraversingNonMapValue() {
            assertThat(mapAccessor.containsPath("/key3/foo")).isFalse();
        }

        @Test
        public void getReturnsEmptyWhenTraversingNonMapIntermediate() {
            assertThat(mapAccessor.get("/key3/foo")).isNotPresent();
        }
    }

    @Nested
    class ValidatePathWhitespaceSegmentTests {

        @Test
        public void pathWithWhitespaceOnlySegmentThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.get("/foo/ /bar"))
                    .withMessageContaining("path");
        }

        @Test
        public void containsPathWithWhitespaceOnlySegmentThrows() {
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> mapAccessor.containsPath("/foo/ /bar"))
                    .withMessageContaining("path");
        }
    }

    @Nested
    class CreateUnmodifiableSetTests {

        @Test
        public void setValuesAreMadeUnmodifiable() {
            Set<String> set = new LinkedHashSet<>();
            set.add("alpha");
            set.add("beta");

            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("mySet", set);

            MapAccessor accessor = MapAccessor.of(map);
            Optional<Object> optional = accessor.get("/mySet");

            assertThat(optional).isPresent();
            assertThat(optional.get()).isInstanceOf(Set.class);

            Set<String> result = (Set<String>) optional.get();
            assertThat(result).containsExactly("alpha", "beta");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("gamma"));
        }
    }

    @Nested
    class CreateUnmodifiableCollectionTests {

        @Test
        public void collectionValuesAreMadeUnmodifiable() {
            Collection<String> collection = new ArrayDeque<>();
            collection.add("x");
            collection.add("y");

            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("myCollection", collection);

            MapAccessor accessor = MapAccessor.of(map);
            Optional<Object> optional = accessor.get("/myCollection");

            assertThat(optional).isPresent();
            assertThat(optional.get()).isInstanceOf(Collection.class);

            Collection<String> result = (Collection<String>) optional.get();
            assertThat(result).containsExactly("x", "y");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("z"));
        }
    }

    @Nested
    class CreateUnmodifiableNestedTests {

        @Test
        public void nestedSetInsideMapIsUnmodifiable() {
            Set<String> innerSet = new LinkedHashSet<>();
            innerSet.add("item1");

            Map<Object, Object> inner = new LinkedHashMap<>();
            inner.put("set", innerSet);

            Map<Object, Object> root = new LinkedHashMap<>();
            root.put("nested", inner);

            MapAccessor accessor = MapAccessor.of(root);
            Optional<Object> optional = accessor.get("/nested/set");

            assertThat(optional).isPresent();
            Set<String> result = (Set<String>) optional.get();
            assertThat(result).containsExactly("item1");

            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("item2"));
        }

        @Test
        public void nestedCollectionInsideListIsUnmodifiable() {
            Collection<String> col = new ArrayDeque<>();
            col.add("a");

            List<Object> list = new ArrayList<>();
            list.add(col);

            Map<Object, Object> root = new LinkedHashMap<>();
            root.put("data", list);

            MapAccessor accessor = MapAccessor.of(root);
            Optional<Object> optional = accessor.get("/data");

            assertThat(optional).isPresent();
            List<Object> result = (List<Object>) optional.get();
            assertThat(result).hasSize(1);

            Collection<String> innerCol = (Collection<String>) result.get(0);
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> innerCol.add("b"));
        }
    }

    @Nested
    class CreateUnmodifiableEmptyCollectionTests {

        @Test
        public void emptySetIsMadeUnmodifiable() {
            Set<String> emptySet = new LinkedHashSet<>();

            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("emptySet", emptySet);

            MapAccessor accessor = MapAccessor.of(map);
            Optional<Object> optional = accessor.get("/emptySet");

            assertThat(optional).isPresent();
            Set<String> result = (Set<String>) optional.get();
            assertThat(result).isEmpty();
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("x"));
        }

        @Test
        public void emptyCollectionIsMadeUnmodifiable() {
            Collection<String> emptyCol = new ArrayDeque<>();

            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("emptyCol", emptyCol);

            MapAccessor accessor = MapAccessor.of(map);
            Optional<Object> optional = accessor.get("/emptyCol");

            assertThat(optional).isPresent();
            Collection<String> result = (Collection<String>) optional.get();
            assertThat(result).isEmpty();
            assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("x"));
        }
    }

    @Nested
    class ContainsPathEdgeCaseTests {

        @Test
        public void containsPathWithRootPathReturnsTrue() {
            MapAccessor accessor = MapAccessor.of(new LinkedHashMap<>());
            assertThat(accessor.containsPath("/")).isTrue();
        }

        @Test
        public void getWithMissingIntermediateKeyReturnsEmpty() {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("key1", new LinkedHashMap<>());

            MapAccessor accessor = MapAccessor.of(map);
            Optional<Object> result = accessor.get("/key1/missing/deep");
            assertThat(result).isNotPresent();
        }

        @Test
        public void getWithValueNullKey() {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("key", null);

            MapAccessor accessor = MapAccessor.of(map);
            assertThat(accessor.containsPath("/key")).isTrue();
            assertThat(accessor.get("/key")).isNotPresent();
        }
    }

    @Nested
    class TypedAccessorTests {

        @Test
        public void getWithClassReturnsValueWhenTypeMatches() {
            Optional<String> value = mapAccessor.get("/httpServer/authentication/basic/username", String.class);
            assertThat(value).hasValue("Prometheus");
        }

        @Test
        public void getWithClassReturnsEmptyWhenTypeMismatches() {
            Optional<Integer> value = mapAccessor.get("/httpServer/authentication/basic/username", Integer.class);
            assertThat(value).isEmpty();
        }

        @Test
        public void getWithClassReturnsEmptyWhenPathMissing() {
            Optional<String> value = mapAccessor.get("/does/not/exist", String.class);
            assertThat(value).isEmpty();
        }

        @Test
        public void getWithClassReturnsEmptyWhenValueNull() {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("key", null);
            MapAccessor accessor = MapAccessor.of(map);
            Optional<String> value = accessor.get("/key", String.class);
            assertThat(value).isEmpty();
        }

        @Test
        public void getWithMapperMapsValue() {
            Optional<Integer> value = mapAccessor.get("/httpServer/threads/minimum", input -> ((Integer) input) + 1);
            assertThat(value).hasValue(2);
        }

        @Test
        public void getWithMapperReturnsEmptyWhenPathMissing() {
            Optional<String> value = mapAccessor.get("/does/not/exist", Object::toString);
            assertThat(value).isEmpty();
        }

        @Test
        public void getWithMapperReturnsEmptyWhenValueNull() {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("key", null);
            MapAccessor accessor = MapAccessor.of(map);
            Optional<String> value = accessor.get("/key", Object::toString);
            assertThat(value).isEmpty();
        }

        @Test
        public void containsPathWithClassReturnsTrueForMatchingType() {
            assertThat(mapAccessor.containsPath("/httpServer/authentication/basic/username", String.class))
                    .isTrue();
        }

        @Test
        public void containsPathWithClassReturnsFalseForMismatchingType() {
            assertThat(mapAccessor.containsPath("/httpServer/authentication/basic/username", Integer.class))
                    .isFalse();
        }

        @Test
        public void containsPathWithClassReturnsFalseForNullValue() {
            Map<Object, Object> map = new LinkedHashMap<>();
            map.put("key", null);
            MapAccessor accessor = MapAccessor.of(map);
            assertThat(accessor.containsPath("/key", String.class)).isFalse();
        }
    }
}
