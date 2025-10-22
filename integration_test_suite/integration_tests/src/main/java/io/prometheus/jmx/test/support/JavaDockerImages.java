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

/** Class to implement JavaDockerImages */
public final class JavaDockerImages {

    private static final String ALL = "ALL";

    private static final String DOCKER_IMAGES_CONFIGURATION = "java.docker.images";

    public static final String SMOKE_TEST_DOCKER_IMAGES_RESOURCE =
            "/smoke-test-java-docker-images.txt";

    private static final List<String> SMOKE_TEST_DOCKER_IMAGES =
            Collections.unmodifiableList(load(SMOKE_TEST_DOCKER_IMAGES_RESOURCE));

    public static final String ALL_DOCKER_IMAGES_RESOURCE = "/java-docker-images.txt";

    private static final List<String> ALL_DOCKER_IMAGE_NAMES =
            Collections.unmodifiableList(load(ALL_DOCKER_IMAGES_RESOURCE));

    /** Constructor */
    private JavaDockerImages() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get collection of Docker image names filtered by a Predicate
     *
     * @return the collection of Docker image names
     */
    public static Collection<String> names() {
        String configurationValues =
                System.getenv(
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
     * Method to get a collection of all Docker image names
     *
     * @return a collection of all Docker image names
     */
    public static Collection<String> allNames() {
        return ALL_DOCKER_IMAGE_NAMES;
    }

    /**
     * Method to load the list of Docker image names from a resource
     *
     * @param resource resource
     * @return the List of lines
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

            bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

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
     * Method to split a String on whitespace and return a List of Strings
     *
     * @param string string
     * @return a List of Strings
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
