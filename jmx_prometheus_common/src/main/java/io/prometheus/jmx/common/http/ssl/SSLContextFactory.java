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

package io.prometheus.jmx.common.http.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

public class SSLContextFactory {

    private static final String JAVAX_NET_SSL_KEY_STORE = "javax.net.ssl.keyStore";
    private static final String JAVAX_NET_SSL_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    private static final String[] PROTOCOLS = { "TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1" };

    /**
     * Constructor
     */
    private SSLContextFactory() {
        // DO NOTHING
    }

    /**
     * Method to create an SSLContext using the default Java keystore properties
     *
     * @param certificateAlias certificateAlias
     * @return the return value
     * @throws GeneralSecurityException GeneralSecurityException
     * @throws IOException IOException
     */
    public static SSLContext createSSLContext(String certificateAlias) throws GeneralSecurityException, IOException {
        String keyStorePath = System.getProperty(JAVAX_NET_SSL_KEY_STORE);
        String keyStorePassword = System.getProperty(JAVAX_NET_SSL_KEY_STORE_PASSWORD);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        try (InputStream inputStream = new FileInputStream(keyStorePath)) {
            // Load the keystore
            keyStore.load(inputStream, keyStorePassword.toCharArray());

            // Loop through the certificate aliases in the keystore
            // building a set of certificate aliases that don't match
            // the requested certificate alias
            Set<String> certificateAliasesToRemove = new HashSet<>();
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String keyStoreCertificateAlias = aliases.nextElement();
                if (!keyStoreCertificateAlias.equals(certificateAlias)) {
                    certificateAliasesToRemove.add(keyStoreCertificateAlias);
                }
            }

            // Remove the certificate aliases that don't
            // match the requested certificate alias from the keystore
            for (String certificateAliasToRemove : certificateAliasesToRemove) {
                keyStore.deleteEntry(certificateAliasToRemove);
            }

            // Validate the keystore contains the certificate alias that is requested
            if (!keyStore.containsAlias(certificateAlias)) {
                throw new GeneralSecurityException(
                        String.format(
                                "certificate alias [%s] not found in keystore [%s]",
                                certificateAlias,
                                keyStorePath));
            }

            // Create and initialize an SSLContext

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

            keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init(keyStore);

            SSLContext sslContext = createSSLContext();

            sslContext.init(
                    keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            return sslContext;
        }
    }

    /**
     * Method to create an SSLContext, looping through more secure to less secure TLS protocols
     *
     * @return the return value
     * @throws GeneralSecurityException GeneralSecurityException
     */
    private static SSLContext createSSLContext() throws GeneralSecurityException {
        // Loop through potential protocols since there doesn't appear
        // to be a way to get the most secure supported protocol
        for (int i = 0; i < PROTOCOLS.length; i++) {
            try {
                return SSLContext.getInstance(PROTOCOLS[i]);
            } catch (Throwable t) {
                // DO NOTHING
            }
        }

        throw new GeneralSecurityException(String.format("No supported TLS protocols found"));
    }
}
