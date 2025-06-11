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

package io.prometheus.jmx.common.password;

import static java.lang.String.format;

import io.prometheus.jmx.common.password.provider.EnvironmentVariablePasswordProvider;
import io.prometheus.jmx.common.password.provider.FilePasswordProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves passwords from multiple sources such as an environment variable, file, or literal. Uses
 * pluggable {@link PasswordProvider} instances.
 */
public final class PasswordResolver {

    private static final List<PasswordProvider> PASSWORD_PROVIDERS = new ArrayList<>();

    static {
        PASSWORD_PROVIDERS.add(new FilePasswordProvider());
        PASSWORD_PROVIDERS.add(new EnvironmentVariablePasswordProvider());
    }

    /** Constructor */
    private PasswordResolver() {
        // INTENTIONALLY BLANK
    }

    /**
     * Resolves a password string, which may be
     *
     * <p>${env:MY_ENV_VAR}
     *
     * <p>${file:/path/to/secret.txt}
     *
     * <p>literal-password
     *
     * @param password the input string
     * @return the resolved password value
     * @throws RuntimeException if resolution fails
     */
    public static String resolve(String password) {
        if (password == null || password.trim().isEmpty()) {
            return password;
        }

        // Get the specification from the password
        String spec = getSpec(password);

        // Loop through all registered PasswordProviders
        for (PasswordProvider provider : PASSWORD_PROVIDERS) {
            // Check if the provider supports the spec
            if (provider.supports(spec)) {
                // Resolve the password using the provider
                Optional<String> result = provider.resolve(spec);

                // If the provider successfully resolved the password
                if (result.isPresent()) {
                    // Return the resolved password
                    return result.get();
                }
            }
        }

        // If no provider could resolve the password, return the original password
        return password;
    }

    /**
     * Method to extract the string from the password specification.
     *
     * @param password the password specification
     * @return the spec
     */
    private static String getSpec(String password) {
        // Trim the password to remove leading and trailing whitespace
        String trimmed = password.trim();

        // If exactly "${}", return it as a literal
        if ("${}".equals(trimmed)) {
            return trimmed;
        }

        String spec;

        // Handle expressions like ${env:FOO} and ${file:/path/to/secret.txt}
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            spec = trimmed.substring(2, trimmed.length() - 1).trim();

            // If the spec is empty after trimming, throw an exception
            if (spec.isEmpty()) {
                throw new PasswordResolverException(
                        format("Invalid password format [%s]", password));
            }
        } else {
            spec = trimmed;
        }

        return spec;
    }
}
