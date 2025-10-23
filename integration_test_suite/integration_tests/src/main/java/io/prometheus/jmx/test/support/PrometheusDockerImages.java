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

/** Class to implement PrometheusDockerImages */
public final class PrometheusDockerImages {

    private static final String ALL = "ALL";

    private static final String DOCKER_IMAGES_CONFIGURATION = "prometheus.docker.images";

    private static final String SMOKE_TEST_DOCKER_IMAGES_RESOURCE =
            "/smoke-test-prometheus-docker-images.txt";

    private static final List<String> SMOKE_TEST_DOCKER_IMAGES =
            Collections.unmodifiableList(load(SMOKE_TEST_DOCKER_IMAGES_RESOURCE));

    private static final String ALL_DOCKER_IMAGES_RESOURCE = "/prometheus-docker-images.txt";

    private static final List<String> ALL_DOCKER_IMAGE_NAMES =
            Collections.unmodifiableList(load(ALL_DOCKER_IMAGES_RESOURCE));

    /** Constructor */
    private PrometheusDockerImages() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get a collection of Docker image names filter by configuration
     *
     * @return the collection of Docker image names
     */
    public static Collection<String> names() {
        String configurationValue =
                System.getenv(
                        DOCKER_IMAGES_CONFIGURATION.toUpperCase(Locale.ENGLISH).replace('.', '_'));

        if (configurationValue == null || configurationValue.trim().isEmpty()) {
            configurationValue = System.getProperty(DOCKER_IMAGES_CONFIGURATION);
        }

        if (configurationValue == null || configurationValue.trim().isEmpty()) {
            return SMOKE_TEST_DOCKER_IMAGES;
        }

        if (configurationValue.trim().equalsIgnoreCase(ALL)) {
            return ALL_DOCKER_IMAGE_NAMES;
        }

        return Collections.unmodifiableList(toList(configurationValue));
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
