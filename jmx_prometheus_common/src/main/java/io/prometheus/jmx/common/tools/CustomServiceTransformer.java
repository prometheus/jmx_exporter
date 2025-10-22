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
 * ResourceTransformer that modifies the META-INF/services files to add a prefix to service file
 * names and entries
 */
public class CustomServiceTransformer implements ResourceTransformer {

    private static final String PREFIX = "e1723a08afd7bca35570fd31a7656f59.";
    private static final String SERVICES_DIR = "META-INF/services/";

    private final Map<String, List<String>> serviceEntries = new HashMap<>();

    /** Default constructor for CustomServiceTransformer. */
    public CustomServiceTransformer() {
        // INTENTIONALLY BLANK
    }

    @Override
    public boolean canTransformResource(String resource) {
        return resource.startsWith(SERVICES_DIR);
    }

    @Override
    public void processResource(String resource, InputStream is, List<Relocator> relocators)
            throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            List<String> entries = serviceEntries.computeIfAbsent(resource, k -> new ArrayList<>());
            reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(
                            line -> {
                                // Avoid double prefixing
                                return line.startsWith(PREFIX) ? line : PREFIX + line;
                            })
                    .forEach(
                            line -> {
                                if (!entries.contains(line)) {
                                    entries.add(line);
                                }
                            });
        }
    }

    @Override
    public boolean hasTransformedResource() {
        return !serviceEntries.isEmpty();
    }

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
