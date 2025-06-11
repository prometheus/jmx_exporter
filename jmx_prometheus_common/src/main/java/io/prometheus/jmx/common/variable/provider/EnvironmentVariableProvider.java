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

package io.prometheus.jmx.common.variable.provider;

import io.prometheus.jmx.common.variable.VariableProvider;
import java.util.Optional;

/** A {@link VariableProvider} that tries to resolve the variable from an environment variable. */
public class EnvironmentVariableProvider implements VariableProvider {

    public static final String PREFIX = "env:";

    /** Constructor */
    public EnvironmentVariableProvider() {
        // INTENTIONALLY BLANK
    }

    @Override
    public boolean supports(String variableSpec) {
        return variableSpec != null && variableSpec.toLowerCase().startsWith(PREFIX);
    }

    @Override
    public Optional<String> resolve(String variableSpec) {
        String environmentVariableName = variableSpec.substring(PREFIX.length()).trim();

        // If the environment variable name is empty after trimming, return an empty Optional
        if (environmentVariableName.isEmpty()) {
            return Optional.empty();
        }

        // Retrieve the value of the environment variable
        String value = System.getenv(environmentVariableName);

        // If the value is null or empty, return an empty Optional
        if (value == null || value.trim().isEmpty()) {
            // Fall through silently
            return Optional.empty();
        }

        return Optional.of(value.trim());
    }
}
