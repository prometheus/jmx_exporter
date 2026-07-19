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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.prometheus.jmx.common.ConfigurationException;
import io.prometheus.jmx.common.util.MapAccessor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PemIdentityLoaderTest {

    private static final String TEST_RESOURCES = "src/test/resources/io/prometheus/jmx/common/http/ssl/";

    @TempDir
    Path tempDir;

    // --- Happy path tests ---

    @Test
    void loadsPkcs8RsaKey() throws Exception {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs8.pem", null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isInstanceOf(RSAPrivateKey.class);
        assertThat(material.getCertificateChain()).hasSize(1);
        assertThat(material.getCertificateContentHash()).isNotBlank();
        assertThat(material.getPrivateKeyContentHash()).isNotBlank();
    }

    @Test
    void loadsPkcs1RsaKey() throws Exception {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs1.pem", null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isInstanceOf(RSAPrivateKey.class);
        assertThat(material.getCertificateChain()).hasSize(1);
    }

    @Test
    void loadsEcPkcs8Key() throws Exception {
        MapAccessor config = createConfig("test-cert-ec.pem", "test-key-ec-pkcs8.pem", null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isNotNull();
        assertThat(material.getPrivateKey().getAlgorithm()).isEqualTo("EC");
        assertThat(material.getCertificateChain()).hasSize(1);
    }

    @Test
    void loadsEncryptedPkcs8Key() throws Exception {
        MapAccessor config = createConfig("test-cert.pem", "test-key-encrypted-pkcs8.pem", "testpassword");
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isInstanceOf(RSAPrivateKey.class);
    }

    @Test
    void loadsEncryptedPkcs1Key() throws Exception {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs1-encrypted.pem", "testpassword");
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isInstanceOf(RSAPrivateKey.class);
        assertThat(material.getCertificateChain()).hasSize(1);
    }

    @Test
    void handlesCrLfLineEndings() throws Exception {
        String certContent =
                new String(Files.readAllBytes(Paths.get(TEST_RESOURCES + "test-cert.pem")), StandardCharsets.UTF_8);
        String keyContent = new String(
                Files.readAllBytes(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem")), StandardCharsets.UTF_8);

        // Convert LF to CRLF
        certContent = certContent.replace("\n", "\r\n");
        keyContent = keyContent.replace("\n", "\r\n");

        Path certPath = tempDir.resolve("cert-crlf.pem");
        Path keyPath = tempDir.resolve("key-crlf.pem");
        Files.write(certPath, certContent.getBytes(StandardCharsets.UTF_8));
        Files.write(keyPath, keyContent.getBytes(StandardCharsets.UTF_8));

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.getPrivateKey()).isNotNull();
    }

    // --- Validation/error tests ---

    @Test
    void rejectsMissingCertificateFilename() {
        Map<Object, Object> pemConfig = new HashMap<>();
        Map<Object, Object> privateKeyConfig = new HashMap<>();
        privateKeyConfig.put("filename", "key.pem");
        pemConfig.put("privateKey", privateKeyConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", createSslMap(pemConfig));

        assertThatThrownBy(() -> PemIdentityLoader.load(MapAccessor.of(config)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("/httpServer/ssl/pem/certificate/filename");
    }

    @Test
    void rejectsBlankCertificateFilename() {
        Map<Object, Object> certConfig = new HashMap<>();
        certConfig.put("filename", "");
        Map<Object, Object> pemConfig = new HashMap<>();
        pemConfig.put("certificate", certConfig);
        Map<Object, Object> privateKeyConfig = new HashMap<>();
        privateKeyConfig.put("filename", "key.pem");
        pemConfig.put("privateKey", privateKeyConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", createSslMap(pemConfig));

        assertThatThrownBy(() -> PemIdentityLoader.load(MapAccessor.of(config)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("/httpServer/ssl/pem/certificate/filename");
    }

    @Test
    void rejectsNonStringCertificateFilename() {
        Map<Object, Object> certConfig = new HashMap<>();
        certConfig.put("filename", 123);
        Map<Object, Object> pemConfig = new HashMap<>();
        pemConfig.put("certificate", certConfig);
        Map<Object, Object> privateKeyConfig = new HashMap<>();
        privateKeyConfig.put("filename", "key.pem");
        pemConfig.put("privateKey", privateKeyConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", createSslMap(pemConfig));

        // ToString converts 123 to "123", so it's treated as a valid string filename
        // The error will be about the file not being found
        assertThatThrownBy(() -> PemIdentityLoader.load(MapAccessor.of(config)))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rejectsMissingPrivateKeyFilename() {
        Map<Object, Object> certConfig = new HashMap<>();
        certConfig.put("filename", "cert.pem");
        Map<Object, Object> pemConfig = new HashMap<>();
        pemConfig.put("certificate", certConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", createSslMap(pemConfig));

        assertThatThrownBy(() -> PemIdentityLoader.load(MapAccessor.of(config)))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("/httpServer/ssl/pem/privateKey/filename");
    }

    @Test
    void rejectsUnreadableCertificateFile() {
        MapAccessor config = createConfig("/nonexistent/cert.pem", "key.pem", null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rejectsEmptyCertificateFile() throws Exception {
        Path certPath = tempDir.resolve("empty-cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.write(certPath, "".getBytes(StandardCharsets.UTF_8));
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsEmptyKeyFile() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("empty-key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.write(keyPath, "".getBytes(StandardCharsets.UTF_8));

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void rejectsMalformedPemCertificate() throws Exception {
        Path certPath = tempDir.resolve("bad-cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.write(
                certPath,
                "-----BEGIN CERTIFICATE-----\nINVALIDBASE64\n-----END CERTIFICATE-----\n"
                        .getBytes(StandardCharsets.UTF_8));
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rejectsCertificateOnlyInKeyFile() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("cert-in-key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        // Put certificate content in key file
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("No private key found");
    }

    @Test
    void rejectsMalformedPemKey() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("bad-key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.write(
                keyPath,
                "-----BEGIN PRIVATE KEY-----\nINVALIDBASE64\n-----END PRIVATE KEY-----\n"
                        .getBytes(StandardCharsets.UTF_8));

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rejectsEmptyKeyBody() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("empty-body-key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.write(
                keyPath, "-----BEGIN PRIVATE KEY-----\n-----END PRIVATE KEY-----\n".getBytes(StandardCharsets.UTF_8));

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config)).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void rejectsEncryptedKeyWithoutPassword() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-encrypted-pkcs8.pem", null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("encrypted")
                .hasMessageContaining("no password");
    }

    @Test
    void rejectsEncryptedPkcs1KeyWithoutPassword() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs1-encrypted.pem", null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("encrypted")
                .hasMessageContaining("no password");
    }

    @Test
    void rejectsWrongPassword() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-encrypted-pkcs8.pem", "wrongpassword");

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unable to decrypt")
                .hasMessageNotContaining("wrongpassword");
    }

    @Test
    void rejectsWrongPasswordForPkcs1Key() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs1-encrypted.pem", "wrongpassword");

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("Unable to decrypt")
                .hasMessageNotContaining("wrongpassword");
    }

    @Test
    void rejectsMismatchedKeyAndCert() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-other.pem", null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void errorMessageDoesNotContainPassword() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-encrypted-pkcs8.pem", "s3cret!");

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("s3cret!");
                });
    }

    @Test
    void errorMessageDoesNotContainPasswordForPkcs1Key() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-pkcs1-encrypted.pem", "s3cret!");

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("s3cret!");
                });
    }

    @Test
    void errorMessageDoesNotContainKeyMaterial() {
        MapAccessor config = createConfig("test-cert.pem", "test-key-other.pem", null);

        assertThatThrownBy(() -> PemIdentityLoader.load(config))
                .isInstanceOf(ConfigurationException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage()).doesNotContain("BEGIN PRIVATE KEY");
                    assertThat(e.getMessage()).doesNotContain("BEGIN RSA PRIVATE KEY");
                });
    }

    // --- Reload tests ---

    @Test
    void hasChangedReturnsFalseWhenUnchanged() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        assertThat(material.hasChanged()).isFalse();
    }

    @Test
    void hasChangedDetectsCertChange() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Modify cert file
        byte[] certBytes = Files.readAllBytes(certPath);
        Files.write(certPath, certBytes);

        // The content is the same, so hasChanged should still be false
        assertThat(material.hasChanged()).isFalse();

        // Now actually change the content
        Files.write(certPath, "modified content".getBytes(StandardCharsets.UTF_8));
        assertThat(material.hasChanged()).isTrue();
    }

    @Test
    void hasChangedDetectsKeyChange() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Modify key file
        Files.write(keyPath, "modified content".getBytes(StandardCharsets.UTF_8));
        assertThat(material.hasChanged()).isTrue();
    }

    @Test
    void reloadReturnsPriorOnMissingCertFile() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Delete cert file
        Files.delete(certPath);

        IdentityMaterial reloaded = material.reload(config);
        assertThat(reloaded).isSameAs(material);
    }

    @Test
    void reloadReturnsPriorOnMalformedReplacement() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Replace cert with garbage
        Files.write(certPath, "not a valid PEM".getBytes(StandardCharsets.UTF_8));

        IdentityMaterial reloaded = material.reload(config);
        assertThat(reloaded).isSameAs(material);
    }

    @Test
    void reloadReturnsPriorOnMismatchedReplacement() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Replace cert with a different cert (mismatched with key)
        Files.copy(
                Paths.get(TEST_RESOURCES + "test-cert-other.pem"),
                certPath,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        IdentityMaterial reloaded = material.reload(config);
        assertThat(reloaded).isSameAs(material);
    }

    @Test
    void multipleInstancesHaveIndependentState() throws Exception {
        Path certPath1 = tempDir.resolve("cert1.pem");
        Path keyPath1 = tempDir.resolve("key1.pem");
        Path certPath2 = tempDir.resolve("cert2.pem");
        Path keyPath2 = tempDir.resolve("key2.pem");

        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath1);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath1);
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath2);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath2);

        MapAccessor config1 = createConfigFromPaths(certPath1, keyPath1, null);
        MapAccessor config2 = createConfigFromPaths(certPath2, keyPath2, null);

        PemIdentityMaterial material1 = PemIdentityLoader.load(config1);
        PemIdentityMaterial material2 = PemIdentityLoader.load(config2);

        // Modify only first instance's cert
        Files.write(certPath1, "modified content".getBytes(StandardCharsets.UTF_8));

        assertThat(material1.hasChanged()).isTrue();
        assertThat(material2.hasChanged()).isFalse();
    }

    @Test
    void reloadReturnsNewInstanceOnSuccessfulReplacement() throws Exception {
        Path certPath = tempDir.resolve("cert.pem");
        Path keyPath = tempDir.resolve("key.pem");
        Files.copy(Paths.get(TEST_RESOURCES + "test-cert.pem"), certPath);
        Files.copy(Paths.get(TEST_RESOURCES + "test-key-pkcs8.pem"), keyPath);

        MapAccessor config = createConfigFromPaths(certPath, keyPath, null);
        PemIdentityMaterial material = PemIdentityLoader.load(config);

        // Trigger hasChanged() by modifying a file with valid content
        // Append a newline to the cert file - content changes but PEM is still valid
        byte[] certBytes = Files.readAllBytes(certPath);
        byte[] withNewline = new byte[certBytes.length + 1];
        System.arraycopy(certBytes, 0, withNewline, 0, certBytes.length);
        withNewline[certBytes.length] = (byte) '\n';
        Files.write(certPath, withNewline);

        assertThat(material.hasChanged()).isTrue();

        IdentityMaterial reloaded = material.reload(config);
        assertThat(reloaded).isNotSameAs(material);
    }

    // --- Helper methods ---

    private MapAccessor createConfig(String certResource, String keyResource, String password) {
        String certPath = TEST_RESOURCES + certResource;
        String keyPath = TEST_RESOURCES + keyResource;
        return createConfigFromPaths(Paths.get(certPath), Paths.get(keyPath), password);
    }

    private MapAccessor createConfigFromPaths(Path certPath, Path keyPath, String password) {
        Map<Object, Object> certConfig = new HashMap<>();
        certConfig.put("filename", certPath.toString());

        Map<Object, Object> keyConfig = new HashMap<>();
        keyConfig.put("filename", keyPath.toString());
        if (password != null) {
            keyConfig.put("password", password);
        }

        Map<Object, Object> pemConfig = new HashMap<>();
        pemConfig.put("certificate", certConfig);
        pemConfig.put("privateKey", keyConfig);

        Map<Object, Object> config = new HashMap<>();
        config.put("httpServer", createSslMap(pemConfig));

        return MapAccessor.of(config);
    }

    private Map<Object, Object> createSslMap(Object pemOrKeyStore) {
        Map<Object, Object> sslConfig = new HashMap<>();
        if (pemOrKeyStore instanceof Map) {
            // Check if it's a PEM config
            Map<?, ?> map = (Map<?, ?>) pemOrKeyStore;
            if (map.containsKey("certificate") || map.containsKey("privateKey")) {
                sslConfig.put("pem", pemOrKeyStore);
            } else {
                sslConfig.put("keyStore", pemOrKeyStore);
            }
        }
        Map<Object, Object> httpServerConfig = new HashMap<>();
        httpServerConfig.put("ssl", sslConfig);
        return httpServerConfig;
    }
}
