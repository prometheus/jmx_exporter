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

package io.prometheus.jmx.variable;

/** Class to resolves variables. */
public final class VariableResolver {

    /** Constructor */
    private VariableResolver() {
        // INTENTIONALLY BLANK
    }

    /**
     * Resolves a variable.
     *
     * @param variable the variable
     * @return the resolved variable value
     */
    public static String resolveVariable(String variable) {
        // If the variable is null or empty, return the variable
        if (variable == null || variable.trim().isEmpty()) {
            return variable;
        }

        // Trim the variable to remove leading and trailing whitespace
        String trimmedVariable = variable.trim();

        // If exactly "${}", return it as a literal
        if ("${}".equals(trimmedVariable)) {
            return variable;
        }

        // Handle expressions like ${FOO}
        if (trimmedVariable.startsWith("${") && trimmedVariable.endsWith("}")) {
            return resolveEnvironmentVariable(trimmedVariable);
        }

        return variable;
    }

    /**
     * Resolves an environment variable.
     *
     * @param variable the variable
     * @return the resolved variable value, or null if it can't be resolved
     */
    private static String resolveEnvironmentVariable(String variable) {
        // Remove the ${ and } from the variable
        String environmentVariableName = variable.substring(2, variable.length() - 1).trim();

        // If the environment variable name is empty after trimming, return null
        if (environmentVariableName.isEmpty()) {
            throw new VariableResolverException(
                    "Invalid environment variable name '" + variable + "'");
        }

        // Retrieve the value of the environment variable
        String value = System.getenv(environmentVariableName);

        // If the value is null, throw an exception
        if (value == null) {
            throw new VariableResolverException(
                    "Environment variable '" + variable + "' not defined");
        }

        // Trim the value to remove leading and trailing whitespace
        value = value.trim();

        // If the value is empty or only whitespace, throw an exception
        if (value.isEmpty()) {
            throw new VariableResolverException("Environment variable '" + variable + "' is empty");
        }

        return value;
    }
}
