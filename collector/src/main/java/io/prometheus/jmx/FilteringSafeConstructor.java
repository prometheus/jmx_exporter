package io.prometheus.jmx;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Class to implement an extension to SafeConstructor to handle SnakeYAML version differences
 *
 * This is a SnakeYAML version 2.0.0 which provides safe filtering
 *
 * Java 6 versions of this class implement safe filtering of entity types
 */
public class FilteringSafeConstructor extends SafeConstructor {

    public FilteringSafeConstructor() {
        super(new LoaderOptions());
    }
}

