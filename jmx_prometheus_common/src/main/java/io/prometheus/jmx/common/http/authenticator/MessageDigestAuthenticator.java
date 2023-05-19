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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class to implement a username / salted message digest password BasicAuthenticator
 */
public class MessageDigestAuthenticator extends BasicAuthenticator {

    private final String username;
    private final String hash;
    private final String algorithm;
    private final String salt;
    private final Set<CacheKey> cacheKeys;

    /**
     * Constructor
     *
     * @param realm realm
     * @param username username
     * @param hash hash
     * @param algorithm algorithm
     * @param salt salt
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public MessageDigestAuthenticator(String realm, String username, String hash, String algorithm, String salt)
            throws NoSuchAlgorithmException {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(hash);
        Precondition.notNullOrEmpty(algorithm);
        Precondition.notNullOrEmpty(salt);

        MessageDigest.getInstance(algorithm);

        this.username = username;
        this.hash = hash.toUpperCase();
        this.algorithm = algorithm;
        this.salt = salt;
        this.cacheKeys = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * called for each incoming request to verify the
     * given name and password in the context of this
     * Authenticator's realm. Any caching of credentials
     * must be done by the implementation of this method
     *
     * @param username the username from the request
     * @param password the password from the request
     * @return <code>true</code> if the credentials are valid,
     * <code>false</code> otherwise.
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        CacheKey cacheKey = new CacheKey(username, password);
        if (cacheKeys.contains(cacheKey)) {
            return true;
        }

        boolean isValid = this.username.equals(username)
                && this.hash.equals(generateHash(algorithm, salt, password));
        if (isValid) {
            cacheKeys.add(cacheKey);
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
    private static String generateHash(String algorithm, String salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            return number.toString(16).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
