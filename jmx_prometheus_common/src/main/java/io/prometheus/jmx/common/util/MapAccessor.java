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
import java.util.function.Function;

/**
 * Immutable accessor for nested map structures using path-based syntax.
 *
 * <p>Provides a type-safe way to traverse and access values in nested {@link Map} structures
 * using a Unix-style path syntax (e.g., {@code /root/child/key}).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Map<Object, Object> server = new java.util.LinkedHashMap<Object, Object>();
 * server.put("port", 8080);
 * server.put("host", "localhost");
 * Map<Object, Object> config = new java.util.LinkedHashMap<Object, Object>();
 * config.put("server", server);
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
    public Optional<Object> getPath(String path) {
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
     * Gets the value at the specified path.
     *
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @return an {@link Optional} containing the value, or an empty {@link Optional}
     * @deprecated use {@link #getPath(String)} instead
     */
    @Deprecated
    public Optional<Object> get(String path) {
        return getPath(path);
    }

    /**
     * Gets the value at the specified path and maps it to the requested type.
     *
     * <p>Returns an empty {@link Optional} if the path does not exist, the value is {@code null},
     * or the value is not an instance of {@code type}.
     *
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @param type the target type to cast to, must not be {@code null}
     * @param <T> the target type
     * @return an {@link Optional} containing the typed value, or an empty {@link Optional}
     * @throws IllegalArgumentException if {@code path} is {@code null}, blank, or malformed
     */
    public <T> Optional<T> getPath(String path, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        return getPath(path).filter(type::isInstance).map(type::cast);
    }

    /**
     * Gets the value at the specified path and maps it to the requested type.
     *
     * @param <T> the target type
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @param type the target type to cast to, must not be {@code null}
     * @return an {@link Optional} containing the typed value, or an empty {@link Optional}
     * @deprecated use {@link #getPath(String, Class)} instead
     */
    @Deprecated
    public <T> Optional<T> get(String path, Class<T> type) {
        return getPath(path, type);
    }

    /**
     * Gets the value at the specified path and maps it with the provided function.
     *
     * <p>Returns an empty {@link Optional} if the path does not exist or the value is {@code
     * null}. The mapper is applied only when a non-null value exists at the path.
     *
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @param mapper mapping function to convert the value, must not be {@code null}
     * @param <T> the mapped type
     * @return an {@link Optional} containing the mapped value, or an empty {@link Optional}
     * @throws IllegalArgumentException if {@code path} is {@code null}, blank, or malformed
     */
    public <T> Optional<T> getPath(String path, Function<Object, ? extends T> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper is null");
        }

        return getPath(path).map(mapper);
    }

    /**
     * Gets the value at the specified path and maps it with the provided function.
     *
     * @param <T> the mapped type
     * @param path the path to get, must start with {@code /}, must not be {@code null} or blank
     * @param mapper mapping function to convert the value, must not be {@code null}
     * @return an {@link Optional} containing the mapped value, or an empty {@link Optional}
     * @deprecated use {@link #getPath(String, Function)} instead
     */
    @Deprecated
    public <T> Optional<T> get(String path, Function<Object, ? extends T> mapper) {
        return getPath(path, mapper);
    }

    /**
     * Checks whether a path exists and contains a non-null value compatible with {@code type}.
     *
     * @param path the path to check, must start with {@code /}, must not be {@code null} or blank
     * @param type the expected type, must not be {@code null}
     * @param <T> the expected type
     * @return {@code true} if the path exists and the value is a non-null instance of {@code
     *     type}, otherwise {@code false}
     * @throws IllegalArgumentException if {@code path} is {@code null}, blank, or malformed
     */
    public <T> boolean containsPath(String path, Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type is null");
        }

        return getPath(path, type).isPresent();
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
