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

package io.prometheus.jmx.common.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;

/**
 * Maven Shade plugin resource transformer for META-INF/services files.
 *
 * <p>This transformer modifies META-INF/services files to add a unique prefix to service names
 * and entries. This is used during shading to prevent conflicts between the shaded and unshaded
 * versions of the same library.
 *
 * <p>The prefix {@code e1723a08afd7bca35570fd31a7656f59.} is added to:
 *
 * <ul>
 *   <li>Service file names (e.g., META-INF/services/MyService becomes
 *       META-INF/services/e1723a08afd7bca35570fd31a7656f59.MyService)
 *   <li>Service implementation class names within the files
 * </ul>
 *
 * <p>This class is used during the Maven build process and is not used at runtime.
 */
public class CustomServiceTransformer implements ResourceTransformer {

    /**
     * Prefix to add to service names and entries.
     */
    private static final String PREFIX = "e1723a08afd7bca35570fd31a7656f59.";

    /**
     * META-INF services directory path.
     */
    private static final String SERVICES_DIR = "META-INF/services/";

    /**
     * Map of service files to their entries.
     */
    private final Map<String, List<String>> serviceEntries = new HashMap<>();

    /**
     * Constructs a new CustomServiceTransformer.
     *
     * <p>Default constructor for use by Maven Shade plugin.
     */
    public CustomServiceTransformer() {
        // INTENTIONALLY BLANK
    }

    /**
     * Determines whether the resource is a META-INF/services file that should be transformed.
     *
     * @param resource the resource path to check
     * @return {@code true} if the resource starts with {@value #SERVICES_DIR}, {@code false}
     *     otherwise
     */
    @Override
    public boolean canTransformResource(String resource) {
        return resource.startsWith(SERVICES_DIR);
    }

    /**
     * Reads a META-INF/services resource and adds its entries with the shading prefix.
     *
     * <p>Entries that already start with the prefix are not double-prefixed. Duplicate entries
     * within the same service file are deduplicated.
     *
     * @param resource the resource path being processed
     * @param is the input stream for the resource content
     * @param relocators the list of relocators applied during shading (not used by this transformer)
     * @throws IOException if the resource cannot be read
     */
    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            List<String> entries = serviceEntries.computeIfAbsent(resource, k -> new ArrayList<>());
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> {
                        // Avoid double prefixing
                        return line.startsWith(PREFIX) ? line : PREFIX + line;
                    })
                    .forEach(line -> {
                        if (!entries.contains(line)) {
                            entries.add(line);
                        }
                    });
        }
    }

    /**
     * Returns whether any META-INF/services resources were processed and need to be written.
     *
     * @return {@code true} if at least one service file was collected, {@code false} otherwise
     */
    @Override
    public boolean hasTransformedResource() {
        return !serviceEntries.isEmpty();
    }

    /**
     * Writes the collected service entries to the JAR with prefixed filenames and entries.
     *
     * <p>Each service file is written to a new path where the service name is prefixed with
     * {@value #PREFIX}, and all implementation class entries within the file are also prefixed.
     *
     * @param jos the JAR output stream to write to
     * @throws IOException if writing to the JAR fails
     */
    @Override
    public void modifyOutputStream(JarOutputStream jos) throws IOException {
        for (Map.Entry<String, List<String>> entry : serviceEntries.entrySet()) {
            String originalPath = entry.getKey();
            List<String> lines = entry.getValue();

            String newPath = SERVICES_DIR + PREFIX + originalPath.substring(SERVICES_DIR.length());

            jos.putNextEntry(new JarEntry(newPath));
            for (String line : lines) {
                jos.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
            jos.closeEntry();
        }
    }
}
