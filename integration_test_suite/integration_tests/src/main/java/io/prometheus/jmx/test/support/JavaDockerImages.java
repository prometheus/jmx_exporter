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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Provides Java Docker image names for integration tests, loaded from classpath resources
 * with environment-variable and system-property overrides.
 *
 * <p>Image names are resolved in the following order:
 * <ol>
 *   <li>Environment variable {@code JAVA_DOCKER_IMAGES}</li>
 *   <li>System property {@code java.docker.images}</li>
 *   <li>Smoke test image list (default)</li>
 * </ol>
 *
 * <p>Setting the value to {@code ALL} selects the full image list.
 */
public final class JavaDockerImages {

    private static final String ALL = "ALL";

    private static final String DOCKER_IMAGES_CONFIGURATION = "java.docker.images";

    /**
     * The classpath resource for the smoke test Java Docker image list.
     */
    public static final String SMOKE_TEST_DOCKER_IMAGES_RESOURCE = "/smoke-test-java-docker-images.txt";

    private static final List<String> SMOKE_TEST_DOCKER_IMAGES =
            Collections.unmodifiableList(load(SMOKE_TEST_DOCKER_IMAGES_RESOURCE));

    /**
     * The classpath resource for the complete Java Docker image list.
     */
    public static final String ALL_DOCKER_IMAGES_RESOURCE = "/java-docker-images.txt";

    private static final List<String> ALL_DOCKER_IMAGE_NAMES =
            Collections.unmodifiableList(load(ALL_DOCKER_IMAGES_RESOURCE));

    /**
     * Private constructor to prevent instantiation.
     */
    private JavaDockerImages() {
        // INTENTIONALLY BLANK
    }

    /**
     * Returns the configured Java Docker image names for integration tests.
     *
     * <p>Checks the environment variable {@code JAVA_DOCKER_IMAGES} first, then the system
     * property {@code java.docker.images}. If neither is set, returns the smoke test image list.
     * If the value is {@code ALL}, returns the complete image list.
     *
     * @return an unmodifiable collection of Docker image names
     */
    public static Collection<String> names() {
        String configurationValues = System.getenv(
                DOCKER_IMAGES_CONFIGURATION.toUpperCase(Locale.ENGLISH).replace('.', '_'));

        if (configurationValues == null || configurationValues.trim().isEmpty()) {
            configurationValues = System.getProperty(DOCKER_IMAGES_CONFIGURATION);
        }

        if (configurationValues == null || configurationValues.trim().isEmpty()) {
            return SMOKE_TEST_DOCKER_IMAGES;
        }

        if (configurationValues.trim().equalsIgnoreCase(ALL)) {
            return ALL_DOCKER_IMAGE_NAMES;
        }

        return Collections.unmodifiableList(toList(configurationValues));
    }

    /**
     * Returns all available Java Docker image names from the full image list.
     *
     * @return an unmodifiable collection of all Docker image names
     */
    public static Collection<String> allNames() {
        return ALL_DOCKER_IMAGE_NAMES;
    }

    /**
     * Loads Docker image names from a classpath resource, skipping blank and comment lines.
     *
     * @param resource the classpath resource path to load
     * @return the list of non-blank, non-comment image names
     * @throws RuntimeException if the resource cannot be found or read
     */
    private static List<String> load(String resource) {
        List<String> lines = new ArrayList<>();

        InputStream inputStream = null;
        BufferedReader bufferedReader = null;

        try {
            inputStream = JavaDockerImages.class.getResourceAsStream(resource);

            if (inputStream == null) {
                throw new IOException("Resource not found");
            }

            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    lines.add(line.trim());
                }
            }

            return lines;
        } catch (Throwable t) {
            throw new RuntimeException(format("Exception reading resource [%s]", resource), t);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable t) {
                    // INTENTIONALLY BLANK
                }
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable t) {
                    // INTENTIONALLY BLANK
                }
            }
        }
    }

    /**
     * Splits a whitespace-separated string into a list of trimmed, non-empty tokens.
     *
     * @param string the whitespace-separated string to split
     * @return the list of trimmed tokens
     */
    private static List<String> toList(String string) {
        List<String> list = new ArrayList<>();

        String[] strings = string.split("\\s+");
        for (String s : strings) {
            if (!s.trim().isEmpty()) {
                list.add(s.trim());
            }
        }

        return list;
    }
}
