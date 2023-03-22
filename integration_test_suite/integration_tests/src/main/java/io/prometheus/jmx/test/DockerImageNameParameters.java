/*
 * Copyright 2022-2023 Douglas Hoard
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

import org.devopology.test.engine.api.Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Class to get Docker image names
 */
public final class DockerImageNameParameters {

    private static final String DOCKER_IMAGE_NAMES_ENVIRONMENT_VARIABLE = "DOCKER_IMAGE_NAMES";
    private static final String DOCKER_IMAGE_NAMES_SYSTEM_PROPERTY = "docker.image.names";
    private static final String DOCKER_IMAGE_NAMES_RESOURCE = "/docker-image-names.txt";

    private static String[] DOCKER_IMAGE_NAMES;

    /**
     * Predicate to accept all Docker image names
     */
    public static final Predicate<Parameter> ALL_JAVA_VERSIONS = parameter -> true;

    /**
     * Predicate to accept only Docker image names that don't contain ":6"
     */
    public static final Predicate<Parameter> OMIT_JAVA_6_VERSIONS = parameter -> !parameter.value(String.class).contains(":6");

    /**
     * Constructor
     */
    private DockerImageNameParameters() {
        // DO NOTHING
    }

    /**
     * Method to get Stream of all Docker image name Parameters
     *
     * @return
     */
    public static Stream<Parameter> parameters() {
        return parameters(ALL_JAVA_VERSIONS);
    }

    /**
     * Method to get Stream of Docker image name Parameters filtered by a Predicate
     *
     * @param predicate
     * @return
     */
    public static Stream<Parameter> parameters(Predicate<Parameter> predicate) {
        Objects.requireNonNull(predicate);

        synchronized (DockerImageNameParameters.class) {
            if (DOCKER_IMAGE_NAMES == null) {
                DOCKER_IMAGE_NAMES = load(DOCKER_IMAGE_NAMES_RESOURCE);
            }
        }

        String[] dockerImageNames = null;

        // Environment variable to define specific Docker image names
        String dockerImageNamesEnvironmentVariable = System.getenv(DOCKER_IMAGE_NAMES_ENVIRONMENT_VARIABLE);
        if (dockerImageNamesEnvironmentVariable != null) {
            dockerImageNamesEnvironmentVariable = dockerImageNamesEnvironmentVariable.trim();
            if (!dockerImageNamesEnvironmentVariable.isEmpty()) {
                dockerImageNames = dockerImageNamesEnvironmentVariable.split("\\s+");
            }
        }

        if (dockerImageNames == null) {
            // System property value to defined specific Docker image names
            String docketImageNamesSystemProperty = System.getProperty(DOCKER_IMAGE_NAMES_SYSTEM_PROPERTY);
            if (docketImageNamesSystemProperty != null) {
                docketImageNamesSystemProperty = docketImageNamesSystemProperty.trim();
                if (!docketImageNamesSystemProperty.isEmpty()) {
                    dockerImageNames = docketImageNamesSystemProperty.split("\\s+");
                }
            }
        }

        if (dockerImageNames == null) {
            // Default to all Docker image names;
            dockerImageNames = DOCKER_IMAGE_NAMES;
        }

        Collection<Parameter> parameters = new ArrayList<>();
        for (String dockerImageName : dockerImageNames) {
            Parameter parameter = Parameter.of(dockerImageName);
            if (predicate.test(parameter)) {
                parameters.add(parameter);
            }
        }

        return parameters.stream();
    }

    /**
     * Method to determine if a Docker image is Java 6 based on the Docker image name (name contains ":6")
     *
     * @param dockerImageName
     * @return
     */
    public static boolean isJava6(String dockerImageName) {
        return dockerImageName.contains(":6");
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
                                    DockerImageNameParameters.class.getResourceAsStream(resource),
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
            throw new RuntimeException("Exception reading resource " + DOCKER_IMAGE_NAMES_RESOURCE, e);
        }
    }
}
