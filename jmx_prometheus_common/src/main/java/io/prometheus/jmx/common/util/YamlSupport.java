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
