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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test support utilities for resolving classpath resources to filesystem paths, used by
 * integration-test environments to prepare Docker bind mounts.
 *
 * <p>This class is not instantiable and all methods are static.
 */
public final class TestSupport {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private TestSupport() {}

    /**
     * Resolves a classpath directory resource to an on-disk {@link Path}.
     *
     * @param contextClass the class whose classloader should resolve the resource; must not be {@code
     *     null}
     * @param resource the classpath resource path; must not be {@code null}
     * @return the on-disk path of the resource directory
     * @throws IllegalArgumentException if {@code contextClass} is {@code null}, the resource is not
     *     found, or the resource is not a file on disk (for example, inside a JAR)
     */
    public static Path resolveClasspathDirectory(Class<?> contextClass, String resource) {
        if (contextClass == null) {
            throw new IllegalArgumentException("contextClass must be set");
        }
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        URL url = contextClass.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalArgumentException("Classpath resource not found: " + resource);
        }
        if (!"file".equals(url.getProtocol())) {
            throw new IllegalArgumentException("Classpath resource is not a file on disk (inside JAR?): " + resource);
        }
        try {
            return Paths.get(url.toURI());
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve path for: " + resource, e);
        }
    }
}
