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
import io.prometheus.jmx.common.util.function.ConvertToInteger;
import io.prometheus.jmx.common.util.function.ConvertToMapAccessor;
import io.prometheus.jmx.common.util.function.ConvertToString;
import io.prometheus.jmx.common.util.function.ValidatStringIsNotBlank;
import io.prometheus.jmx.common.util.function.ValidateIntegerInRange;
import io.prometheus.jmx.util.map.MapAccessor;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Class to create the HTTPServer used by both the Java agent exporter and the standalone exporter
 */
@SuppressWarnings("unchecked")
public class HTTPServerFactory {

    private MapAccessor rootMapAccessor;

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
        try {
            Reader reader = null;

            try {
                reader = new FileReader(exporterYamlFile);
                Map<Object, Object> yamlMap = new Yaml().load(reader);
                rootMapAccessor = new MapAccessor(yamlMap);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Throwable t) {
                        // DO NOTHING
                    }
                }
            }
        } catch (IOException e) {
            throw new ConfigurationException(
                    String.format(
                            "Exception loading exporter YAML file [%s]",
                            exporterYamlFile),
                    e);
        }
    }

    /**
     * Method to configuration authentication
     *
     * @param httpServerBuilder httpServerBuilder
     */
    private void configureAuthentication(HTTPServer.Builder httpServerBuilder) {
        MapAccessor httpServerAuthenticationBasicMapAccessor =
                rootMapAccessor
                        .get("/httpServer/authentication/basic")
                        .map(new ConvertToMapAccessor(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic")))
                        .orElse(null);

        if (httpServerAuthenticationBasicMapAccessor != null) {
            String username =
                    httpServerAuthenticationBasicMapAccessor
                            .get("/username")
                            .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/username must be a string")))
                            .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/username must not be blank")))
                            .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/username is a required string"));

            String password =
                    httpServerAuthenticationBasicMapAccessor
                            .get("/password")
                            .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/password must be a string")))
                            .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/password must not be blank")))
                            .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/password is a required string"));

            String algorithm =
                    httpServerAuthenticationBasicMapAccessor
                            .get("/algorithm")
                            .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/algorithm must be a string")))
                            .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/algorithm must not be blank")))
                            .orElse("plaintext");

            Authenticator authenticator;

            if ("plaintext".equalsIgnoreCase(algorithm)) {
                authenticator = new PlaintextAuthenticator("/", username, password);
            } else if (algorithm.startsWith("SHA-")) {
                authenticator = createMessageDigestAuthenticator(
                        httpServerAuthenticationBasicMapAccessor,
                        "/",
                        username,
                        password,
                        algorithm);
            } else if (algorithm.startsWith("PBKDF2")) {
                authenticator = createPBKDF2Authenticator(
                        httpServerAuthenticationBasicMapAccessor,
                        "/",
                        username,
                        password,
                        algorithm);
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
     * @param httpServerAuthenticationBasicMapAccessor httpServerAuthenticationBasicMapAccessor
     * @param realm realm
     * @param username username
     * @param password password
     * @param algorithm algorithm
     * @return a MessageDigestAuthenticator
     */
    private Authenticator createMessageDigestAuthenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor, String realm, String username, String password, String algorithm) {
        String salt =
                httpServerAuthenticationBasicMapAccessor
                        .get("/salt")
                        .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be a string")))
                        .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must not be blank")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/salt is a required string"));

        try {
            return new MessageDigestAuthenticator(realm, username, password, algorithm, salt);
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException(
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, message digest [%s] not found",
                            algorithm));
        }
    }

    /**
     * Method to create a PBKDF2Authenticator
     *
     * @param httpServerAuthenticationBasicMapAccessor httpServerAuthenticationBasicMapAccessor
     * @param realm realm
     * @param username username
     * @param password password
     * @param algorithm algorithm
     * @return a PBKDF2Authenticator
     */
    private Authenticator createPBKDF2Authenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor, String realm, String username, String password, String algorithm) {
        String salt =
                httpServerAuthenticationBasicMapAccessor
                        .get("/salt")
                        .map(new ConvertToString(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be a string")))
                        .map(new ValidatStringIsNotBlank(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/salt must be not blank")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/salt is a required string"));

        int iterations =
                httpServerAuthenticationBasicMapAccessor
                        .get("/iterations")
                        .map(new ConvertToInteger(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/iterations must be an integer")))
                        .map(new ValidateIntegerInRange(1, Integer.MAX_VALUE, ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/iterations must be between greater than 0")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/iterations is a required integer"));

        int keyLength =
                httpServerAuthenticationBasicMapAccessor
                        .get("/keyLength")
                        .map(new ConvertToInteger(ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/keyLength must be an integer")))
                        .map(new ValidateIntegerInRange(1, Integer.MAX_VALUE, ConfigurationException.supplier("Invalid configuration for /httpServer/authentication/basic/keyLength must be greater than 0")))
                        .orElseThrow(ConfigurationException.supplier("/httpServer/authentication/basic/keyLength is a required integer"));

        try {
            return new PBKDF2Authenticator(realm, username, password, algorithm, salt, iterations, keyLength);
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException(
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, algorithm [%s] not found",
                            algorithm));
        }
    }
}
