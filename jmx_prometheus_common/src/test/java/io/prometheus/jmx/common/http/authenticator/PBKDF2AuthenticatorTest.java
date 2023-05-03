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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PBKDF2AuthenticatorTest {

    @Test
    public void testPBKDF2WithHmacSHA1() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        String salt = "PvrLg8tJphqTM8286VfH2w==";
        int iterations = 1000;
        int keyLength = 128;

        String hash = PBKDF2Authenticator.generateHash(algorithm, salt, iterations, keyLength, "secret");
        String[] usernames = new String[] { "Prometheus", "prometheus", "bad", "", null };
        String[] passwords = new String[] { "secret", "Secret", "bad", "", null };

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/",
                        "Prometheus",
                        hash,
                        algorithm,
                        salt,
                        iterations,
                        keyLength);

        for (String username : usernames) {
            for (String password : passwords) {
                boolean expectedIsAuthenticated = false;
                if ("Prometheus".equals(username) && "secret".equals(password)) {
                    expectedIsAuthenticated = true;
                }
                boolean actualIsAuthenticated = PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        String salt = "PvrLg8tJphqTM8286VfH2w==";
        int iterations = 1000;
        int keyLength = 256;

        String hash = PBKDF2Authenticator.generateHash(algorithm, salt, iterations, keyLength, "secret");
        String[] usernames = new String[] { "Prometheus", "prometheus", "bad", "", null };
        String[] passwords = new String[] { "secret", "Secret", "bad", "", null };

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/",
                        "Prometheus",
                        hash,
                        algorithm,
                        salt,
                        iterations,
                        keyLength);

        for (String username : usernames) {
            for (String password : passwords) {
                boolean expectedIsAuthenticated = false;
                if ("Prometheus".equals(username) && "secret".equals(password)) {
                    expectedIsAuthenticated = true;
                }
                boolean actualIsAuthenticated = PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        String salt = "PvrLg8tJphqTM8286VfH2w==";
        int iterations = 1000;
        int keyLength = 512;

        String hash = PBKDF2Authenticator.generateHash(algorithm, salt, iterations, keyLength, "secret");
        String[] usernames = new String[] { "Prometheus", "prometheus", "bad", "", null };
        String[] passwords = new String[] { "secret", "Secret", "bad", "", null };

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/",
                        "Prometheus",
                        hash,
                        algorithm,
                        salt,
                        iterations,
                        keyLength);

        for (String username : usernames) {
            for (String password : passwords) {
                boolean expectedIsAuthenticated = false;
                if ("Prometheus".equals(username) && "secret".equals(password)) {
                    expectedIsAuthenticated = true;
                }
                boolean actualIsAuthenticated = PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }
}
