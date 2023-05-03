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

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class MessageDigestAuthenticatorTest {

    @Test
    public void test() throws Exception {
        String[] algorithms = new String[] { "SHA-1", "SHA-256", "SHA-512" };
        String[] usernames = new String[] { "Prometheus", "prometheus", "bad", "", null };
        String[] passwords = new String[] { "secret", "Secret", "bad", "", null };

        for (String algorithm : algorithms) {
            String salt = UUID.randomUUID().toString();
            String hash = hash(algorithm, "secret", salt);

            MessageDigestAuthenticator messageDigestAuthenticator =
                    new MessageDigestAuthenticator("/", "Prometheus", hash, algorithm, salt);

            for (String username : usernames) {
                for (String password : passwords) {
                    boolean expectedIsAuthenticated = false;
                    if ("Prometheus".equals(username) && "secret".equals(password)) {
                        expectedIsAuthenticated = true;
                    }
                    boolean actualIsAuthenticated = messageDigestAuthenticator.checkCredentials(username, password);
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
        StringBuilder hexString = new StringBuilder(number.toString(16));
        int digestLength = 2 * digest.getDigestLength();
        while (hexString.length() < digestLength) {
            hexString.insert(0, '0');
        }
        return hexString.toString().toLowerCase();
    }
}
