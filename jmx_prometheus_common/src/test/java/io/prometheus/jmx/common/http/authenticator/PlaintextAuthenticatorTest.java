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

package io.prometheus.jmx.common.http.authenticator;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.authenticator.PlaintextAuthenticator;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

public class PlaintextAuthenticatorTest extends BaseAuthenticatorTest {

    @Test
    public void test() {
        BasicAuthenticator plainTextAuthenticator = new PlaintextAuthenticator("/", VALID_USERNAME, VALID_PASSWORD);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated = VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated = plainTextAuthenticator.checkCredentials(username, password);
                assertThat(actualIsAuthenticated).isEqualTo(expectedIsAuthenticated);
            }
        }
    }

    @Test
    public void testCachedCredentials() {
        BasicAuthenticator plainTextAuthenticator = new PlaintextAuthenticator("/", VALID_USERNAME, VALID_PASSWORD);

        assertThat(plainTextAuthenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();

        assertThat(plainTextAuthenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();

        assertThat(plainTextAuthenticator.checkCredentials("bad", "bad")).isFalse();

        assertThat(plainTextAuthenticator.checkCredentials("bad", "bad")).isFalse();
    }

    @Test
    public void testCacheShortCircuitsVerification() throws Exception {
        BasicAuthenticator plainTextAuthenticator = new PlaintextAuthenticator("/", VALID_USERNAME, VALID_PASSWORD);

        assertThat(verificationCount(plainTextAuthenticator)).isZero();

        assertThat(plainTextAuthenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();
        assertThat(plainTextAuthenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();

        // The second identical valid login is served from the cache, so verification is not re-run.
        assertThat(verificationCount(plainTextAuthenticator)).isEqualTo(1);
    }

    @Test
    public void testInvalidCredentialsAreNotServedFromCache() throws Exception {
        BasicAuthenticator plainTextAuthenticator = new PlaintextAuthenticator("/", VALID_USERNAME, VALID_PASSWORD);

        assertThat(plainTextAuthenticator.checkCredentials("bad", "bad")).isFalse();
        assertThat(plainTextAuthenticator.checkCredentials("bad", "bad")).isFalse();
        assertThat(plainTextAuthenticator.checkCredentials("bad", "worse")).isFalse();

        // Each distinct wrong credential is a cache miss and must be verified.
        assertThat(verificationCount(plainTextAuthenticator)).isEqualTo(3);
    }

    private static int verificationCount(Object authenticator) throws Exception {
        Field field = authenticator.getClass().getDeclaredField("verificationCount");
        field.setAccessible(true);
        return (int) field.get(authenticator);
    }
}
