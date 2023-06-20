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

package io.prometheus.jmx.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class to get Docker image names
 */
public final class DockerImageNames {

    private static final String DOCKER_IMAGE_NAMES_CONFIGURATION = "docker.image.names";

    private static final String ALL_DOCKER_IMAGE_NAMES_RESOURCE = "/docker-image-names.all.txt";
    private static String[] ALL_DOCKER_IMAGE_NAMES;

    private static final String SMOKE_TEST_DOCKER_IMAGE_NAMES_RESOURCE = "/docker-image-names.smoke-test.txt";
    private static String[] SMOKE_TEST_DOCKER_IMAGE_NAMES;

    /**
     * Predicate to accept all Docker image names
     */
    public static final Predicate<String> ALL_JAVA_VERSIONS = name -> true;

    /**
     * Predicate to accept only Docker image names that don't contain ":6"
     */
    public static final Predicate<String> OMIT_JAVA_6_VERSIONS = name -> !name.contains(":6");

    /**
     * Constructor
     */
    private DockerImageNames() {
        // DO NOTHING
    }

    /**
     * Method to get Stream of all Docker image names
     *
     * @return
     */
    public static Stream<String> names() {
        return names(ALL_JAVA_VERSIONS);
    }

    /**
     * Method to get Stream of Docker image names filtered by a Predicate
     *
     * @param predicate
     * @return
     */
    public static Stream<String> names(Predicate<String> predicate) {
        Objects.requireNonNull(predicate);

        synchronized (DockerImageNames.class) {
            if (ALL_DOCKER_IMAGE_NAMES == null) {
                ALL_DOCKER_IMAGE_NAMES = load(ALL_DOCKER_IMAGE_NAMES_RESOURCE);
            }
            if (SMOKE_TEST_DOCKER_IMAGE_NAMES == null) {
                SMOKE_TEST_DOCKER_IMAGE_NAMES = load(SMOKE_TEST_DOCKER_IMAGE_NAMES_RESOURCE);
            }
        }

        String[] dockerImageNames;

        String dockerImageNameValue =
                System.getenv(DOCKER_IMAGE_NAMES_CONFIGURATION.toUpperCase(Locale.ENGLISH).replace('.', '_'));

        if (dockerImageNameValue != null) {
            dockerImageNameValue = dockerImageNameValue.trim();
            if (dockerImageNameValue.isBlank()) {
                dockerImageNameValue = null;
            }
        }

        if (dockerImageNameValue == null) {
            dockerImageNameValue = System.getProperty(DOCKER_IMAGE_NAMES_CONFIGURATION);
            if (dockerImageNameValue != null) {
                if (dockerImageNameValue.isBlank()) {
                    dockerImageNameValue = null;
                }
            }
        }

        if (dockerImageNameValue == null) {
            dockerImageNames = SMOKE_TEST_DOCKER_IMAGE_NAMES;
        } else if (dockerImageNameValue.equalsIgnoreCase("ALL")) {
            dockerImageNames = ALL_DOCKER_IMAGE_NAMES;
        } else {
            dockerImageNames = dockerImageNameValue.split("\\s+");
        }

        Collection<String> dockerImageNamesCollection = new ArrayList<>();
        for (String dockerImageName : dockerImageNames) {
            if (predicate.test(dockerImageName)) {
                dockerImageNamesCollection.add(dockerImageName);
            }
        }

        return dockerImageNamesCollection.stream();
    }

    /**
     * Method to load the list of Docker image names from a resource
     *
     * @param resource
     * @return
     */
    private static String[] load(String resource) {
        List<String> dockerImageNames = new ArrayList<>();
        BufferedReader bufferedReader;

        try {
            bufferedReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    DockerImageNames.class.getResourceAsStream(resource),
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
            throw new RuntimeException("Exception reading resource " + ALL_DOCKER_IMAGE_NAMES_RESOURCE, e);
        }
    }
}
