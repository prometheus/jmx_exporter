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

package io.prometheus.jmx.common.variable;

import static java.lang.String.format;

import io.prometheus.jmx.common.variable.provider.EnvironmentVariableProvider;
import io.prometheus.jmx.common.variable.provider.FileVariableProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Class to resolves variables from multiple sources such as an environment variable, file, or
 * literal.
 *
 * <p>Uses pluggable {@link VariableProvider} instances.
 */
public final class VariableResolver {

    private static final List<VariableProvider> VARIABLE_PROVIDERS = new ArrayList<>();

    static {
        VARIABLE_PROVIDERS.add(new FileVariableProvider());
        VARIABLE_PROVIDERS.add(new EnvironmentVariableProvider());
    }

    /** Constructor */
    private VariableResolver() {
        // INTENTIONALLY BLANK
    }

    /**
     * Resolves a variable string, which may be...
     *
     * <p>${env:MY_ENV_VAR}
     *
     * <p>${file:/path/to/secret.txt}
     *
     * <p>literal-value
     *
     * @param variable the variable
     * @return the resolved variable value
     */
    public static String resolve(String variable) {
        // If the variable is null or empty, return the variable
        if (variable == null || variable.trim().isEmpty()) {
            return variable;
        }

        // Get the specification from the variable
        String variableSpec = getVariableSpec(variable);

        // Loop through all registered variable providers
        for (VariableProvider variableProvider : VARIABLE_PROVIDERS) {
            // Check if the provider supports the spec
            if (variableProvider.supports(variableSpec)) {
                // Resolve the variable using the provider
                Optional<String> result = variableProvider.resolve(variableSpec);

                // If the provider successfully resolved the variable, return the variable
                if (result.isPresent()) {
                    return result.get();
                }
            }
        }

        // If no provider could resolve the variable, return the original value
        return variable;
    }

    /**
     * Method to extract the string from the variable specification.
     *
     * @param variable the variable specification
     * @return the spec
     */
    private static String getVariableSpec(String variable) {
        // Trim the variable to remove leading and trailing whitespace
        String trimmedVariable = variable.trim();

        // If exactly "${}", return it as a literal
        if ("${}".equals(trimmedVariable)) {
            return trimmedVariable;
        }

        String variableSpec;

        // Handle expressions like ${env:FOO}, ${file:/path/to/secret.txt}, etc.
        if (trimmedVariable.startsWith("${") && trimmedVariable.endsWith("}")) {
            variableSpec = trimmedVariable.substring(2, trimmedVariable.length() - 1).trim();

            // If the spec is empty after trimming, throw an exception
            if (variableSpec.isEmpty()) {
                throw new VariableResolverException(
                        format("Invalid variable format [%s]", variable));
            }
        } else {
            variableSpec = trimmedVariable;
        }

        return variableSpec;
    }
}
