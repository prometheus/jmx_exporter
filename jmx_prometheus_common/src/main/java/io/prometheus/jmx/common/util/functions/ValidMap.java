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

package io.prometheus.jmx.common.util.functions;

import io.prometheus.jmx.common.util.Precondition;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Function that validates a map contains valid (non-null, non-blank) string keys and values.
 *
 * <p>This function iterates through all entries in the map and validates that neither keys nor
 * values are null or blank. If validation fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Map<String, String>, Map<String, String>> validator = ValidMap.of(() -> new ConfigurationException("Invalid map"));
 * Map<String, String> valid = new java.util.LinkedHashMap<String, String>();
 * valid.put("key", "value");
 * Map<String, String> result = validator.apply(valid);  // Returns the map
 *
 * Map<String, String> invalid = new java.util.LinkedHashMap<String, String>();
 * invalid.put("key", "");
 * validator.apply(invalid);  // Throws ConfigurationException
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class ValidMap implements Function<Map<String, String>, Map<String, String>> {

    /**
     * Supplier for the exception to throw when validation fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ValidMap validator with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when validation fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    private ValidMap(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }
    /**
     * Validates that all keys and values in the map are non-null and non-blank strings.
     *
     * @param map the map to validate, must not be {@code null}
     * @return the validated map, unchanged
     * @throws RuntimeException if any key or value is {@code null} or blank, as supplied by the
     *     configured exception supplier
     */
    @Override
    public Map<String, String> apply(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key == null
                    || key.trim().isEmpty()
                    || value == null
                    || value.trim().isEmpty()) {
                throw supplier.get();
            }
        }

        return map;
    }

    public static ValidMap of(Supplier<? extends RuntimeException> supplier) {
        return new ValidMap(supplier);
    }
}
