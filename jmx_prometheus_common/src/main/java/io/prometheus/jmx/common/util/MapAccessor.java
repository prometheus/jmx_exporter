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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** MapAccessor to work with Maps using a path style syntax for value access */
@SuppressWarnings("unchecked")
public class MapAccessor {

    private static final Map<Object, Object> EMPTY_MAP =
            Collections.unmodifiableMap(new HashMap<>());

    private final Map<Object, Object> map;

    /**
     * Constructor
     *
     * @param map the map
     */
    private MapAccessor(Map<Object, Object> map) {
        this.map = new LinkedHashMap<>(Collections.unmodifiableMap(map));
    }

    /**
     * Method to determine if a path exists
     *
     * @param path the path
     * @return true if the path exists (but could be null), false otherwise
     */
    public boolean contains(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        path = validate(path);
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
     * Method to get a path Object
     *
     * @param path the path
     * @return an Optional containing the path Object or an empty Optional if the path doesn't exist
     */
    public Optional<Object> get(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        if (!contains(path)) {
            return Optional.empty();
        }

        path = validate(path);
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

        return current != null ? Optional.of(current) : Optional.of(EMPTY_MAP);
    }

    /**
     * Method to create a MapAccessor
     *
     * @param map the map
     * @return a MapAccessor
     */
    public static MapAccessor of(Map<Object, Object> map) {
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }

        return new MapAccessor(map);
    }

    /**
     * Method to validate a path
     *
     * @param path the path
     * @return the return value
     */
    private String validate(String path) {
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

        if (path.matches(".*\\/(\\s*)\\/.*")) {
            throw new IllegalArgumentException(format("path [%s] is invalid", path));
        }

        return path;
    }
}
