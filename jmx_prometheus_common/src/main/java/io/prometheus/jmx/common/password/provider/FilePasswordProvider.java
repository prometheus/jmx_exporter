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

package io.prometheus.jmx.common.password.provider;

import io.prometheus.jmx.common.password.PasswordProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * A PasswordProvider that reads the password from a file. The file path should be specified with
 * the prefix "file:". If the file cannot be read or is empty, it returns an empty Optional.
 */
public class FilePasswordProvider implements PasswordProvider {

    public static final String PREFIX = "file:";

    @Override
    public boolean supports(String spec) {
        return spec.toLowerCase().startsWith(PREFIX);
    }

    @Override
    public Optional<String> resolve(String spec) {
        String path = spec.substring(PREFIX.length()).trim();

        // If the path is empty after trimming, return an empty Optional
        if (path.isEmpty()) {
            return Optional.empty();
        }

        // Check if the file exists and is a regular file
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            return Optional.empty();
        }

        try {
            // Read the file content as a UTF-8 string and trim it
            String value =
                    new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8).trim();

            // Return an empty Optional if the value is empty
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (IOException e) {
            // Fall through silently
            return Optional.empty();
        }
    }
}
