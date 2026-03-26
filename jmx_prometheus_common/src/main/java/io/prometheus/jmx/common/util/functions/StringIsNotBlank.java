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
 * Function that validates a String is not blank.
 *
 * <p>A string is considered blank if it is empty after trimming whitespace. If validation fails,
 * it throws an exception from the provided supplier.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Function<String, String> validator = new StringIsNotBlank(() -> new ConfigurationException("String is blank"));
 * String result = validator.apply("hello");  // Returns "hello"
 * validator.apply("   ");  // Throws ConfigurationException
 * }</pre>
 *
 * <p>Thread-safety: This class is thread-safe. Each invocation operates on the input independently.
 */
public class StringIsNotBlank implements Function<String, String> {

    /**
     * Supplier for the exception to throw when validation fails.
     */
    private final Supplier<? extends RuntimeException> supplier;

    /**
     * Constructs a StringIsNotBlank validator with the specified exception supplier.
     *
     * @param supplier supplier for the exception to throw when validation fails, must not be
     *     {@code null}
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public StringIsNotBlank(Supplier<? extends RuntimeException> supplier) {
        Precondition.notNull(supplier);
        this.supplier = supplier;
    }

    /**
     * Validates that the string is not blank.
     *
     * @param value the string to validate
     * @return the validated string, unchanged
     * @throws RuntimeException if the string is blank
     */
    @Override
    public String apply(String value) {
        if (value.trim().isEmpty()) {
            throw supplier.get();
        }

        return value;
    }
}
