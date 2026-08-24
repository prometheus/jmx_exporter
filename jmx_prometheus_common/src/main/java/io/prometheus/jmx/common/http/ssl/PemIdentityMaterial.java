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
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Optional;
import nl.altindag.ssl.SSLFactory;

/**
 * Package-private implementation of {@link IdentityMaterial} for PEM-based server identity.
 *
 * <p>Wraps a parsed PEM certificate chain and private key, applies them to an SSLFactory builder,
 * and detects file changes for reload.
 */
class PemIdentityMaterial implements IdentityMaterial {

    private final PrivateKey privateKey;
    private final Certificate[] certificateChain;
    private final char[] keyPassword;
    private final Path certificatePath;
    private final Path privateKeyPath;
    private final String certificateContentHash;
    private final String privateKeyContentHash;

    PemIdentityMaterial(
            PrivateKey privateKey,
            Certificate[] certificateChain,
            char[] keyPassword,
            Path certificatePath,
            Path privateKeyPath,
            String certificateContentHash,
            String privateKeyContentHash) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
        this.keyPassword = keyPassword;
        this.certificatePath = certificatePath;
        this.privateKeyPath = privateKeyPath;
        this.certificateContentHash = certificateContentHash;
        this.privateKeyContentHash = privateKeyContentHash;
    }

    @Override
    public void applyTo(SSLFactory.Builder builder) {
        builder.withSwappableIdentityMaterial().withIdentityMaterial(privateKey, keyPassword, certificateChain);
    }

    @Override
    public boolean hasChanged() {
        Optional<String> currentCertHash = PemIdentityLoader.getContentHashOptional(certificatePath);
        if (!currentCertHash.isPresent()) {
            return false;
        }

        Optional<String> currentKeyHash = PemIdentityLoader.getContentHashOptional(privateKeyPath);
        if (!currentKeyHash.isPresent()) {
            return false;
        }

        return !certificateContentHash.equals(currentCertHash.get())
                || !privateKeyContentHash.equals(currentKeyHash.get());
    }

    @Override
    public IdentityMaterial reload(MapAccessor rootMapAccessor) {
        try {
            PemIdentityMaterial newMaterial = PemIdentityLoader.load(rootMapAccessor);

            // Detect torn files: re-check hashes after parsing
            Optional<String> postParseCertHash = PemIdentityLoader.getContentHashOptional(certificatePath);
            Optional<String> postParseKeyHash = PemIdentityLoader.getContentHashOptional(privateKeyPath);

            if (!postParseCertHash.isPresent() || !postParseKeyHash.isPresent()) {
                return this;
            }

            if (!postParseCertHash.get().equals(newMaterial.certificateContentHash)
                    || !postParseKeyHash.get().equals(newMaterial.privateKeyContentHash)) {
                return this;
            }

            return newMaterial;
        } catch (ConfigurationException e) {
            return this;
        }
    }

    PrivateKey getPrivateKey() {
        return privateKey;
    }

    Certificate[] getCertificateChain() {
        return certificateChain;
    }

    Path getCertificatePath() {
        return certificatePath;
    }

    Path getPrivateKeyPath() {
        return privateKeyPath;
    }

    String getCertificateContentHash() {
        return certificateContentHash;
    }

    String getPrivateKeyContentHash() {
        return privateKeyContentHash;
    }
}
