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

import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.Precondition;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Function that converts an Object to a MapAccessor.
 *
 * <p>This function casts the input object to a map and wraps it in a MapAccessor. If casting
 * fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Object, MapAccessor> toMapAccessor = new ToMapAccessor(() -> new ConfigurationException("Invalid map"));
 * MapAccessor result = toMapAccessor.apply(Map.of("key", "value"));
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
@SuppressWarnings("unchecked")
public class ToMapAccessor implements Function<Object, MapAccessor> {

    /**
     * Supplier for the exception to throw when conversion fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ToMapAccessor function with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when conversion fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public ToMapAccessor(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public MapAccessor apply(Object value) {
        try {
            return MapAccessor.of((Map<Object, Object>) value);
        } catch (ClassCastException e) {
            throw supplier.get();
        }
    }
}
