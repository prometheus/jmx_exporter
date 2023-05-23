/*
 * Copyright (C) 2023 The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.http.authentication;

import io.prometheus.jmx.test.BaseTest;
import io.prometheus.jmx.test.TestArgument;
import org.antublue.test.engine.api.TestEngine;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

@TestEngine.BaseClass
public class BasicAuthenticationBaseTest extends BaseTest {

    protected final String VALID_USERNAME = "Prometheus";
    protected final String VALID_PASSWORD = "secret";
    protected final String[] TEST_USERNAMES = new String[] { VALID_USERNAME, "prometheus", "bad", "", null };
    protected final String[] TEST_PASSWORDS = new String[] { VALID_PASSWORD, "Secret", "bad", "", null };

    protected final static PBKDF2WithHmacTestArgumentFilter PBKDF2WITHHMAC_TEST_PARAMETER_FILTER =
            new PBKDF2WithHmacTestArgumentFilter();

    private static class PBKDF2WithHmacTestArgumentFilter implements Predicate<TestArgument> {

        private Set<String> filteredDockerImages;

        public PBKDF2WithHmacTestArgumentFilter() {
            filteredDockerImages = new HashSet<>();
            filteredDockerImages.add("ibmjava:8");
            filteredDockerImages.add("ibmjava:8-jre");
            filteredDockerImages.add("ibmjava:8-sdk");
            filteredDockerImages.add("ibmjava:8-sfj");
        }

        /**
         * Evaluates this predicate on the given argument.
         *
         * @param testArgument the input argument
         * @return {@code true} if the input argument matches the predicate,
         * otherwise {@code false}
         */
        @Override
        public boolean test(TestArgument testArgument) {
            return !filteredDockerImages.contains(testArgument.dockerImageName());
        }
    }
}
