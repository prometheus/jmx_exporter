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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable accessor for nested map structures using path-based syntax.
 *
 * <p>Provides a type-safe way to traverse and access values in nested {@link Map} structures
 * using a Unix-style path syntax (e.g., {@code /root/child/key}).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Map<Object, Object> config = Map.of(
 *     "server", Map.of(
 *         "port", 8080,
 *         "host", "localhost"
 *     )
 * );
 * MapAccessor accessor = MapAccessor.of(config);
 * Optional<Object> port = accessor.get("/server/port");  // Returns Optional[8080]
 * boolean hasHost = accessor.containsPath("/server/host");  // Returns true
 * }</pre>
 *
 * <p>This class is immutable and thread-safe. All operations return new instances or
 * immutable views.
 */
@SuppressWarnings("unchecked")
public class MapAccessor {

    /**
     * The underlying map, stored as an unmodifiable map.
     */
    private final Map<Object, Object> map;

    /**
     * Constructs a MapAccessor wrapping the given map.
     *
     * <p>The map is wrapped in an unmodifiable view to ensure immutability.
     *
     * @param map the map to wrap, must not be {@code null}
     * @throws IllegalArgumentException if {@code map} is {@code null}
     */
    private MapAccessor(Map<Object, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }

        this.map = (Map<Object, Object>) createUnmodifiable(map);
    }

    /**
     * Checks if a path exists in the map.
     *
     * <p>A path exists if all intermediate keys are present in the nested map structure.
     * A path can exist even if the value at that path is {@code null}.
     *
     * @param path the path to check, must start with {@code /}, must not be {@code null} or blank
     * @return {@code true} if the path exists, {@code false} otherwise
     * @throws IllegalArgumentException if {@code path} is {@code null}, blank, or malformed
     */
    public boolean containsPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        path = validatePath(path);
        if ("/".equals(path)) {
            return true;
        }

        String[] tokens = path.split("/");
        Object current = map;

        for (int i = 1; i < tokens.length; i++) {
            if (!(current instanceof Map)) {
                return false;
            }

            Map<?, ?> currentMap = (Map<?, ?>) current;

            if (!currentMap.containsKey(tokens[i])) {
                return false;
            }

            current = currentMap.get(tokens[i]);
        }

        return true;
    }

    /**
     * Gets the value at the specified path.
     *
     * <p>Returns an {@link Optional} containing the value if the path exists and the value is
     * non-null, or an empty {@link Optional} if the path does not exist or the value is
     * {@code null}.
     *
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @return an {@link Optional} containing the value, or an empty {@link Optional}
     * @throws IllegalArgumentException if {@code path} is {@code null}, blank, or malformed
     */
    public Optional<Object> get(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        if (!containsPath(path)) {
            return Optional.empty();
        }

        path = validatePath(path);
        if ("/".equals(path)) {
            return Optional.of(map);
        }

        String[] tokens = path.split("/");
        Object current = map;

        for (int i = 1; i < tokens.length; i++) {
            Map<?, ?> currentMap = (Map<?, ?>) current;

            if (!currentMap.containsKey(tokens[i])) {
                return Optional.empty();
            }

            current = currentMap.get(tokens[i]);
        }

        return Optional.ofNullable(current);
    }

    /**
     * Creates a MapAccessor from a map.
     *
     * <p>The map is wrapped in an unmodifiable view to ensure immutability.
     *
     * @param map the map to wrap, must not be {@code null}
     * @return a new MapAccessor instance
     * @throws IllegalArgumentException if {@code map} is {@code null}
     */
    public static MapAccessor of(Map<Object, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }

        return new MapAccessor(map);
    }

    /**
     * Validates and normalizes a path string.
     *
     * <p>A valid path must:
     *
     * <ul>
     *   <li>Start with {@code /}
     *   <li>Not end with {@code /} (except for the root path)
     *   <li>Not contain empty segments (e.g., {@code //})
     *   <li>Not contain whitespace-only segments
     * </ul>
     *
     * @param path the path to validate, must not be {@code null}
     * @return the validated path
     * @throws IllegalArgumentException if the path is invalid
     */
    private String validatePath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }

        if (path.equals("/")) {
            return path;
        }

        path = path.trim();

        if (path.isEmpty()) {
            throw new IllegalArgumentException("path is empty");
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        if (path.endsWith("/")) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        if (path.contains("//")) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        if (path.matches(".*/(\\s*)/.*")) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        return path;
    }

    /**
     * Creates an unmodifiable deep copy of the given value.
     *
     * <p>Recursively converts maps, lists, sets, and collections to their unmodifiable
     * counterparts. Other values are returned unchanged.
     *
     * @param value the value to make unmodifiable
     * @return an unmodifiable version of the value
     */
    private Object createUnmodifiable(Object value) {
        if (value instanceof Map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(entry.getKey(), createUnmodifiable(entry.getValue()));
            }

            return Collections.unmodifiableMap(result);
        } else if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                result.add(createUnmodifiable(item));
            }

            return Collections.unmodifiableList(result);
        } else if (value instanceof Set) {
            Set<Object> result = new LinkedHashSet<>();
            for (Object item : (Set<?>) value) {
                result.add(createUnmodifiable(item));
            }

            return Collections.unmodifiableSet(result);
        } else if (value instanceof Collection) {
            Collection<Object> result = new ArrayList<>();
            for (Object item : (Collection<?>) value) {
                result.add(createUnmodifiable(item));
            }

            return Collections.unmodifiableCollection(result);
        } else {
            return value;
        }
    }
}
