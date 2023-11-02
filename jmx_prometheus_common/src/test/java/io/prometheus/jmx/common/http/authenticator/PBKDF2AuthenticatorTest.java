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

import org.junit.Test;

public class PBKDF2AuthenticatorTest extends BaseAuthenticatorTest {

    @Test
    public void testPBKDF2WithHmacSHA1() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "17:87:CA:B9:14:73:60:36:8B:20:82:87:92:58:43:B8:A3:85:66:BC:C1:6D:C3:31:6C:1D:47:48:C7:F2:E4:1D:96"
                    + ":00:11:F8:4D:94:63:2F:F2:7A:F0:3B:72:63:16:5D:EF:5C:97:CC:EC:59:CB:18:4A:AA:F5:23:63:0B:6E:3B:65"
                    + ":E0:72:6E:69:7D:EB:83:05:05:E5:D6:F2:19:99:49:3F:89:DA:DE:83:D7:2B:5B:7D:C9:56:B4:F2:F6:A5:61:29"
                    + ":29:ED:DF:4C:4E:8D:EA:DF:47:A2:B0:89:11:86:D4:77:A1:02:E9:0C:26:A4:1E:2A:C1:A8:71:E0:93:8F:A4";

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA1_lowerCase() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "17:87:CA:B9:14:73:60:36:8B:20:82:87:92:58:43:B8:A3:85:66:BC:C1:6D:C3:31:6C:1D:47:48:C7:F2:E4:1D:96"
                    + ":00:11:F8:4D:94:63:2F:F2:7A:F0:3B:72:63:16:5D:EF:5C:97:CC:EC:59:CB:18:4A:AA:F5:23:63:0B:6E:3B:65"
                    + ":E0:72:6E:69:7D:EB:83:05:05:E5:D6:F2:19:99:49:3F:89:DA:DE:83:D7:2B:5B:7D:C9:56:B4:F2:F6:A5:61:29"
                    + ":29:ED:DF:4C:4E:8D:EA:DF:47:A2:B0:89:11:86:D4:77:A1:02:E9:0C:26:A4:1E:2A:C1:A8:71:E0:93:8F:A4";

        hash = hash.toLowerCase();

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA1_withoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "17:87:CA:B9:14:73:60:36:8B:20:82:87:92:58:43:B8:A3:85:66:BC:C1:6D:C3:31:6C:1D:47:48:C7:F2:E4:1D:96"
                    + ":00:11:F8:4D:94:63:2F:F2:7A:F0:3B:72:63:16:5D:EF:5C:97:CC:EC:59:CB:18:4A:AA:F5:23:63:0B:6E:3B:65"
                    + ":E0:72:6E:69:7D:EB:83:05:05:E5:D6:F2:19:99:49:3F:89:DA:DE:83:D7:2B:5B:7D:C9:56:B4:F2:F6:A5:61:29"
                    + ":29:ED:DF:4C:4E:8D:EA:DF:47:A2:B0:89:11:86:D4:77:A1:02:E9:0C:26:A4:1E:2A:C1:A8:71:E0:93:8F:A4";

        hash = hash.replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA1_lowerCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "17:87:CA:B9:14:73:60:36:8B:20:82:87:92:58:43:B8:A3:85:66:BC:C1:6D:C3:31:6C:1D:47:48:C7:F2:E4:1D:96"
                    + ":00:11:F8:4D:94:63:2F:F2:7A:F0:3B:72:63:16:5D:EF:5C:97:CC:EC:59:CB:18:4A:AA:F5:23:63:0B:6E:3B:65"
                    + ":E0:72:6E:69:7D:EB:83:05:05:E5:D6:F2:19:99:49:3F:89:DA:DE:83:D7:2B:5B:7D:C9:56:B4:F2:F6:A5:61:29"
                    + ":29:ED:DF:4C:4E:8D:EA:DF:47:A2:B0:89:11:86:D4:77:A1:02:E9:0C:26:A4:1E:2A:C1:A8:71:E0:93:8F:A4";

        hash = hash.toLowerCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA1_upperCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA1";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "17:87:CA:B9:14:73:60:36:8B:20:82:87:92:58:43:B8:A3:85:66:BC:C1:6D:C3:31:6C:1D:47:48:C7:F2:E4:1D:96"
                    + ":00:11:F8:4D:94:63:2F:F2:7A:F0:3B:72:63:16:5D:EF:5C:97:CC:EC:59:CB:18:4A:AA:F5:23:63:0B:6E:3B:65"
                    + ":E0:72:6E:69:7D:EB:83:05:05:E5:D6:F2:19:99:49:3F:89:DA:DE:83:D7:2B:5B:7D:C9:56:B4:F2:F6:A5:61:29"
                    + ":29:ED:DF:4C:4E:8D:EA:DF:47:A2:B0:89:11:86:D4:77:A1:02:E9:0C:26:A4:1E:2A:C1:A8:71:E0:93:8F:A4";

        hash = hash.toUpperCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB"
                    + ":70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7"
                    + ":9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29"
                    + ":F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B";

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256_lowerCase() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB"
                    + ":70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7"
                    + ":9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29"
                    + ":F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B";

        hash = hash.toLowerCase();

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256_withoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB"
                    + ":70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7"
                    + ":9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29"
                    + ":F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B";

        hash = hash.replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256_lowerCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB"
                    + ":70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7"
                    + ":9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29"
                    + ":F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B";

        hash = hash.toLowerCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA256_upperCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA256";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "B6:9C:5C:8A:10:3E:41:7B:BA:18:FC:E1:F2:0C:BC:D9:65:70:D3:53:AB:97:EE:2F:3F:A8:88:AF:43:EA:E6:D7:FB"
                    + ":70:14:23:F9:51:29:5C:3A:9F:65:C3:20:EE:09:C9:C6:8A:B7:D3:0A:E1:F3:10:2B:9B:36:3F:1F:B6:1D:52:A7"
                    + ":9C:CB:AD:55:25:46:C5:73:09:6C:38:9C:F2:FD:82:7F:90:E5:31:EF:7E:3E:6B:B2:0C:38:77:23:EC:3A:CF:29"
                    + ":F7:E5:4D:4E:CC:35:7A:C2:E5:CB:E3:B3:E5:09:2B:CC:B9:40:26:A4:28:E9:5F:2D:18:B2:14:41:E7:4D:5B";

        hash = hash.toUpperCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "07:6F:E2:27:9B:CA:48:66:9B:13:9E:02:9C:AE:FC:E4:1A:2F:0F:E6:48:A3:FF:8E:D2:30:59:68:12:A6:29:34:FC:99:29:8A:98:65:AE:4B:05:7C:B6:83:A4:83:C0:32:E4:90:61:1D:DD:2E:53:17:01:FF:6A:64:48:B2:AA:22:DE:B3:BC:56:08:C6:66:EC:98:F8:96:8C:1B:DA:B2:F2:2A:6C:22:8E:19:CC:B2:62:55:3E:BE:DC:C7:58:36:9D:92:CF:D7:D2:A1:6D:8F:DC:DE:8E:E9:36:D4:E7:2D:0A:6D:A1:B8:56:0A:53:BB:17:E2:D5:DE:A0:48:51:FC:33";

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512_lowerCase() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "07:6F:E2:27:9B:CA:48:66:9B:13:9E:02:9C:AE:FC:E4:1A:2F:0F:E6:48:A3:FF:8E:D2:30:59:68:12:A6:29:34:FC:99:29:8A:98:65:AE:4B:05:7C:B6:83:A4:83:C0:32:E4:90:61:1D:DD:2E:53:17:01:FF:6A:64:48:B2:AA:22:DE:B3:BC:56:08:C6:66:EC:98:F8:96:8C:1B:DA:B2:F2:2A:6C:22:8E:19:CC:B2:62:55:3E:BE:DC:C7:58:36:9D:92:CF:D7:D2:A1:6D:8F:DC:DE:8E:E9:36:D4:E7:2D:0A:6D:A1:B8:56:0A:53:BB:17:E2:D5:DE:A0:48:51:FC:33";

        hash = hash.toLowerCase();

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512_withoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "07:6F:E2:27:9B:CA:48:66:9B:13:9E:02:9C:AE:FC:E4:1A:2F:0F:E6:48:A3:FF:8E:D2:30:59:68:12:A6:29:34:FC:99:29:8A:98:65:AE:4B:05:7C:B6:83:A4:83:C0:32:E4:90:61:1D:DD:2E:53:17:01:FF:6A:64:48:B2:AA:22:DE:B3:BC:56:08:C6:66:EC:98:F8:96:8C:1B:DA:B2:F2:2A:6C:22:8E:19:CC:B2:62:55:3E:BE:DC:C7:58:36:9D:92:CF:D7:D2:A1:6D:8F:DC:DE:8E:E9:36:D4:E7:2D:0A:6D:A1:B8:56:0A:53:BB:17:E2:D5:DE:A0:48:51:FC:33";

        hash = hash.replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512_lowerCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "07:6F:E2:27:9B:CA:48:66:9B:13:9E:02:9C:AE:FC:E4:1A:2F:0F:E6:48:A3:FF:8E:D2:30:59:68:12:A6:29:34:FC:99:29:8A:98:65:AE:4B:05:7C:B6:83:A4:83:C0:32:E4:90:61:1D:DD:2E:53:17:01:FF:6A:64:48:B2:AA:22:DE:B3:BC:56:08:C6:66:EC:98:F8:96:8C:1B:DA:B2:F2:2A:6C:22:8E:19:CC:B2:62:55:3E:BE:DC:C7:58:36:9D:92:CF:D7:D2:A1:6D:8F:DC:DE:8E:E9:36:D4:E7:2D:0A:6D:A1:B8:56:0A:53:BB:17:E2:D5:DE:A0:48:51:FC:33";

        hash = hash.toLowerCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }

    @Test
    public void testPBKDF2WithHmacSHA512_upperCaseWithoutColon() throws Exception {
        String algorithm = "PBKDF2WithHmacSHA512";
        int iterations = 1000;
        int keyLength = 128;
        String hash =
                "07:6F:E2:27:9B:CA:48:66:9B:13:9E:02:9C:AE:FC:E4:1A:2F:0F:E6:48:A3:FF:8E:D2:30:59:68:12:A6:29:34:FC:99:29:8A:98:65:AE:4B:05:7C:B6:83:A4:83:C0:32:E4:90:61:1D:DD:2E:53:17:01:FF:6A:64:48:B2:AA:22:DE:B3:BC:56:08:C6:66:EC:98:F8:96:8C:1B:DA:B2:F2:2A:6C:22:8E:19:CC:B2:62:55:3E:BE:DC:C7:58:36:9D:92:CF:D7:D2:A1:6D:8F:DC:DE:8E:E9:36:D4:E7:2D:0A:6D:A1:B8:56:0A:53:BB:17:E2:D5:DE:A0:48:51:FC:33";

        hash = hash.toUpperCase().replace(":", "");

        PBKDF2Authenticator PBKDF2Authenticator =
                new PBKDF2Authenticator(
                        "/", VALID_USERNAME, hash, algorithm, SALT, iterations, keyLength);

        for (String username : TEST_USERNAMES) {
            for (String password : TEST_PASSWORDS) {
                boolean expectedIsAuthenticated =
                        VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
                boolean actualIsAuthenticated =
                        PBKDF2Authenticator.checkCredentials(username, password);
                assertEquals(expectedIsAuthenticated, actualIsAuthenticated);
            }
        }
    }
}
