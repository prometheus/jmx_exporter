/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.common.util;

public class Precondition {

    private Precondition() {
        // DO NOTHING
    }

    /**
     * Method to check an Object is not null
     *
     * @param object object
     */
    public static void notNull(Object object) {
        notNull(object, "object is null");
    }

    /**
     * Method to check an Object is not null
     *
     * @param object object
     * @param message message
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Method to check that a String is not null and not empty
     *
     * @param string string
     */
    public static void notNullOrEmpty(String string) {
        notNullOrEmpty(string, String.format("string [%s] is null or empty", string));
    }

    /**
     * Method to check that a String is not null and not empty
     *
     * @param string string
     * @param message message
     */
    public static void notNullOrEmpty(String string, String message) {
        if (string == null || string.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Method to check that an integer is greater than or equal to a value
     *
     * @param value value
     * @param minimumValue minimumValue
     */
    public static void isGreaterThanOrEqualTo(int value, int minimumValue) {
        isGreaterThanOrEqualTo(
                value,
                minimumValue,
                String.format("value [%s] is less than minimum value [%s]", value, minimumValue));
    }

    /**
     * Method to check that an integer is greater than or equal to a value
     *
     * @param value value
     * @param minimumValue minimumValue
     * @param message message
     */
    public static void isGreaterThanOrEqualTo(int value, int minimumValue, String message) {
        if (value < minimumValue) {
            throw new IllegalArgumentException(message);
        }
    }
}
