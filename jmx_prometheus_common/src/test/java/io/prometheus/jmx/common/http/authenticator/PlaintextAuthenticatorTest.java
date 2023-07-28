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

import static org.junit.Assert.assertEquals;

import com.sun.net.httpserver.BasicAuthenticator;
import org.junit.Test;

public class PlaintextAuthenticatorTest extends BaseAuthenticatorTest {

    @Test
    public void test() {
        BasicAuthenticator plainTextAuthenticator =
                new PlaintextAuthenticator("/", VALID_USERNAME, VALID_PASSWORD);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        plainTextAuthenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }
}
