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

import java.util.Optional;

/** Interface to implement VariableProvider */
public interface VariableProvider {

    /**
     * Checks whether this provider can handle the given variable specification.
     *
     * @param variableSpec the trimmed content inside ${...} or the raw literal
     * @return true if this provider supports the variable spec, else false
     */
    boolean supports(String variableSpec);

    /**
     * Tries to resolve the variable. Returns empty if it cannot.
     *
     * @param variableSpec the same string to supports()
     * @return an Optional containing the resolved variable, or empty to fall through
     */
    Optional<String> resolve(String variableSpec);
}
