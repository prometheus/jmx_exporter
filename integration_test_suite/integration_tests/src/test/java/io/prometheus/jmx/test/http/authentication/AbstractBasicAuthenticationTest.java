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

import io.prometheus.jmx.test.AbstractTest;
import io.prometheus.jmx.test.support.TestArguments;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractBasicAuthenticationTest extends AbstractTest {

    protected static final String VALID_USERNAME = "Prometheus";

    protected static final String VALID_PASSWORD = "secret";

    protected static final String[] INVALID_USERNAMES =
            new String[] {"prometheus", "bad", "", null};

    protected static final String[] INVALID_PASSWORDS = new String[] {"Secret", "bad", "", null};

    protected static final PBKDF2WithHmacTestArgumentFilter PBKDF2_WITH_MAC_TEST_ARGUMENT_FILTER =
            new PBKDF2WithHmacTestArgumentFilter();

    /** Class to implement a PBKDF2WithHmacTestArgumentFilter */
    protected static class PBKDF2WithHmacTestArgumentFilter implements Predicate<TestArguments> {

        private final Set<String> filteredDockerImages;

        /** Constructor */
        public PBKDF2WithHmacTestArgumentFilter() {
            // Filter out Docker image names that don't support PBKDF2 with HMAC
            filteredDockerImages = new HashSet<>();
            filteredDockerImages.add("ibmjava:8");
            filteredDockerImages.add("ibmjava:8-jre");
            filteredDockerImages.add("ibmjava:8-sdk");
            filteredDockerImages.add("ibmjava:8-sfj");
        }

        /**
         * Evaluates this predicate on the given argument.
         *
         * @param testArguments the input argument
         * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
         */
        @Override
        public boolean test(TestArguments testArguments) {
            return !filteredDockerImages.contains(testArguments.getDockerImageName());
        }
    }

    /**
     * Method to create a Collection of AuthenticationTestCredentials
     *
     * @return a Collection of AuthenticationTestCredentials
     */
    public static Collection<AuthenticationTestCredentials> getAuthenticationTestCredentials() {
        Collection<AuthenticationTestCredentials> collection = new ArrayList<>();
        collection.add(AuthenticationTestCredentials.of(VALID_USERNAME, VALID_PASSWORD, true));

        for (String username : INVALID_USERNAMES) {
            for (String password : INVALID_PASSWORDS) {
                collection.add(AuthenticationTestCredentials.of(username, password, false));
            }
        }

        return collection;
    }

    /** Class to implement AuthenticationTestCredentials */
    public static class AuthenticationTestCredentials {

        private String username;
        private String password;
        private boolean isValid;

        /**
         * Constructor
         *
         * @param username
         * @param password
         * @param isValid
         */
        private AuthenticationTestCredentials(String username, String password, boolean isValid) {
            this.username = username;
            this.password = password;
            this.isValid = isValid;
        }

        /**
         * Method to get the username
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Method to get the password
         *
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Method to return if the credentials are valid
         *
         * @return true if the credentials are valid, else false
         */
        public boolean isValid() {
            return isValid;
        }

        /**
         * Method to create an AuthenticationTestCredentials
         *
         * @param username username
         * @param password password
         * @param isValid isValid
         * @return an AuthenticationTestCredentials
         */
        public static AuthenticationTestCredentials of(
                String username, String password, boolean isValid) {
            return new AuthenticationTestCredentials(username, password, isValid);
        }
    }
}
