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

package io.prometheus.jmx.common.http.authenticator;

public class BaseAuthenticatorTest {

    protected final String VALID_USERNAME = "Prometheus";
    protected final String VALID_PASSWORD = "secret";

    protected static final String[] TEST_USERNAMES =
            new String[] {"Prometheus", "prometheus", "bad", "", null};
    protected static final String[] TEST_PASSWORDS =
            new String[] {"secret", "Secret", "bad", "", null};

    protected static final String SALT = "98LeBWIjca";
}
