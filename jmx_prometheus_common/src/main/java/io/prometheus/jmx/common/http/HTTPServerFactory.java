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

package io.prometheus.jmx.common.http;

import com.sun.net.httpserver.Authenticator;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.jmx.common.http.authenticator.MessageDigestAuthenticator;
import io.prometheus.jmx.common.http.authenticator.PBKDF2Authenticator;
import io.prometheus.jmx.common.http.authenticator.PlaintextAuthenticator;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToInteger;
import io.prometheus.jmx.common.configuration.ConvertToMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToString;
import io.prometheus.jmx.common.configuration.ValidatStringIsNotBlank;
import io.prometheus.jmx.common.configuration.ValidateIntegerInRange;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class to create the HTTPServer used by both the Java agent exporter and the standalone exporter
 */
public class HTTPServerFactory {

    private static final String REALM = "/";
    private static final String PLAINTEXT = "plaintext";
    private static final Set<String> SHA_ALGORITHMS;
    private static final Set<String> PBKDF2_ALGORITHMS;
    private static final Map<String, Integer> PBKDF2_ALGORITHM_ITERATIONS;

    private static final int PBKDF2_KEY_LENGTH_BITS = 128;

    static {
        SHA_ALGORITHMS = new HashSet<>();
        SHA_ALGORITHMS.add("SHA-1");
        SHA_ALGORITHMS.add("SHA-256");
        SHA_ALGORITHMS.add("SHA-512");

        PBKDF2_ALGORITHMS = new HashSet<>();
        PBKDF2_ALGORITHMS.add("PBKDF2WithHmacSHA1");
        PBKDF2_ALGORITHMS.add("PBKDF2WithHmacSHA256");
        PBKDF2_ALGORITHMS.add("PBKDF2WithHmacSHA512");

        PBKDF2_ALGORITHM_ITERATIONS = new HashMap<>();
        PBKDF2_ALGORITHM_ITERATIONS.put("PBKDF2WithHmacSHA1", 1300000);
        PBKDF2_ALGORITHM_ITERATIONS.put("PBKDF2WithHmacSHA256", 600000);
        PBKDF2_ALGORITHM_ITERATIONS.put("PBKDF2WithHmacSHA512", 210000);
    }

    private YamlMapAccessor rootYamlMapAccessor;

    /**
     * Constructor
     */
    public HTTPServerFactory() {
        // DO NOTHING
    }

    /**
     * Method to create an HTTPServer using the supplied arguments
     *
     * @param inetSocketAddress inetSocketAddress
     * @param collectorRegistry collectorRegistry
     * @param daemon daemon
     * @param exporterYamlFile  exporterYamlFile
     * @return an HTTPServer
     * @throws IOException IOException
     */
    public HTTPServer createHTTPServer(
            InetSocketAddress inetSocketAddress,
            CollectorRegistry collectorRegistry,
            boolean daemon,
            File exporterYamlFile) throws IOException {

        HTTPServer.Builder httpServerBuilder =
                new HTTPServer.Builder()
                        .withInetSocketAddress(inetSocketAddress)
                        .withRegistry(collectorRegistry)
                        .withDaemonThreads(daemon);

        createMapAccessor(exporterYamlFile);
        configureAuthentication(httpServerBuilder);

        return httpServerBuilder.build();
    }

    /**
     * Method to create a MapAccessor for accessing YAML configuration
     *
     * @param exporterYamlFile exporterYamlFile
     */
    private void createMapAccessor(File exporterYamlFile) {
        try (Reader reader = new FileReader(exporterYamlFile)) {
            Map<Object, Object> yamlMap = new Yaml().load(reader);
            rootYamlMapAccessor = new YamlMapAccessor(yamlMap);
        } catch (Throwable t) {
            throw new ConfigurationException(
                    String.format(
                            "Exception loading exporter YAML file [%s]",
                            exporterYamlFile),
                    t);
        }
    }

    /**
     * Method to configuration authentication
     *
     * @param httpServerBuilder httpServerBuilder
     */
    private void configureAuthentication(HTTPServer.Builder httpServerBuilder) {
        YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor =
                rootYamlMapAccessor
                        .get("/httpServer/authentication/basic")
                        .map(new ConvertToMapAccessor(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic")))
                        .orElse(null);

        if (httpServerAuthenticationBasicYamlMapAccessor != null) {
            String username =
                    httpServerAuthenticationBasicYamlMapAccessor
                            .get("/username")
                            .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/username must be a string")))
                            .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/username must not be blank")))
                            .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/username is a required string"));

            String algorithm =
                    httpServerAuthenticationBasicYamlMapAccessor
                            .get("/algorithm")
                            .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/algorithm must be a string")))
                            .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/algorithm must not be blank")))
                            .orElse(PLAINTEXT);

            Authenticator authenticator;

            if (PLAINTEXT.equalsIgnoreCase(algorithm)) {
                String password =
                        httpServerAuthenticationBasicYamlMapAccessor
                                .get("/password")
                                .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/password must be a string")))
                                .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/password must not be blank")))
                                .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/password is a required string"));

                authenticator = new PlaintextAuthenticator("/", username, password);
            } else if (SHA_ALGORITHMS.contains(algorithm) || PBKDF2_ALGORITHMS.contains(algorithm)) {
                String hash =
                        httpServerAuthenticationBasicYamlMapAccessor
                                .get("/passwordHash")
                                .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/passwordHash must be a string")))
                                .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/passwordHash must not be blank")))
                                .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/passwordHash is a required string"));

                if (SHA_ALGORITHMS.contains(algorithm)) {
                    authenticator = createMessageDigestAuthenticator(
                            httpServerAuthenticationBasicYamlMapAccessor,
                            REALM,
                            username,
                            hash,
                            algorithm);
                } else {
                    authenticator = createPBKDF2Authenticator(
                            httpServerAuthenticationBasicYamlMapAccessor,
                            REALM,
                            username,
                            hash,
                            algorithm);
                }
            } else {
                throw new ConfigurationException(
                        String.format("Unsupported /httpServer/authentication/basic/algorithm [%s]", algorithm));
            }

            httpServerBuilder.withAuthenticator(authenticator);
        }
    }

    /**
     * Method to create a MessageDigestAuthenticator
     *
     * @param httpServerAuthenticationBasicYamlMapAccessor httpServerAuthenticationBasicMapAccessor
     * @param realm realm
     * @param username username
     * @param password password
     * @param algorithm algorithm
     * @return a MessageDigestAuthenticator
     */
    private Authenticator createMessageDigestAuthenticator(
            YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor, String realm, String username, String password, String algorithm) {
        String salt =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/salt")
                        .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be a string")))
                        .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must not be blank")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/salt is a required string"));

        try {
            return new MessageDigestAuthenticator(realm, username, password, algorithm, salt);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported algorithm [%s]",
                            algorithm));
        }
    }

    /**
     * Method to create a PBKDF2Authenticator
     *
     * @param httpServerAuthenticationBasicYamlMapAccessor httpServerAuthenticationBasicMapAccessor
     * @param realm realm
     * @param username username
     * @param password password
     * @param algorithm algorithm
     * @return a PBKDF2Authenticator
     */
    private Authenticator createPBKDF2Authenticator(
            YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor, String realm, String username, String password, String algorithm) {
        String salt =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/salt")
                        .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be a string")))
                        .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be not blank")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/salt is a required string"));

        int iterations =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/iterations")
                        .map(new ConvertToInteger(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/iterations must be an integer")))
                        .map(new ValidateIntegerInRange(1, Integer.MAX_VALUE, ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/iterations must be between greater than 0")))
                        .orElse(PBKDF2_ALGORITHM_ITERATIONS.get(algorithm));

        int keyLength =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/keyLength")
                        .map(new ConvertToInteger(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/keyLength must be an integer")))
                        .map(new ValidateIntegerInRange(1, Integer.MAX_VALUE, ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/keyLength must be greater than 0")))
                        .orElse(PBKDF2_KEY_LENGTH_BITS);

        try {
            return new PBKDF2Authenticator(realm, username, password, algorithm, salt, iterations, keyLength);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported algorithm [%s]",
                            algorithm));
        }
    }
}
