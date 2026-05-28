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

package io.prometheus.jmx.test.support;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Package-private utility class encapsulating the shared logic for resolving Docker image names
 * from classpath resources with environment-variable and system-property overrides.
 *
 * <p>Loaded resources are cached by resource path so that repeated calls from different
 * test classes avoid redundant I/O.
 *
 * <p>This class is not part of the public API and should only be used by
 * {@link JavaDockerImages} and {@link PrometheusDockerImages}.
 */
final class DockerImagesSupport {

    private static final String ALL = "ALL";

    private static final ConcurrentMap<String, List<String>> RESOURCE_CACHE = new ConcurrentHashMap<>();

    private DockerImagesSupport() {}

    /**
     * Resolves Docker image names by checking an environment variable, a system property,
     * and falling back to a default list.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Environment variable derived from {@code configurationKey} (dots replaced with
     *       underscores, uppercased)</li>
     *   <li>System property {@code configurationKey}</li>
     *   <li>{@code defaultImages} if neither is set</li>
     * </ol>
     *
     * <p>If the resolved value equals {@code "ALL"} (case-insensitive), returns
     * {@code allImages}.
     *
     * @param configurationKey the dot-separated configuration key (e.g. {@code "java.docker.images"})
     * @param defaultImages the default image list returned when no override is set
     * @param allImages the complete image list returned when the value is {@code "ALL"}
     * @return an unmodifiable collection of Docker image names
     */
    static Collection<String> resolveNames(
            String configurationKey, List<String> defaultImages, List<String> allImages) {
        String configurationValue =
                System.getenv(configurationKey.toUpperCase(Locale.ENGLISH).replace('.', '_'));

        if (isBlank(configurationValue)) {
            configurationValue = System.getProperty(configurationKey);
        }

        if (isBlank(configurationValue)) {
            return defaultImages;
        }

        if (configurationValue.strip().equalsIgnoreCase(ALL)) {
            return allImages;
        }

        return toList(configurationValue);
    }

    /**
     * Loads Docker image names from a classpath resource, skipping blank and comment lines.
     * Results are cached by resource path so that repeated calls return the same list instance.
     *
     * @param resource the classpath resource path (e.g. {@code "/java-docker-images.txt"})
     * @param resourceClass the class whose classloader will load the resource
     * @return an unmodifiable list of non-blank, non-comment image names
     * @throws RuntimeException if the resource cannot be found or read
     */
    static List<String> load(String resource, Class<?> resourceClass) {
        return RESOURCE_CACHE.computeIfAbsent(resource, key -> doLoad(key, resourceClass));
    }

    private static List<String> doLoad(String resource, Class<?> resourceClass) {
        InputStream inputStream = resourceClass.getResourceAsStream(resource);

        if (inputStream == null) {
            throw new RuntimeException(
                    format("Exception reading resource [%s]", resource), new IOException("Resource not found"));
        }

        try (InputStream is = inputStream;
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::strip)
                    .filter(line -> !line.isBlank() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableList());
        } catch (Exception e) {
            throw new RuntimeException(format("Exception reading resource [%s]", resource), e);
        }
    }

    /**
     * Splits a whitespace-separated string into an unmodifiable list of trimmed, non-empty tokens.
     *
     * @param string the whitespace-separated string to split
     * @return an unmodifiable list of trimmed tokens
     */
    static List<String> toList(String string) {
        return Arrays.stream(string.split("\\s+"))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableList());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
