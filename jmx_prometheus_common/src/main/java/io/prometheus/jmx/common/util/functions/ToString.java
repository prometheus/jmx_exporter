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
 * Function that converts an Object to a String.
 *
 * <p>This function handles String values directly and calls {@link Object#toString()} on other
 * objects. If conversion fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Object, String> toString = new ToString(() -> new ConfigurationException("Invalid string"));
 * String result = toString.apply("hello");  // Returns "hello"
 * String result2 = toString.apply(42);  // Returns "42"
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class ToString implements Function<Object, String> {

    /**
     * Supplier for the exception to throw when conversion fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a ToString function with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when conversion fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public ToString(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    @Override
    public String apply(Object value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        try {
            if (value instanceof String) {
                return (String) value;
            } else {
                return value.toString();
            }
        } catch (Throwable t) {
            throw supplier.get();
        }
    }
}
