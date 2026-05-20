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

import io.prometheus.jmx.common.authenticator.MessageDigestAuthenticator;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;

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
                    boolean actualIsAuthenticated = messageDigestAuthenticator.checkCredentials(username, password);
                    assertThat(actualIsAuthenticated).isEqualTo(expectedIsAuthenticated);
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
                    boolean actualIsAuthenticated = messageDigestAuthenticator.checkCredentials(username, password);
                    assertThat(actualIsAuthenticated).isEqualTo(expectedIsAuthenticated);
                }
            }
        }
    }

    @Test
    public void testCheckCredentialsWithNullUsername() throws Exception {
        String algorithm = "SHA-256";
        String hash = hash(algorithm, VALID_PASSWORD, SALT).toLowerCase();
        MessageDigestAuthenticator authenticator =
                new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

        assertThat(authenticator.checkCredentials(null, VALID_PASSWORD)).isFalse();
    }

    @Test
    public void testCheckCredentialsWithNullPassword() throws Exception {
        String algorithm = "SHA-256";
        String hash = hash(algorithm, VALID_PASSWORD, SALT).toLowerCase();
        MessageDigestAuthenticator authenticator =
                new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

        assertThat(authenticator.checkCredentials(VALID_USERNAME, null)).isFalse();
    }

    @Test
    public void testGeneratePasswordHashViaReflection() throws Exception {
        Method method = MessageDigestAuthenticator.class.getDeclaredMethod(
                "generatePasswordHash", String.class, String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(null, "SHA-256", SALT, VALID_PASSWORD);

        assertThat(result).isNotNull();
        assertThat(result).matches("[0-9a-f]+");
    }

    @Test
    public void testToLowerCaseHexadecimalViaReflection() throws Exception {
        Method method = MessageDigestAuthenticator.class.getDeclaredMethod("toLowerCaseHexadecimal", byte[].class);
        method.setAccessible(true);

        byte[] input = new byte[] {0x00, 0x0f, (byte) 0xff, (byte) 0xab};
        String result = (String) method.invoke(null, (Object) input);

        assertThat(result).isEqualTo("000fffab");
    }

    @Test
    public void testInvalidCredentialsAreCached() throws Exception {
        String algorithm = "SHA-256";
        String hash = hash(algorithm, VALID_PASSWORD, SALT).toLowerCase();
        MessageDigestAuthenticator authenticator =
                new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

        assertThat(authenticator.checkCredentials(VALID_USERNAME, "wrong")).isFalse();
        assertThat(authenticator.checkCredentials(VALID_USERNAME, "wrong")).isFalse();
    }

    @Test
    public void testValidCredentialsThenInvalidCached() throws Exception {
        String algorithm = "SHA-256";
        String hash = hash(algorithm, VALID_PASSWORD, SALT).toLowerCase();
        MessageDigestAuthenticator authenticator =
                new MessageDigestAuthenticator("/", VALID_USERNAME, hash, algorithm, SALT);

        assertThat(authenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();
        assertThat(authenticator.checkCredentials(VALID_USERNAME, "wrong")).isFalse();
        assertThat(authenticator.checkCredentials(VALID_USERNAME, VALID_PASSWORD))
                .isTrue();
    }

    private static String hash(String algorithm, String value, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hashedBytes = digest.digest((salt + ":" + value).getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, hashedBytes);
        return number.toString(16).toLowerCase();
    }
}
