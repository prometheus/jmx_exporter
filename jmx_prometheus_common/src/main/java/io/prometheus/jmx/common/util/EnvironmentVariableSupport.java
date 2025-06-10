/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

/** Class to implement EnvironmmentVariableSupport */
public class EnvironmentVariableSupport {

    /** Constructor */
    private EnvironmentVariableSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Resolves a string as an environment variable if valid and defined. Supports "$VAR" and
     * "${VAR}" formats. If not valid or undefined/blank, returns the original value.
     *
     * @param value the input string
     * @return the environment variable value if valid and non-blank, otherwise the original value
     */
    public static String resolve(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }

        String environmentVariableName = null;

        if (value.startsWith("${") && value.endsWith("}") && value.length() > 3) {
            environmentVariableName = value.substring(2, value.length() - 1);
        } else if (value.startsWith("$") && value.length() > 1) {
            environmentVariableName = value.substring(1);
        }

        if (isValidEnvironmentVariable(environmentVariableName)) {
            String environmentVariableValue = System.getenv(environmentVariableName);
            if (environmentVariableValue != null && !environmentVariableValue.trim().isEmpty()) {
                return environmentVariableValue;
            }
        }

        return value;
    }

    /**
     * Validates that the string is a valid environment variable name. Only allows letters, digits,
     * and underscores, and must not be null or empty.
     *
     * @param name the variable name
     * @return true if valid
     */
    private static boolean isValidEnvironmentVariable(String name) {
        return name != null && !name.isEmpty() && name.matches("[A-Za-z_][A-Za-z0-9_]*");
    }
}
