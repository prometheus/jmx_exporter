/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Class to implement a MapAccessor to work with nested Maps / values */
@SuppressWarnings("unchecked")
public class YamlMapAccessor {

    private final Map<Object, Object> map;

    /**
     * Constructor
     *
     * @param map map
     */
    public YamlMapAccessor(Map<Object, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("Map is null");
        }

        this.map = map;
    }

    /**
     * Method to determine if a path exists
     *
     * @param path path
     * @return true if the path exists (but could be null), false otherwise
     */
    public boolean containsPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        path = validatePath(path);
        if (path.equals("/")) {
            return true;
        }

        String[] pathTokens = path.split(Pattern.quote("/"));
        Map<Object, Object> subMap = map;

        for (int i = 1; i < pathTokens.length; i++) {
            try {
                if (subMap.containsKey(pathTokens[i])) {
                    subMap = (Map<Object, Object>) subMap.get(pathTokens[i]);
                } else {
                    return false;
                }
            } catch (NullPointerException | ClassCastException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method to get a path Object
     *
     * @param path path
     * @return an Optional containing the path Object or an empty Optional if the path doesn't exist
     */
    public Optional<Object> get(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        path = validatePath(path);
        if (path.equals("/")) {
            return Optional.of(map);
        }

        String[] pathTokens = path.split(Pattern.quote("/"));
        Object object = map;

        for (int i = 1; i < pathTokens.length; i++) {
            try {
                object = resolve(pathTokens[i], object);
            } catch (NullPointerException | ClassCastException e) {
                return Optional.empty();
            }
        }

        return Optional.ofNullable(object);
    }

    /**
     * Method to get a path Object or create an Object using the Supplier
     *
     * <p>parent paths will be created if required
     *
     * @param path path
     * @param supplier supplier
     * @return an Optional containing the path Object or Optional created by the Supplier
     */
    public Optional<Object> getOrCreate(String path, Supplier<Object> supplier) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        path = validatePath(path);
        if (path.equals("/")) {
            return Optional.of(map);
        }

        if (supplier == null) {
            throw new IllegalArgumentException("supplier is null");
        }

        String[] pathTokens = path.split(Pattern.quote("/"));
        Object previous = map;
        Object current = null;

        for (int i = 1; i < pathTokens.length; i++) {
            try {
                current = resolve(pathTokens[i], previous);
                if (current == null) {
                    if ((i + 1) == pathTokens.length) {
                        Object object = supplier.get();
                        ((Map<String, Object>) previous).put(pathTokens[i], object);
                        return Optional.of(object);
                    } else {
                        current = new LinkedHashMap<>();
                        ((Map<String, Object>) previous).put(pathTokens[i], current);
                    }
                }
                previous = current;
            } catch (NullPointerException e) {
                return Optional.empty();
            } catch (ClassCastException e) {
                if ((i + 1) == pathTokens.length) {
                    throw new IllegalArgumentException(
                            String.format("path [%s] isn't a Map", flatten(pathTokens, 1, i)));
                }
                return Optional.empty();
            }
        }

        return Optional.ofNullable(current);
    }

    /**
     * Method to get a path Object, throwing an RuntimeException created by the Supplier if the path
     * doesn't exist
     *
     * @param path path
     * @param supplier supplier
     * @return an Optional containing the path Object
     */
    public Optional<Object> getOrThrow(String path, Supplier<? extends RuntimeException> supplier) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        if (supplier == null) {
            throw new IllegalArgumentException("supplier is null");
        }

        path = validatePath(path);
        if (path.equals("/")) {
            return Optional.of(map);
        }

        String[] pathTokens = path.split(Pattern.quote("/"));
        Object object = map;

        for (int i = 1; i < pathTokens.length; i++) {
            try {
                object = resolve(pathTokens[i], object);
            } catch (NullPointerException | ClassCastException e) {
                throw supplier.get();
            }

            if (object == null) {
                throw supplier.get();
            }
        }

        return Optional.of(object);
    }

    /**
     * Method to get a MapAccessor backed by an empty Map
     *
     * @return the return value
     */
    public static YamlMapAccessor empty() {
        return new YamlMapAccessor(new LinkedHashMap<>());
    }

    /**
     * Method to validate a path
     *
     * @param path path
     * @return the return value
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
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        if (path.endsWith("/")) {
            throw new IllegalArgumentException(String.format("path [%s] is invalid", path));
        }

        return path;
    }

    /**
     * Method to resolve a path token to an Object
     *
     * @param pathToken pathToken
     * @param object object
     * @return the return value
     * @param <T> the return type
     */
    private <T> T resolve(String pathToken, Object object) {
        return (T) ((Map<String, Object>) object).get(pathToken);
    }

    /**
     * Method to flatten an array of path tokens to a path
     *
     * @param pathTokens pathTokens
     * @param begin begin
     * @param end end
     * @return the return value
     */
    private String flatten(String[] pathTokens, int begin, int end) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = begin; i < end; i++) {
            stringBuilder.append("/").append(pathTokens[i]);
        }

        return stringBuilder.toString();
    }
}
