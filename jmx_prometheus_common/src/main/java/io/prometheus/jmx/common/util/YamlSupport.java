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
import org.yaml.snakeyaml.Yaml;

/** Class to implement YamlSupport */
public class YamlSupport {

    /** Constructor */
    private YamlSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to load a YAML string
     *
     * @param yaml the YAML string
     * @return a map of the YAML string
     */
    public static Map<Object, Object> loadYaml(String yaml) {
        return new Yaml().load(yaml);
    }

    /**
     * Method to load a YAML file
     *
     * @param file the YAML file
     * @return a map of the YAML file
     * @throws IOException If an I/O error occurs
     */
    public static Map<Object, Object> loadYaml(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            return new Yaml().load(reader);
        }
    }
}
