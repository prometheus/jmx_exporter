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

import io.prometheus.jmx.test.support.environment.EnvironmentException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Copies a classpath directory resource to a fresh world-readable temp directory and returns its
     * path.
     *
     * <p>The temp directory and its contents are registered with {@link File#deleteOnExit()}.
     * Permissions are applied so directories and shell scripts are world-executable and all entries are
     * world-readable; any permission operation that fails is reported via {@link EnvironmentException}.
     *
     * @param contextClass the class whose classloader should resolve the resource; must not be {@code
     *     null}
     * @param resource the classpath resource path; must not be {@code null}
     * @return the path of the created temp directory
     * @throws IllegalArgumentException if {@code contextClass} is {@code null} or the resource cannot be
     *     resolved
     * @throws EnvironmentException if the copy or permission application fails
     */
    public static Path copyClasspathDirectoryToTemp(Class<?> contextClass, String resource) {
        Path source = resolveClasspathDirectory(contextClass, resource);
        try {
            Path tempDir = Files.createTempDirectory("docker-bind-");
            tempDir.toFile().deleteOnExit();
            copyTree(source, tempDir);
            applyReadablePermissions(tempDir);
            return tempDir;
        } catch (IOException e) {
            throw new EnvironmentException("Failed to copy classpath resource: " + resource, e);
        }
    }

    /**
     * Recursively copies a source directory tree to a target directory.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException if a file or directory cannot be copied
     */
    private static void copyTree(Path source, Path target) throws IOException {
        try (var walk = Files.walk(source)) {
            for (Path path : walk.toList()) {
                Path dest = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    dest.toFile().deleteOnExit();
                }
            }
        }
    }

    /**
     * Makes the directory tree rooted at {@code root} world-readable, applying world-execute only to
     * directories and shell scripts.
     *
     * <p>Any permission operation that returns {@code false} is collected; if any failures occur, an
     * {@link EnvironmentException} listing them is thrown.
     *
     * @param root the root of the tree to chmod
     * @throws IOException if the tree cannot be walked
     * @throws EnvironmentException if any permission operation fails
     */
    private static void applyReadablePermissions(Path root) throws IOException {
        List<String> failures = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            for (Path path : walk.toList()) {
                File file = path.toFile();
                if (!file.setReadable(true, false)) {
                    failures.add("setReadable " + file);
                }
                if (file.isDirectory() || isScript(file)) {
                    if (!file.setExecutable(true, false)) {
                        failures.add("setExecutable " + file);
                    }
                }
            }
        }
        if (!failures.isEmpty()) {
            throw new EnvironmentException("Failed to make bind directory world-readable: " + failures);
        }
    }

    /**
     * Returns whether the given file is a shell script by name convention.
     *
     * @param file the file to test
     * @return {@code true} if the file name ends with {@code .sh}
     */
    private static boolean isScript(File file) {
        return file.getName().endsWith(".sh");
    }
}
