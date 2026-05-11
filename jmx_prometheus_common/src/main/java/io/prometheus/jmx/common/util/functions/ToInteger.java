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
 * Function that converts an Object to an Integer.
 *
 * <p>This function handles Integer values and String representations of integers.
 * If conversion fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Object, Integer> toInteger = new ToInteger(() -> new ConfigurationException("Invalid integer"));
 * Integer result = toInteger.apply("42");  // Returns 42
 * Integer result2 = toInteger.apply(100);  // Returns 100
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class ToInteger implements Function<Object, Integer> {

    /**
     * Supplier for the exception to throw when conversion fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ToInteger function with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when conversion fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public ToInteger(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Converts the given object to an {@link Integer}.
     *
     * @param value the object to convert, must not be {@code null}
     * @return the integer value
     * @throws RuntimeException if conversion fails, as supplied by the configured exception
     *     supplier
     */
    @Override
    public Integer apply(Object value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
