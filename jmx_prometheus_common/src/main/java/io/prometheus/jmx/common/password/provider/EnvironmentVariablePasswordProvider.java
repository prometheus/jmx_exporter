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

package io.prometheus.jmx.common.password.provider;

import io.prometheus.jmx.common.password.PasswordProvider;
import java.util.Optional;

/** A PasswordProvider that tries to resolve the password from an environment variable. */
public class EnvironmentVariablePasswordProvider implements PasswordProvider {

    @Override
    public boolean supports(String spec) {
        return spec.toLowerCase().startsWith("env:");
    }

    @Override
    public Optional<String> resolve(String spec) {
        String environmentVariableName = spec.substring("env:".length()).trim();

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
