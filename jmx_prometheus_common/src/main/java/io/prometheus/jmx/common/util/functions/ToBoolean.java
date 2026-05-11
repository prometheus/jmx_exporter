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
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Function that converts an Object to a Boolean.
 *
 * <p>This function handles both Boolean values and String representations of boolean values.
 * If conversion fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Object, Boolean> toBoolean = new ToBoolean(() -> new ConfigurationException("Invalid boolean"));
 * Boolean result = toBoolean.apply("true");  // Returns true
 * Boolean result2 = toBoolean.apply(false);  // Returns false
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class ToBoolean implements Function<Object, Boolean> {

    /**
     * Supplier for the exception to throw when conversion fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ToBoolean function with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when conversion fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public ToBoolean(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Converts the given object to a {@link Boolean}.
     *
     * <p>Handles both {@code Boolean} values and string representations of boolean values.
     *
     * @param value the object to convert, must not be {@code null}
     * @return the boolean value
     * @throws RuntimeException if conversion fails, as supplied by the configured exception
     *     supplier
     */
    @Override
    public Boolean apply(Object value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return Boolean.valueOf(value.toString());
            }
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
