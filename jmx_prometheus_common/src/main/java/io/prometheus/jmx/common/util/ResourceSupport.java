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

/** Class to implement ResourceSupport */
public class ResourceSupport {

    /** Constructor */
    private ResourceSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to check if a resource exists
     *
     * @param resource resource
     * @return true if the resource exists, false otherwise
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
     * Method to load a resource's content
     *
     * @param resource resource
     * @return the resource content
     * @throws IOException IOException
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
     * Method to export a resource to a temporary file
     *
     * @param resource the resource to export
     * @param file the file to export to
     * @throws IOException If an I/O error occurs
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

            try (OutputStream outputStream =
                    new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
                byte[] buffer = new byte[8192];
                int count;

                while ((count = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, count);
                }
            }
        }
    }
}
