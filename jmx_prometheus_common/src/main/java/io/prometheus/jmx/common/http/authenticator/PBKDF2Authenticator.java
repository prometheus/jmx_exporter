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

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.util.Precondition;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Class to implement a username / salted message digest password BasicAuthenticator */
public class PBKDF2Authenticator extends BasicAuthenticator {

    private static final int MAXIMUM_INVALID_CACHE_KEY_ENTRIES = 16;

    private final String username;
    private final String passwordHash;
    private final String algorithm;
    private final String salt;
    private final int iterations;
    private final int keyLength;
    private final Set<CacheKey> cacheKeys;
    private final LinkedList<CacheKey> invalidCacheKeys;

    /**
     * Constructor
     *
     * @param realm realm
     * @param username username
     * @param passwordHash passwordHash
     * @param algorithm algorithm
     * @param salt salt
     * @param iterations iterations
     * @param keyLength keyLength
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public PBKDF2Authenticator(
            String realm,
            String username,
            String passwordHash,
            String algorithm,
            String salt,
            int iterations,
            int keyLength)
            throws GeneralSecurityException {
        super(realm);

        Precondition.notNullOrEmpty(username);
        Precondition.notNullOrEmpty(passwordHash);
        Precondition.notNullOrEmpty(algorithm);
        Precondition.notNullOrEmpty(salt);
        Precondition.isGreaterThanOrEqualTo(iterations, 1);
        Precondition.isGreaterThanOrEqualTo(keyLength, 1);

        SecretKeyFactory.getInstance(algorithm);

        this.username = username;
        this.passwordHash = passwordHash.toLowerCase().replace(":", "");
        this.algorithm = algorithm;
        this.salt = salt;
        this.iterations = iterations;
        this.keyLength = keyLength;
        this.cacheKeys = Collections.synchronizedSet(new HashSet<>());
        this.invalidCacheKeys = new LinkedList<>();
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

        CacheKey cacheKey = new CacheKey(username, password);
        if (cacheKeys.contains(cacheKey)) {
            return true;
        } else {
            synchronized (invalidCacheKeys) {
                if (invalidCacheKeys.contains(cacheKey)) {
                    return false;
                }
            }
        }

        boolean isValid =
                this.username.equals(username)
                        && this.passwordHash.equals(
                                generatePasswordHash(
                                        algorithm, salt, iterations, keyLength, password));
        if (isValid) {
            cacheKeys.add(cacheKey);
        } else {
            synchronized (invalidCacheKeys) {
                invalidCacheKeys.add(cacheKey);
                if (invalidCacheKeys.size() > MAXIMUM_INVALID_CACHE_KEY_ENTRIES) {
                    invalidCacheKeys.removeFirst();
                }
            }
        }

        return isValid;
    }

    /**
     * Method to generate a hash based on the configured secret key algorithm
     *
     * @param algorithm algorithm
     * @param salt salt
     * @param iterations iterations
     * @param keyLength keyLength
     * @param password password
     * @return the hash
     */
    private static String generatePasswordHash(
            String algorithm, String salt, int iterations, int keyLength, String password) {
        try {
            PBEKeySpec pbeKeySpec =
                    new PBEKeySpec(
                            password.toCharArray(),
                            salt.getBytes(StandardCharsets.UTF_8),
                            iterations,
                            keyLength * 8);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            byte[] secretKeyBytes = secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
            return HexString.toHex(secretKeyBytes);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
