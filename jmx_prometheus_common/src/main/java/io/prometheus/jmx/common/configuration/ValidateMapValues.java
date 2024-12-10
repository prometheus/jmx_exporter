package io.prometheus.jmx.common.configuration;

import io.prometheus.jmx.common.util.Precondition;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class to validate a Maps keys/values, throwing a RuntimeException from the Supplier if any
 * key/value is null or empty
 */
public class ValidateMapValues implements Function<Map<String, String>, Map<String, String>> {

    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructor
     *
     * @param supplier supplier
     */
    public ValidateMapValues(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public Map<String, String> apply(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null || key.trim().isEmpty() || value == null || value.isEmpty()) {
                throw supplier.get();
            }
        }

        return map;
    }
}
