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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Function that converts an Object to a {@code Map<String, String>}.
 *
 * <p>This function casts the input object to a map and converts all keys and values to trimmed
 * strings. If conversion fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Object, Map<String, String>> toMap = new ToMap(() -> new ConfigurationException("Invalid map"));
 * Map<String, String> result = toMap.apply(Map.of("key1", "value1", "key2", "value2"));
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
@SuppressWarnings("unchecked")
public class ToMap implements Function<Object, Map<String, String>> {

    /**
     * Supplier for the exception to throw when conversion fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ToMap function with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when conversion fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public ToMap(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Casts the given object to a map and converts all keys and values to trimmed strings.
     *
     * @param o the object to convert, expected to be a {@link Map}
     * @return a new {@link LinkedHashMap} with all keys and values converted to trimmed strings
     * @throws RuntimeException if conversion fails, as supplied by the configured exception supplier
     */
    @Override
    public Map<String, String> apply(Object o) {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            Map<Object, Object> map = (Map<Object, Object>) o;

            map.forEach(
                    (o1, o2) -> result.put(o1.toString().trim(), o2.toString().trim()));

            return result;
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
