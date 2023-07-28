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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;

public class MessageDigestAuthenticatorTest extends BaseAuthenticatorTest {

    @Test
    public void test_lowerCase() throws Exception {
        String[] algorithms = new String[] {"SHA-1", "SHA-256", "SHA-512"};

        for (String algorithm : algorithms) {
            String hash = hash(algorithm, VALID_PASSWORD, SALT).toLowerCase();

            MessageDigestAuthenticator messageDigestAuthenticator =
                    new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

            for (String username : TEST_USERNAMES) {
                for (String password : TEST_PASSWORDS) {
                    boolean expectedIsAuthenticated =
                            VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                    boolean actualIsAuthenticated =
                            messageDigestAuthenticator.checkCredentials(username, password);
                    assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
                }
            }
        }
    }

    @Test
    public void test_upperCase() throws Exception {
        String[] algorithms = new String[] {"SHA-1", "SHA-256", "SHA-512"};

        for (String algorithm : algorithms) {
            String hash = hash(algorithm, VALID_PASSWORD, SALT).toUpperCase();

            MessageDigestAuthenticator messageDigestAuthenticator =
                    new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

            for (String username : TEST_USERNAMES) {
                for (String password : TEST_PASSWORDS) {
                    boolean expectedIsAuthenticated =
                            VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                    boolean actualIsAuthenticated =
                            messageDigestAuthenticator.checkCredentials(username, password);
                    assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
                }
            }
        }
    }

    private static String hash(String algorithm, String value, String salt)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest((salt + ":" + value).getBytes("UTF-8"));
        BigInteger number = new BigInteger(1, hashedBytes);
        return number.toString(16).toLowerCase();
    }
}
