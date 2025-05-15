/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Class to implement ResourceSupport */
public class ResourceSupport {

    /** Constructor */
    private ResourceSupport() {
        // INTENTIONALLY BLANK
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
}
