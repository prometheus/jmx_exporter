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

import java.util.Collection;
import java.util.List;

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

    private static final String DOCKER_IMAGES_CONFIGURATION = "java.docker.images";

    private static final String SMOKE_TEST_DOCKER_IMAGES_RESOURCE = "/smoke-test-java-docker-images.txt";

    private static final String ALL_DOCKER_IMAGES_RESOURCE = "/java-docker-images.txt";

    private static final List<String> SMOKE_TEST_DOCKER_IMAGES =
            DockerImagesSupport.load(SMOKE_TEST_DOCKER_IMAGES_RESOURCE, JavaDockerImages.class);

    private static final List<String> ALL_DOCKER_IMAGE_NAMES =
            DockerImagesSupport.load(ALL_DOCKER_IMAGES_RESOURCE, JavaDockerImages.class);

    private JavaDockerImages() {}

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
        return DockerImagesSupport.resolveNames(
                DOCKER_IMAGES_CONFIGURATION, SMOKE_TEST_DOCKER_IMAGES, ALL_DOCKER_IMAGE_NAMES);
    }
}
