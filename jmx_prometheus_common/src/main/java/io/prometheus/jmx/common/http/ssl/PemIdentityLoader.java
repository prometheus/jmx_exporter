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

package io.prometheus.jmx.common.http.ssl;

import io.prometheus.jmx.common.ConfigurationException;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.functions.StringIsNotBlank;
import io.prometheus.jmx.common.util.functions.ToString;
import io.prometheus.jmx.variable.VariableResolver;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nl.altindag.ssl.util.CertificateUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEInputDecryptorProviderBuilder;

/**
 * Package-private static loader for PEM-based server identity.
 *
 * <p>Parses certificate chain and private key from PEM files, verifies they match, and returns a
 * {@link PemIdentityMaterial}.
 */
public class PemIdentityLoader {

    private static final String PEM_CERTIFICATE_FILENAME = "/httpServer/ssl/pem/certificate/filename";
    private static final String PEM_PRIVATE_KEY_FILENAME = "/httpServer/ssl/pem/privateKey/filename";
    private static final String PEM_PRIVATE_KEY_PASSWORD = "/httpServer/ssl/pem/privateKey/password";

    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1 MB

    /**
     * Loads PEM identity material from the configuration.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return the parsed PEM identity material
     * @throws ConfigurationException if the configuration is invalid or files cannot be parsed
     */
    public static PemIdentityMaterial load(MapAccessor rootMapAccessor) {
        // Extract certificate filename
        String certFilename = rootMapAccessor
                .getPath(PEM_CERTIFICATE_FILENAME)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for " + PEM_CERTIFICATE_FILENAME + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for " + PEM_CERTIFICATE_FILENAME + " must not be blank")))
                .orElseThrow(ConfigurationException.supplier(PEM_CERTIFICATE_FILENAME + " is a required string"));

        // Extract private key filename
        String keyFilename = rootMapAccessor
                .getPath(PEM_PRIVATE_KEY_FILENAME)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for " + PEM_PRIVATE_KEY_FILENAME + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for " + PEM_PRIVATE_KEY_FILENAME + " must not be blank")))
                .orElseThrow(ConfigurationException.supplier(PEM_PRIVATE_KEY_FILENAME + " is a required string"));

        // Extract optional password
        String password = rootMapAccessor
                .getPath(PEM_PRIVATE_KEY_PASSWORD)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for " + PEM_PRIVATE_KEY_PASSWORD + " must be a string")))
                .orElse(null);

        if (password != null) {
            password = VariableResolver.resolveVariable(password);
        }

        Path certPath = Paths.get(certFilename);
        Path keyPath = Paths.get(keyFilename);

        // Read and parse certificate chain
        Certificate[] certChain = loadCertificateChain(certPath);

        // Read and parse private key
        char[] passwordChars = password != null ? password.toCharArray() : new char[0];
        PrivateKey privateKey = loadPrivateKey(keyPath, passwordChars);

        // Verify key matches leaf certificate
        verifyKeyMatchesCertificate(privateKey, certChain[0]);

        // Compute content hashes
        String certHash = getContentHash(certPath);
        String keyHash = getContentHash(keyPath);

        return new PemIdentityMaterial(privateKey, certChain, passwordChars, certPath, keyPath, certHash, keyHash);
    }

    private static Certificate[] loadCertificateChain(Path certPath) {
        try {
            byte[] certBytes = readBoundedFile(certPath);
            if (certBytes.length == 0) {
                throw new ConfigurationException("PEM certificate file is empty: " + certPath);
            }

            List<Certificate> certificates = CertificateUtils.loadCertificate(certPath);
            if (certificates.isEmpty()) {
                throw new ConfigurationException("No certificates found in: " + certPath);
            }

            return certificates.toArray(new Certificate[0]);
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Unable to load PEM certificate from: " + certPath, e);
        }
    }

    private static PrivateKey loadPrivateKey(Path keyPath, char[] password) {
        byte[] keyBytes;
        try {
            keyBytes = readBoundedFile(keyPath);
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read PEM private key file: " + keyPath, e);
        }

        if (keyBytes.length == 0) {
            throw new ConfigurationException("PEM private key file is empty: " + keyPath);
        }

        String keyContent = new String(keyBytes);

        try (PEMParser pemParser = new PEMParser(new StringReader(keyContent))) {
            List<Object> pemObjects = new ArrayList<>();
            Object obj;
            while ((obj = pemParser.readObject()) != null) {
                pemObjects.add(obj);
            }

            PrivateKey privateKey = null;
            boolean hasEncryptedKey = false;

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            BouncyCastleProvider bcProvider = new BouncyCastleProvider();

            for (Object pemObj : pemObjects) {
                if (pemObj instanceof PrivateKeyInfo) {
                    if (privateKey != null) {
                        throw new ConfigurationException("Multiple private keys found in: " + keyPath);
                    }
                    privateKey = converter.getPrivateKey((PrivateKeyInfo) pemObj);
                } else if (pemObj instanceof PEMKeyPair) {
                    if (privateKey != null) {
                        throw new ConfigurationException("Multiple private keys found in: " + keyPath);
                    }
                    privateKey = converter.getKeyPair((PEMKeyPair) pemObj).getPrivate();
                } else if (pemObj instanceof PKCS8EncryptedPrivateKeyInfo) {
                    if (privateKey != null) {
                        throw new ConfigurationException("Multiple private keys found in: " + keyPath);
                    }
                    hasEncryptedKey = true;
                    if (password.length == 0) {
                        throw new ConfigurationException(
                                "Private key is encrypted but no password was configured for: " + keyPath);
                    }
                    try {
                        PKCS8EncryptedPrivateKeyInfo encryptedInfo = (PKCS8EncryptedPrivateKeyInfo) pemObj;
                        PrivateKeyInfo keyInfo =
                                encryptedInfo.decryptPrivateKeyInfo(new JcePKCSPBEInputDecryptorProviderBuilder()
                                        .setProvider(bcProvider)
                                        .build(password));
                        privateKey = converter.getPrivateKey(keyInfo);
                    } catch (Exception e) {
                        throw new ConfigurationException("Unable to decrypt PEM private key from: " + keyPath);
                    }
                } else if (pemObj instanceof PEMEncryptedKeyPair) {
                    if (privateKey != null) {
                        throw new ConfigurationException("Multiple private keys found in: " + keyPath);
                    }
                    hasEncryptedKey = true;
                    if (password.length == 0) {
                        throw new ConfigurationException(
                                "Private key is encrypted but no password was configured for: " + keyPath);
                    }
                    try {
                        PEMEncryptedKeyPair encryptedKeyPair = (PEMEncryptedKeyPair) pemObj;
                        PEMKeyPair keyPair = encryptedKeyPair.decryptKeyPair(new JcePEMDecryptorProviderBuilder()
                                .setProvider(bcProvider)
                                .build(password));
                        privateKey = converter.getKeyPair(keyPair).getPrivate();
                    } catch (Exception e) {
                        throw new ConfigurationException("Unable to decrypt PEM private key from: " + keyPath);
                    }
                }
            }

            if (privateKey == null) {
                throw new ConfigurationException("No private key found in: " + keyPath);
            }

            return privateKey;
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse PEM private key from: " + keyPath, e);
        }
    }

    private static void verifyKeyMatchesCertificate(PrivateKey privateKey, Certificate certificate) {
        try {
            X509Certificate x509Cert = (X509Certificate) certificate;
            String keyAlgorithm = privateKey.getAlgorithm();

            String sigAlg;
            if ("RSA".equals(keyAlgorithm)) {
                sigAlg = "SHA256withRSA";
            } else if ("EC".equals(keyAlgorithm)) {
                sigAlg = "SHA256withECDSA";
            } else if ("DSA".equals(keyAlgorithm)) {
                sigAlg = "SHA256withDSA";
            } else {
                throw new ConfigurationException("Unsupported private key algorithm: " + keyAlgorithm);
            }

            byte[] challenge = new byte[32];
            new SecureRandom().nextBytes(challenge);

            Signature sig = Signature.getInstance(sigAlg);
            sig.initSign(privateKey);
            sig.update(challenge);
            byte[] signatureBytes = sig.sign();

            Signature verifier = Signature.getInstance(sigAlg);
            verifier.initVerify(x509Cert.getPublicKey());
            verifier.update(challenge);

            if (!verifier.verify(signatureBytes)) {
                throw new ConfigurationException("Private key does not match the leaf certificate");
            }
        } catch (ConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigurationException("Unable to verify private key matches certificate", e);
        }
    }

    /**
     * Computes the SHA-256 content hash of a file.
     *
     * @param path the file path
     * @return the SHA-256 content hash as hex string
     */
    static String getContentHash(Path path) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];

            try (InputStream inputStream = Files.newInputStream(path)) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            }

            return toHex(messageDigest.digest());
        } catch (IOException e) {
            throw new ConfigurationException("Unable to read PEM file: " + path, e);
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException("Unable to compute SHA-256 hash", e);
        }
    }

    /**
     * Computes the SHA-256 content hash of a file for runtime reload checks.
     *
     * <p>Returns empty if the file cannot be read (tolerant for runtime checks).
     *
     * @param path the file path
     * @return the hash, or empty if unreadable
     */
    public static Optional<String> getContentHashOptional(Path path) {
        try {
            return Optional.of(getContentHash(path));
        } catch (ConfigurationException e) {
            return Optional.empty();
        }
    }

    private static byte[] readBoundedFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length > MAX_FILE_SIZE) {
            throw new ConfigurationException("PEM file exceeds maximum size of 1MB: " + path);
        }
        return bytes;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
