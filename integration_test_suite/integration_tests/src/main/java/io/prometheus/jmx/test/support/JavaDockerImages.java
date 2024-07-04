/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/** Class to get Docker image names */
public final class JavaDockerImages {

    private static final String JAVA_DOCKER_IMAGES_CONFIGURATION = "java.docker.images";

    private static final String JAVA_DOCKER_IMAGES_RESOURCE = "/java-docker-images.txt";

    private static final String SMOKE_TEST_JAVA_DOCKER_IMAGES_RESOURCE =
            "/smoke-test-java-docker-images.txt";

    private static String[] ALL_JAVA_DOCKER_IMAGE_NAMES;

    private static String[] SMOKE_TEST_JAVA_DOCKER_IMAGES;

    /** Predicate to accept all Docker image names */
    public static final Predicate<String> ALL_JAVA_VERSIONS = name -> true;

    /** Constructor */
    private JavaDockerImages() {
        // DO NOTHING
    }

    /**
     * Method to get List of all Docker image names
     *
     * @return the List of Docker image names
     */
    public static List<String> names() {
        return names(ALL_JAVA_VERSIONS);
    }

    /**
     * Method to get List of Docker image names filtered by a Predicate
     *
     * @param predicate predicate
     * @return the List of Docker image names
     */
    public static List<String> names(Predicate<String> predicate) {
        Objects.requireNonNull(predicate);

        synchronized (JavaDockerImages.class) {
            if (ALL_JAVA_DOCKER_IMAGE_NAMES == null) {
                ALL_JAVA_DOCKER_IMAGE_NAMES = load(JAVA_DOCKER_IMAGES_RESOURCE);
            }
            if (SMOKE_TEST_JAVA_DOCKER_IMAGES == null) {
                SMOKE_TEST_JAVA_DOCKER_IMAGES = load(SMOKE_TEST_JAVA_DOCKER_IMAGES_RESOURCE);
            }
        }

        String[] dockerImageNames;

        String dockerImageNameValue =
                System.getenv(
                        JAVA_DOCKER_IMAGES_CONFIGURATION
                                .toUpperCase(Locale.ENGLISH)
                                .replace('.', '_'));

        if (dockerImageNameValue != null) {
            dockerImageNameValue = dockerImageNameValue.trim();
            if (dockerImageNameValue.isBlank()) {
                dockerImageNameValue = null;
            }
        }

        if (dockerImageNameValue == null) {
            dockerImageNameValue = System.getProperty(JAVA_DOCKER_IMAGES_CONFIGURATION);
            if (dockerImageNameValue != null) {
                if (dockerImageNameValue.isBlank()) {
                    dockerImageNameValue = null;
                }
            }
        }

        if (dockerImageNameValue == null) {
            dockerImageNames = SMOKE_TEST_JAVA_DOCKER_IMAGES;
        } else if (dockerImageNameValue.equalsIgnoreCase("ALL")) {
            dockerImageNames = ALL_JAVA_DOCKER_IMAGE_NAMES;
        } else {
            dockerImageNames = dockerImageNameValue.split("\\s+");
        }

        List<String> dockerImageNamesCollection = new ArrayList<>();
        for (String dockerImageName : dockerImageNames) {
            if (predicate.test(dockerImageName)) {
                dockerImageNamesCollection.add(dockerImageName);
            }
        }

        return dockerImageNamesCollection;
    }

    /**
     * Method to load the list of Docker image names from a resource
     *
     * @param resource resource
     * @return the String array of lines
     */
    private static String[] load(String resource) {
        List<String> dockerImageNames = new ArrayList<>();
        BufferedReader bufferedReader;

        try {
            bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    JavaDockerImages.class.getResourceAsStream(resource),
                                    StandardCharsets.UTF_8));

            while (true) {
                String line = bufferedReader.readLine();

                if (line == null) {
                    break;
                }

                if (!line.trim().isEmpty() && !line.trim().startsWith("#")) {
                    dockerImageNames.add(line.trim());
                }
            }

            return dockerImageNames.toArray(new String[0]);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Exception reading resource " + JAVA_DOCKER_IMAGES_RESOURCE, e);
        }
    }
}
