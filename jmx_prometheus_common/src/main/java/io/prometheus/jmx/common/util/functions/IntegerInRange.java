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
 * Function that validates an Integer is within a specified range.
 *
 * <p>This function checks if an integer value is within the inclusive range [minimum, maximum].
 * If validation fails, it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<Integer, Integer> validator = new IntegerInRange(1, 100, () -> new ConfigurationException("Value out of range"));
 * Integer result = validator.apply(50);  // Returns 50
 * validator.apply(200);  // Throws ConfigurationException
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class IntegerInRange implements Function<Integer, Integer> {

    /**
     * The minimum allowed value (inclusive).
     */
    private final int minimum;

    /**
     * The maximum allowed value (inclusive).
     */
    private final int maximum;

    /**
     * Supplier for the exception to throw when validation fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs an IntegerInRange validator with the specified range and exception supplier.
     *
     * @param minimum the minimum allowed value (inclusive)
     * @param maximum the maximum allowed value (inclusive)
     * @param supplier supplier for the exception to throw when validation fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public IntegerInRange(int minimum, int maximum, Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.minimum = minimum;
        this.maximum = maximum;
        this.supplier = supplier;
    }

    @Override
    public Integer apply(Integer value) {
        if (value < minimum || value > maximum) {
            throw supplier.get();
        }

        return value;
    }
}
