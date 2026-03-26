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

package io.prometheus.jmx.common.util;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Utility class for loading and exporting classpath resources.
 *
 * <p>Provides static methods to check for resource existence, load resource content as strings,
 * and export resources to files.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. All methods are stateless.
 */
public class ResourceSupport {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private ResourceSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Checks if a classpath resource exists.
     *
     * <p>The resource path is automatically prefixed with {@code /} if not already present.
     *
     * @param resource the resource path to check, must not be {@code null}
     * @return {@code true} if the resource exists, {@code false} otherwise
     */
    public static boolean exists(String resource) {
        boolean hasResource = false;

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        try (InputStream inputStream = ResourceSupport.class.getResourceAsStream(resource)) {
            hasResource = inputStream != null;
        } catch (Throwable t) {
            // INTENTIONALLY BLANK
        }

        return hasResource;
    }

    /**
     * Loads a classpath resource as a string.
     *
     * <p>The resource path is automatically prefixed with {@code /} if not already present.
     * Lines are joined using the platform line separator.
     *
     * @param resource the resource path to load, must not be {@code null}
     * @return the resource content as a string
     * @throws IOException if the resource does not exist or cannot be read
     * @throws NullPointerException if {@code resource} is {@code null}
     */
    public static String load(String resource) throws IOException {
        Precondition.notNull(resource, "resource is null");

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        StringBuilder stringBuilder = new StringBuilder();

        InputStream inputStream = ResourceSupport.class.getResourceAsStream(resource);
        if (inputStream == null) {
            throw new IOException("Resource [" + resource + "] not found");
        }

        try (BufferedReader bufferedReader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }

                if (stringBuilder.length() > 0) {
                    stringBuilder.append(System.lineSeparator());
                }

                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * Exports a classpath resource to a file.
     *
     * <p>The resource path is automatically prefixed with {@code /} if not already present.
     * The resource content is written to the specified file using an 8KB buffer.
     *
     * @param resource the resource path to export, must not be {@code null} or blank
     * @param file the destination file, must not be {@code null}
     * @throws IOException if the resource does not exist, cannot be read, or the file cannot be
     *     written
     * @throws NullPointerException if {@code resource} is {@code null} or {@code file} is
     *     {@code null}
     */
    public static void export(String resource, File file) throws IOException {
        Precondition.notNullOrEmpty(resource, "resource is null or empty");
        Precondition.notNull(file, "file is null");

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        try (InputStream inputStream = ResourceSupport.class.getResourceAsStream(resource)) {
            if (inputStream == null) {
                throw new IOException(format("Resource [%s] not found", resource));
            }

            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                byte[] buffer = new byte[8192];
                int count;

                while ((count = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }
            }
        }
    }
}
