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

package io.prometheus.jmx.common.util;

import static java.lang.String.format;

/**
 * Utility class for validating method arguments and state.
 *
 * <p>Provides static methods for common validation checks that throw
 * {@link IllegalArgumentException} when the check fails.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. All methods are stateless.
 */
public class Precondition {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private Precondition() {
        // INTENTIONALLY BLANK
    }

    /**
     * Validates that an object is not {@code null}.
     *
     * @param object the object to validate
     * @throws IllegalArgumentException if {@code object} is {@code null}
     */
    public static void notNull(Object object) {
        notNull(object, "object is null");
    }

    /**
     * Validates that an object is not {@code null} with a custom error message.
     *
     * @param object the object to validate
     * @param message the error message if validation fails
     * @throws IllegalArgumentException if {@code object} is {@code null}
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that a string is not {@code null} and not blank.
     *
     * @param string the string to validate
     * @throws IllegalArgumentException if {@code string} is {@code null} or blank
     */
    public static void notNullOrEmpty(String string) {
        notNullOrEmpty(string, format("string [%s] is null or empty", string));
    }

    /**
     * Validates that a string is not {@code null} and not blank with a custom error message.
     *
     * @param string the string to validate
     * @param message the error message if validation fails
     * @throws IllegalArgumentException if {@code string} is {@code null} or blank
     */
    public static void notNullOrEmpty(String string, String message) {
        if (string == null || string.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that an integer is greater than or equal to a minimum value.
     *
     * @param value the value to validate
     * @param minimumValue the minimum allowed value (inclusive)
     * @throws IllegalArgumentException if {@code value} is less than {@code minimumValue}
     */
    public static void isGreaterThanOrEqualTo(int value, int minimumValue) {
        isGreaterThanOrEqualTo(
                value, minimumValue, format("value [%s] is less than minimum value [%s]", value, minimumValue));
    }

    /**
     * Validates that an integer is greater than or equal to a minimum value with a custom error
     * message.
     *
     * @param value the value to validate
     * @param minimumValue the minimum allowed value (inclusive)
     * @param message the error message if validation fails
     * @throws IllegalArgumentException if {@code value} is less than {@code minimumValue}
     */
    public static void isGreaterThanOrEqualTo(int value, int minimumValue, String message) {
        if (value < minimumValue) {
            throw new IllegalArgumentException(message);
        }
    }
}
