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

package io.prometheus.jmx.common.authenticator;

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.jmx.common.util.Precondition;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Basic authenticator that validates credentials using PBKDF2 password hashing.
 *
 * <p>Supports PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA256, and PBKDF2WithHmacSHA512 algorithms.
 * This is the most secure authentication method available, recommended for production use.
 *
 * <p>Each uncached non-null authentication attempt derives a candidate PBKDF2 hash and compares
 * both the presented username and derived password hash using constant-time equality checks.
 *
 * <p>Valid credentials are cached using a Caffeine-backed cache with a maximum credential size
 * of 5 KiB and an approximately 500 KiB maximum cache weight. Invalid credentials are not
 * cached to avoid caching password guesses against expensive password verification.
 *
 * <p>Thread-safety: This class is thread-safe. All configuration state is immutable after
 * construction, and credential verification uses constant-time comparisons.
 *
 * @see PlaintextAuthenticator
 * @see MessageDigestAuthenticator
 */
public class PBKDF2Authenticator extends BasicAuthenticator {

    /**
     * Maximum size for a single cached credential value in bytes (5 KiB).
     */
    private static final int MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES = CredentialsCache.DEFAULT_MAX_VALUE_SIZE_BYTES;

    /**
     * Maximum number of entries per credential cache.
     */
    private static final int MAXIMUM_CREDENTIAL_CACHE_ENTRIES = CredentialsCache.DEFAULT_MAX_ENTRIES;

    /**
     * Hexadecimal characters for converting bytes to hex strings.
     */
    private static final char[] HEXADECIMAL_CHARACTERS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    /**
     * The expected username for authentication, encoded as UTF-8 bytes for constant-time comparison.
     */
    private final byte[] usernameBytes;

    /**
     * The expected password hash for authentication (stored as bytes for constant-time comparison).
     */
    private final byte[] passwordHashBytes;

    /**
     * Per-thread PBKDF2 secret key factory.
     */
    private final ThreadLocal<SecretKeyFactory> secretKeyFactory;

    /**
     * The salt used in password hashing, encoded as UTF-8 bytes.
     */
    private final byte[] saltBytes;

    /**
     * The number of iterations for key derivation.
     */
    private final int iterations;

    /**
     * The derived key length in bits.
     */
    private final int derivedKeyLengthBits;

    /**
     * Cache for valid credentials.
     */
    private final CredentialsCache validCredentialsCache;

    /**
     * Constructs a PBKDF2 authenticator with the specified parameters.
     *
     * @param realm the HTTP authentication realm, must not be {@code null} or blank
     * @param username the expected username, must not be {@code null} or blank
     * @param passwordHash the expected password hash, must not be {@code null} or blank
     * @param algorithm the PBKDF2 algorithm, must not be {@code null} or blank
     * @param salt the salt used in hashing, must not be {@code null} or blank
     * @param iterations the number of iterations, must be at least 1
     * @param keyLength the key length in bits, must be at least 1
     * @throws GeneralSecurityException if the algorithm is not supported
     * @throws NullPointerException if any string parameter is {@code null}
     * @throws IllegalArgumentException if any string parameter is blank or if iterations/keyLength
     *     is invalid
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

        this.passwordHashBytes = hexStringToByteArray(passwordHash.toLowerCase().replace(":", ""));
        this.usernameBytes = username.getBytes(StandardCharsets.UTF_8);
        this.secretKeyFactory = ThreadLocal.withInitial(() -> createSecretKeyFactory(algorithm));
        this.saltBytes = salt.getBytes(StandardCharsets.UTF_8);
        this.iterations = iterations;
        this.derivedKeyLengthBits = calculateDerivedKeyLengthBits(this.passwordHashBytes, keyLength);
        this.validCredentialsCache =
                new CredentialsCache(MAXIMUM_CREDENTIAL_VALUE_SIZE_BYTES, MAXIMUM_CREDENTIAL_CACHE_ENTRIES);
    }

    /**
     * Validates the presented credentials against the configured username and PBKDF2 password hash.
     *
     * <p>Both the username and the candidate password hash are compared using constant-time
     * equality checks via {@link MessageDigest#isEqual(byte[], byte[])} to prevent timing
     * side-channel attacks.
     *
     * @param username the presented username, may be {@code null}
     * @param password the presented password, may be {@code null}
     * @return {@code true} if both username and password match, {@code false} if either is
     *     {@code null} or they do not match
     */
    @Override
    public boolean checkCredentials(String username, String password) {
        if (username == null || password == null) {
            return false;
        }

        Credentials credentials = new Credentials(username, password);
        if (validCredentialsCache.contains(credentials)) {
            return true;
        }

        byte[] candidateHashBytes = generatePasswordHashBytes(
                secretKeyFactory.get(), saltBytes, iterations, derivedKeyLengthBits, password);
        boolean usernameMatches = MessageDigest.isEqual(this.usernameBytes, username.getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = MessageDigest.isEqual(this.passwordHashBytes, candidateHashBytes);
        boolean isValid = usernameMatches & passwordMatches;

        if (isValid) {
            validCredentialsCache.add(credentials);
        }

        return isValid;
    }

    /**
     * Generates a password hash using PBKDF2 key derivation.
     *
     * @param algorithm the PBKDF2 algorithm
     * @param salt the salt
     * @param iterations the number of iterations
     * @param keyLength the key length in bits
     * @param password the password to hash
     * @return the lowercase hexadecimal hash
     * @throws RuntimeException if key derivation fails
     */
    private static String generatePasswordHash(
            String algorithm, String salt, int iterations, int keyLength, String password) {
        byte[] hashBytes = generatePasswordHashBytes(
                createSecretKeyFactory(algorithm),
                salt.getBytes(StandardCharsets.UTF_8),
                iterations,
                keyLength,
                password);
        return toLowerCaseHexadecimal(hashBytes);
    }

    /**
     * Generates a password hash as bytes for constant-time comparison.
     *
     * @param secretKeyFactory the PBKDF2 secret key factory
     * @param saltBytes the salt encoded as UTF-8 bytes
     * @param iterations the number of iterations
     * @param keyLength the key length in bits
     * @param password the password to hash
     * @return the hash as a byte array
     * @throws RuntimeException if key derivation fails
     */
    private static byte[] generatePasswordHashBytes(
            SecretKeyFactory secretKeyFactory, byte[] saltBytes, int iterations, int keyLength, String password) {
        PBEKeySpec pbeKeySpec = null;
        try {
            pbeKeySpec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, keyLength);
            return secretKeyFactory.generateSecret(pbeKeySpec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } finally {
            if (pbeKeySpec != null) {
                pbeKeySpec.clearPassword();
            }
        }
    }

    /**
     * Creates a PBKDF2 secret key factory.
     *
     * @param algorithm the PBKDF2 algorithm
     * @return the secret key factory
     * @throws RuntimeException if the algorithm is not supported
     */
    private static SecretKeyFactory createSecretKeyFactory(String algorithm) {
        try {
            return SecretKeyFactory.getInstance(algorithm);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates the PBKDF2 derived key length in bits from the decoded password hash length.
     *
     * <p>The public {@code keyLength} parameter is documented as bits. Earlier versions interpreted
     * it as bytes before passing it to {@link PBEKeySpec}. To preserve existing hashes while allowing
     * documented bit semantics, accept both forms when the decoded hash length matches either the
     * configured bit length or the legacy byte length. The actual derivation length is always the
     * decoded hash length in bits.
     *
     * @param passwordHashBytes the decoded expected password hash
     * @param keyLength the configured key length
     * @return the derived key length in bits
     * @throws IllegalArgumentException if the configured key length and password hash length disagree
     */
    private static int calculateDerivedKeyLengthBits(byte[] passwordHashBytes, int keyLength) {
        int passwordHashLengthBits = passwordHashBytes.length * Byte.SIZE;
        boolean matchesBitSemantics = passwordHashLengthBits == keyLength;
        boolean matchesLegacyByteSemantics = passwordHashBytes.length == keyLength;

        if (!matchesBitSemantics && !matchesLegacyByteSemantics) {
            throw new IllegalArgumentException(String.format(
                    "PBKDF2 passwordHash length (%d bytes / %d bits) does not match configured keyLength (%d bits)",
                    passwordHashBytes.length, passwordHashLengthBits, keyLength));
        }

        return passwordHashLengthBits;
    }

    /**
     * Converts a byte array to a lowercase hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the lowercase hexadecimal string representation
     */
    private static String toLowerCaseHexadecimal(byte[] bytes) {
        int len = bytes.length;
        char[] result = new char[len * 2];

        for (int i = 0, j = 0; i < len; i++) {
            int v = bytes[i] & 0xFF;
            result[j++] = HEXADECIMAL_CHARACTERS[v >>> 4];
            result[j++] = HEXADECIMAL_CHARACTERS[v & 0x0F];
        }

        return new String(result);
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param hex the hexadecimal string to convert
     * @return the byte array representation
     * @throws IllegalArgumentException if the hex string is invalid
     */
    private static byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have an even length");
        }
        byte[] bytes = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int highNibble = Character.digit(hex.charAt(i), 16);
            int lowNibble = Character.digit(hex.charAt(i + 1), 16);

            if (highNibble < 0 || lowNibble < 0) {
                throw new IllegalArgumentException("Hex string contains a non-hexadecimal character");
            }

            bytes[i / 2] = (byte) ((highNibble << 4) + lowNibble);
        }

        return bytes;
    }
}
