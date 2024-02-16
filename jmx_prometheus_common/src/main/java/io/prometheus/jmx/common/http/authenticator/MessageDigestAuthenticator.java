/*
 * Copyright (C) 2022-2023 The Prometheus jmx_exporter Authors
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

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.util.Precondition;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Class to implement a username / salted message digest password BasicAuthenticator */
public class MessageDigestAuthenticator extends BasicAuthenticator {

    private static final int MAXIMUM_VALID_CACHE_SIZE_BYTES = 1000000; // 1 MB
    private static final int MAXIMUM_INVALID_CACHE_SIZE_BYTES = 10000000; // 10 MB

    private final String username;
    private final String passwordHash;
    private final String algorithm;
    private final String salt;
    private final CredentialsCache validCredentialsCache;
    private final CredentialsCache invalidCredentialsCache;

    /**
     * Constructor
     *
     * @param realm realm
     * @param username username
     * @param passwordHash passwordHash
     * @param algorithm algorithm
     * @param salt salt
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public MessageDigestAuthenticator(
            String realm, String username, String passwordHash, String algorithm, String salt)
            throws GeneralSecurityException {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(passwordHash);
        Precondition.notNullOrEmpty(algorithm);
        Precondition.notNullOrEmpty(salt);

        MessageDigest.getInstance(algorithm);

        this.username = username;
        this.passwordHash = passwordHash.toLowerCase().replace(":", "");
        this.algorithm = algorithm;
        this.salt = salt;
        this.validCredentialsCache = new CredentialsCache(MAXIMUM_VALID_CACHE_SIZE_BYTES);
        this.invalidCredentialsCache = new CredentialsCache(MAXIMUM_INVALID_CACHE_SIZE_BYTES);
    }

    /**
     * called for each incoming request to verify the given name and password in the context of this
     * Authenticator's realm. Any caching of credentials must be done by the implementation of this
     * method
     *
     * @param username the username from the request
     * @param password the password from the request
     * @return <code>true</code> if the credentials are valid, <code>false</code> otherwise.
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        Credentials credentials = new Credentials(username, password);
        if (validCredentialsCache.contains(credentials)) {
            return true;
        } else if (invalidCredentialsCache.contains(credentials)) {
            return false;
        }

        boolean isValid =
                this.username.equals(username)
                        && this.passwordHash.equals(
                                generatePasswordHash(algorithm, salt, password));

        if (isValid) {
            validCredentialsCache.add(credentials);
        } else {
            invalidCredentialsCache.add(credentials);
        }

        return isValid;
    }

    /**
     * Method to generate a hash based on the configured message digest algorithm
     *
     * @param algorithm algorithm
     * @param salt salt
     * @param password password
     * @return the hash
     */
    private static String generatePasswordHash(String algorithm, String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            return number.toString(16).toLowerCase();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
