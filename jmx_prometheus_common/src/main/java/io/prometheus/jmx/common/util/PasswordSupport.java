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

import java.util.regex.Pattern;

/** Class to implement PasswordSupport */
public class PasswordSupport {

    private static final Pattern ENVIRONMENT_VARIABLE_PATTERN =
            Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** Constructor */
    private PasswordSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Resolves a password
     *
     * <p>First tries to resolve the password as an environment variable.
     *
     * <p>Defaults to returning the original password if it is not a valid environment variable
     *
     * @param password the password
     * @return the resolved password
     */
    public static String resolve(String password) {
        // If the password is null or empty, return it as is
        if (password == null || password.trim().isEmpty()) {
            return password;
        }

        String environmentVariableName = null;

        // Check if the password starts with ${...} or $... to identify an environment variable
        if (password.startsWith("${") && password.endsWith("}") && password.length() > 3) {
            environmentVariableName = password.substring(2, password.length() - 1);
        } else if (password.startsWith("$") && password.length() > 1) {
            environmentVariableName = password.substring(1);
        }

        // If we have a valid environment variable name, check if it exists and is not empty
        if (isValidEnvironmentVariable(environmentVariableName)) {
            String environmentVariableValue = System.getenv(environmentVariableName);
            if (environmentVariableValue != null && !environmentVariableValue.trim().isEmpty()) {
                return environmentVariableValue;
            }
        }

        // If no valid environment variable was found, return the original password
        return password;
    }

    /**
     * Validates that the string is a valid environment variable name. Only allows letters, digits,
     * and underscores, and must not be null or empty.
     *
     * @param name the name
     * @return true if valid, else false
     */
    private static boolean isValidEnvironmentVariable(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Check if the name matches the environment variable pattern
        return ENVIRONMENT_VARIABLE_PATTERN.matcher(name).matches();
    }
}
