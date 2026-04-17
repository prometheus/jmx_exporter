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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Utility class for loading and parsing YAML configuration files.
 *
 * <p>Provides static methods to load YAML content from strings or files into map structures
 * that can be traversed using {@link MapAccessor}.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. All methods are stateless.
 */
public class YamlSupport {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private YamlSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Loads a YAML string into a map structure.
     *
     * @param yaml the YAML content to parse, must not be {@code null}
     * @return a map representing the YAML structure, may be empty but never {@code null}
     */
    public static Map<Object, Object> loadYaml(String yaml) {
        return new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
    }

    /**
     * Loads a YAML file into a map structure.
     *
     * @param file the YAML file to load, must not be {@code null} and must exist
     * @return a map representing the YAML structure, may be empty but never {@code null}
     * @throws IOException if the file cannot be read or parsed
     */
    public static Map<Object, Object> loadYaml(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
    }
}
