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

package io.prometheus.jmx.common;

import static java.lang.String.format;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpsConfigurator;
import io.prometheus.jmx.common.authenticator.MessageDigestAuthenticator;
import io.prometheus.jmx.common.authenticator.PBKDF2Authenticator;
import io.prometheus.jmx.common.authenticator.PlaintextAuthenticator;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.jmx.common.util.functions.IntegerInRange;
import io.prometheus.jmx.common.util.functions.StringIsNotBlank;
import io.prometheus.jmx.common.util.functions.ToBoolean;
import io.prometheus.jmx.common.util.functions.ToInteger;
import io.prometheus.jmx.common.util.functions.ToMapAccessor;
import io.prometheus.jmx.common.util.functions.ToString;
import io.prometheus.jmx.variable.VariableResolver;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.exception.GenericException;
import nl.altindag.ssl.util.SSLFactoryUtils;

/**
 * Class to create the HTTPServer used by both the Java agent exporter and the Standalone exporter
 */
public class HTTPServerFactory {

    private static final String JAVAX_NET_SSL_KEY_STORE = "javax.net.ssl.keyStore";
    private static final String JAVAX_NET_SSL_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
    private static final String JAVAX_NET_SSL_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";

    private static final String DEFAULT_KEYSTORE_TYPE;

    private static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String JAVAX_NET_SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
    private static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD =
            "javax.net.ssl.trustStorePassword";

    private static final String DEFAULT_TRUST_STORE_TYPE;

    private static final int DEFAULT_MINIMUM_THREADS = 1;
    private static final int DEFAULT_MAXIMUM_THREADS = 10;
    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 120;

    private static final String REALM = "/";
    private static final String PLAINTEXT = "plaintext";
    private static final Set<String> SHA_ALGORITHMS;
    private static final Set<String> PBKDF2_ALGORITHMS;
    private static final Map<String, Integer> PBKDF2_ALGORITHM_ITERATIONS;
    private static final int PBKDF2_KEY_LENGTH_BITS = 128;
    private static final ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newSingleThreadScheduledExecutor();

    private static final String COMMA_SEPARATOR = ",";

    private static KeyStoreProperties keyStoreProperties;
    private static KeyStoreProperties trustStoreProperties;

    static {
        // Get the keystore type system property
        String keyStoreType = System.getProperty(JAVAX_NET_SSL_KEY_STORE_TYPE);
        if (keyStoreType == null) {
            // If the keystore type system property is not set, use the default keystore type
            keyStoreType = KeyStore.getDefaultType();
        }

        // Set the default keystore type
        DEFAULT_KEYSTORE_TYPE = keyStoreType;

        // Get the truststore type system property
        String trustStoreType = System.getProperty(JAVAX_NET_SSL_TRUST_STORE_TYPE);
        if (trustStoreType == null) {
            // If the truststore type system property is not set, use the default truststore type
            trustStoreType = KeyStore.getDefaultType();
        }

        // Set the default truststore type
        DEFAULT_TRUST_STORE_TYPE = trustStoreType;

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

        Runtime.getRuntime().addShutdownHook(new Thread(EXECUTOR_SERVICE::shutdownNow));
    }

    /** Constructor */
    private HTTPServerFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to create an HTTPServer using the supplied arguments
     *
     * @param prometheusRegistry prometheusRegistry
     * @param inetAddress inetAddress
     * @param port port
     * @param exporterYamlFile exporterYamlFile
     * @return an HTTPServer
     * @throws IOException IOException
     */
    public static HTTPServer createAndStartHTTPServer(
            PrometheusRegistry prometheusRegistry,
            InetAddress inetAddress,
            int port,
            File exporterYamlFile)
            throws IOException {
        MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));

        HTTPServer.Builder httpServerBuilder =
                HTTPServer.builder()
                        .inetAddress(inetAddress)
                        .port(port)
                        .registry(prometheusRegistry);

        configureThreads(rootMapAccessor, httpServerBuilder);
        configureAuthentication(rootMapAccessor, httpServerBuilder);
        configureSSL(rootMapAccessor, httpServerBuilder);

        return httpServerBuilder.buildAndStart();
    }

    /**
     * Method to create an HTTPServer using the supplied arguments (used for testing)
     *
     * @param prometheusRegistry prometheusRegistry
     * @param exporterYamlFile exporterYamlFile
     * @return an HTTPServer
     * @throws IOException IOException
     */
    public static HTTPServer createAndStartHTTPServer(
            PrometheusRegistry prometheusRegistry, File exporterYamlFile) throws IOException {
        MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));

        HTTPServer.Builder httpServerBuilder = HTTPServer.builder().registry(prometheusRegistry);

        configureThreads(rootMapAccessor, httpServerBuilder);
        configureAuthentication(rootMapAccessor, httpServerBuilder);
        configureSSL(rootMapAccessor, httpServerBuilder);

        return httpServerBuilder.buildAndStart();
    }

    /**
     * Method to configure the HTTPServer thread pool
     *
     * @param rootMapAccessor rootMapAccessor
     * @param httpServerBuilder httpServerBuilder
     */
    private static void configureThreads(
            MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        int minimum = DEFAULT_MINIMUM_THREADS;
        int maximum = DEFAULT_MAXIMUM_THREADS;
        int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME_SECONDS;

        if (rootMapAccessor.containsPath("/httpServer/threads")) {
            MapAccessor httpServerThreadsMapAccessor =
                    rootMapAccessor
                            .get("/httpServer/threads")
                            .map(
                                    new ToMapAccessor(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for /httpServer/threads"
                                                            + " must be a map")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads must be a map"));

            minimum =
                    httpServerThreadsMapAccessor
                            .get("/minimum")
                            .map(
                                    new ToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/minimum must be an"
                                                        + " integer")))
                            .map(
                                    new IntegerInRange(
                                            1,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/minimum must be 1"
                                                        + " or greater")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads/minimum is a required integer"));

            maximum =
                    httpServerThreadsMapAccessor
                            .get("/maximum")
                            .map(
                                    new ToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/maximum must be an"
                                                        + " integer")))
                            .map(
                                    new IntegerInRange(
                                            minimum,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/maxPoolSize must be"
                                                        + " between greater than 0")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads/maximum is a required integer"));

            keepAliveTime =
                    httpServerThreadsMapAccessor
                            .get("/keepAliveTime")
                            .map(
                                    new ToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/keepAliveTime must"
                                                        + " be an integer")))
                            .map(
                                    new IntegerInRange(
                                            1,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/keepAliveTime must"
                                                        + " be greater than 0")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads/keepAliveTime is a required"
                                                    + " integer"));
        }

        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(
                        minimum,
                        maximum,
                        keepAliveTime,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(true),
                        NamedDaemonThreadFactory.defaultThreadFactory(true),
                        new BlockingRejectedExecutionHandler());

        httpServerBuilder.executorService(threadPoolExecutor);
    }

    /**
     * Method to configure authentication
     *
     * @param rootMapAccessor rootMapAccessor
     * @param httpServerBuilder httpServerBuilder
     */
    private static void configureAuthentication(
            MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        Authenticator authenticator;

        if (rootMapAccessor.containsPath("/httpServer/authentication")) {
            Optional<Object> authenticatorClassAttribute =
                    rootMapAccessor.get("/httpServer/authentication/plugin");

            if (authenticatorClassAttribute.isPresent()) {
                MapAccessor httpServerAuthenticationCustomAuthenticatorMapAccessor =
                        rootMapAccessor
                                .get("/httpServer/authentication/plugin")
                                .map(
                                        new ToMapAccessor(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/plugin"
                                                            + " must be a map")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/plugin"
                                                        + " must be a map"));

                String authenticatorClass =
                        httpServerAuthenticationCustomAuthenticatorMapAccessor
                                .get("/class")
                                .map(
                                        new ToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/plugin/class"
                                                            + " must be a string")))
                                .map(
                                        new StringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/plugin/class"
                                                            + " must not be blank")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/plugin/class must be a"
                                                        + " string"));

                Optional<Object> subjectAttribute =
                        httpServerAuthenticationCustomAuthenticatorMapAccessor.get(
                                "/subjectAttributeName");

                if (subjectAttribute.isPresent()) {
                    String subjectAttributeName =
                            subjectAttribute
                                    .map(
                                            new ToString(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/plugin/class/subjectAttributeName"
                                                                + " must be a string")))
                                    .map(
                                            new StringIsNotBlank(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/plugin/subjectAttributeName"
                                                                + " must not be blank")))
                                    .get();

                    // need subject.doAs for subsequent handlers
                    httpServerBuilder.authenticatedSubjectAttributeName(subjectAttributeName);
                }

                authenticator = loadAuthenticator(authenticatorClass);
            } else {
                MapAccessor httpServerAuthenticationBasicMapAccessor =
                        rootMapAccessor
                                .get("/httpServer/authentication/basic")
                                .map(
                                        new ToMapAccessor(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/basic configuration"
                                                        + " must be a map"));

                String username =
                        httpServerAuthenticationBasicMapAccessor
                                .get("/username")
                                .map(
                                        new ToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/username"
                                                            + " must be a string")))
                                .map(
                                        new StringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/username"
                                                            + " must not be blank")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/basic/username is a"
                                                        + " required string"));

                // Resolve the username
                username = VariableResolver.resolveVariable(username);

                String algorithm =
                        httpServerAuthenticationBasicMapAccessor
                                .get("/algorithm")
                                .map(
                                        new ToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/algorithm"
                                                            + " must be a string")))
                                .map(
                                        new StringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/algorithm"
                                                            + " must not be blank")))
                                .orElse(PLAINTEXT);

                if (PLAINTEXT.equalsIgnoreCase(algorithm)) {
                    String password =
                            httpServerAuthenticationBasicMapAccessor
                                    .get("/password")
                                    .map(
                                            new ToString(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/basic/password"
                                                                + " must be a string")))
                                    .map(
                                            new StringIsNotBlank(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/basic/password"
                                                                + " must not be blank")))
                                    .orElseThrow(
                                            ConfigurationException.supplier(
                                                    "/httpServer/authentication/basic/password"
                                                            + " is a required string"));

                    // Resolve the password
                    password = VariableResolver.resolveVariable(password);

                    authenticator = new PlaintextAuthenticator("/", username, password);
                } else if (SHA_ALGORITHMS.contains(algorithm)
                        || PBKDF2_ALGORITHMS.contains(algorithm)) {
                    String hash =
                            httpServerAuthenticationBasicMapAccessor
                                    .get("/passwordHash")
                                    .map(
                                            new ToString(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/basic/passwordHash"
                                                                + " must be a string")))
                                    .map(
                                            new StringIsNotBlank(
                                                    ConfigurationException.supplier(
                                                            "Invalid configuration for"
                                                                + " /httpServer/authentication/basic/passwordHash"
                                                                + " must not be blank")))
                                    .orElseThrow(
                                            ConfigurationException.supplier(
                                                    "/httpServer/authentication/basic/passwordHash"
                                                            + " is a required string"));

                    if (SHA_ALGORITHMS.contains(algorithm)) {
                        authenticator =
                                createMessageDigestAuthenticator(
                                        httpServerAuthenticationBasicMapAccessor,
                                        REALM,
                                        username,
                                        hash,
                                        algorithm);
                    } else {
                        authenticator =
                                createPBKDF2Authenticator(
                                        httpServerAuthenticationBasicMapAccessor,
                                        REALM,
                                        username,
                                        hash,
                                        algorithm);
                    }
                } else {
                    throw new ConfigurationException(
                            format(
                                    "Unsupported /httpServer/authentication/basic/algorithm"
                                            + " [%s]",
                                    algorithm));
                }
            }

            httpServerBuilder.authenticator(authenticator);
        }
    }

    private static Authenticator loadAuthenticator(String className) {
        Class<?> clazz;

        try {
            clazz = HTTPServerFactory.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    format(
                            "configured /httpServer/authentication/authenticatorClass [%s]"
                                    + " not found, loadClass resulted in [%s:%s]",
                            className, e.getClass(), e.getMessage()));
        }

        if (!Authenticator.class.isAssignableFrom(clazz)) {
            throw new ConfigurationException(
                    format(
                            "configured /httpServer/authentication/authenticatorClass [%s]"
                                + " loadClass resulted in [%s] of the wrong type, is not assignable"
                                + " from Authenticator",
                            className, clazz.getCanonicalName()));
        }

        try {
            return (Authenticator) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(
                    format(
                            "configured /httpServer/authentication/authenticatorClass [%s] no arg"
                                    + " constructor newInstance resulted in exception [%s:%s]",
                            className, e.getClass(), e.getMessage()));
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
    private static Authenticator createMessageDigestAuthenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt =
                httpServerAuthenticationBasicMapAccessor
                        .get("/salt")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " not be blank")))
                        .orElseThrow(
                                ConfigurationException.supplier(
                                        "/httpServer/authentication/basic/salt is a required"
                                                + " string"));

        try {
            return new MessageDigestAuthenticator(realm, username, password, algorithm, salt);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(
                    format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported"
                                    + " algorithm [%s]",
                            algorithm));
        }
    }

    /**
     * Method to create a PBKDF2Authenticator
     *
     * @param httpServerAuthenticationBasicMapAccessor httpServerAuthenticationBasicMapAccessor
     * @param realm realm
     * @param username username
     * @param password password./m
     * @param algorithm algorithm
     * @return a PBKDF2Authenticator
     */
    private static Authenticator createPBKDF2Authenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt =
                httpServerAuthenticationBasicMapAccessor
                        .get("/salt")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be not blank")))
                        .orElseThrow(
                                ConfigurationException.supplier(
                                        "/httpServer/authentication/basic/salt is a required"
                                                + " string"));

        int iterations =
                httpServerAuthenticationBasicMapAccessor
                        .get("/iterations")
                        .map(
                                new ToInteger(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/iterations"
                                                    + " must be an integer")))
                        .map(
                                new IntegerInRange(
                                        1,
                                        Integer.MAX_VALUE,
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/iterations"
                                                    + " must be between greater than 0")))
                        .orElse(PBKDF2_ALGORITHM_ITERATIONS.get(algorithm));

        int keyLength =
                httpServerAuthenticationBasicMapAccessor
                        .get("/keyLength")
                        .map(
                                new ToInteger(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/keyLength"
                                                    + " must be an integer")))
                        .map(
                                new IntegerInRange(
                                        1,
                                        Integer.MAX_VALUE,
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/keyLength"
                                                    + " must be greater than 0")))
                        .orElse(PBKDF2_KEY_LENGTH_BITS);

        try {
            return new PBKDF2Authenticator(
                    realm, username, password, algorithm, salt, iterations, keyLength);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(
                    format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported"
                                    + " algorithm [%s]",
                            algorithm));
        }
    }

    /**
     * Method to configure SSL
     *
     * @param rootMapAccessor rootMapAccessor
     * @param httpServerBuilder httpServerBuilder
     */
    public static void configureSSL(
            MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        if (rootMapAccessor.containsPath("/httpServer/ssl")) {
            try {
                SSLFactory sslFactory = createSslFactory(rootMapAccessor);
                Runnable sslUpdater = () -> reloadSsl(sslFactory, rootMapAccessor);
                // check every hour for file changes and if it has been modified update the ssl
                // configuration
                EXECUTOR_SERVICE.scheduleAtFixedRate(sslUpdater, 1, 1, TimeUnit.HOURS);

                httpServerBuilder.httpsConfigurator(
                        new HttpsConfigurator(sslFactory.getSslContext()));
            } catch (GenericException e) {
                String message = e.getMessage();
                if (message != null && !message.trim().isEmpty()) {
                    message = ", " + message.trim();
                } else {
                    message = "";
                }

                throw new ConfigurationException(
                        format("Exception loading SSL configuration%s", message), e);
            }
        }
    }

    private static SSLFactory createSslFactory(MapAccessor rootMapAccessor) {
        keyStoreProperties = getKeyStoreProperties(rootMapAccessor);
        Optional<KeyStoreProperties> trustProps = getTrustStoreProperties(rootMapAccessor);

        SSLFactory.Builder sslFactoryBuilder =
                SSLFactory.builder()
                        .withSwappableIdentityMaterial()
                        .withIdentityMaterial(
                                keyStoreProperties.getFilename(),
                                keyStoreProperties.getPassword(),
                                keyStoreProperties.getPassword(),
                                keyStoreProperties.getType());

        if (trustProps.isPresent()) {
            trustStoreProperties = trustProps.get();
            sslFactoryBuilder
                    .withSwappableTrustMaterial()
                    .withTrustMaterial(
                            trustStoreProperties.getFilename(),
                            trustStoreProperties.getPassword(),
                            trustStoreProperties.getType());
        }

        sslFactoryBuilder.withNeedClientAuthentication(isMutualTls(rootMapAccessor));
        getProtocolsProperties(rootMapAccessor).ifPresent(sslFactoryBuilder::withProtocols);
        getCiphersProperties(rootMapAccessor).ifPresent(sslFactoryBuilder::withCiphers);

        return sslFactoryBuilder.build();
    }

    private static void reloadSsl(SSLFactory sslFactory, MapAccessor rootMapAccessor) {
        KeyStoreProperties keyProps = getKeyStoreProperties(rootMapAccessor);
        Optional<KeyStoreProperties> trustProps = getTrustStoreProperties(rootMapAccessor);

        boolean sslUpdated = false;
        SSLFactory.Builder updatedSslFactory = SSLFactory.builder();
        if (keyStoreProperties.getLastModifiedTime().isBefore(keyProps.getLastModifiedTime())) {
            updatedSslFactory.withIdentityMaterial(
                    keyProps.getFilename(), keyProps.getPassword(), keyProps.getType());
            keyStoreProperties = keyProps;
            sslUpdated = true;
        }

        if (trustProps.isPresent()
                && getTrustStoreProperties().isPresent()
                && getTrustStoreProperties()
                        .get()
                        .getLastModifiedTime()
                        .isBefore(trustProps.get().getLastModifiedTime())) {
            updatedSslFactory.withTrustMaterial(
                    trustProps.get().getFilename(),
                    trustProps.get().getPassword(),
                    trustProps.get().getType());
            trustStoreProperties = trustProps.get();
            sslUpdated = true;
        }

        if (sslUpdated) {
            SSLFactoryUtils.reload(sslFactory, updatedSslFactory.build());
        }
    }

    private static KeyStoreProperties getKeyStoreProperties(MapAccessor rootMapAccessor) {
        String keyStoreFilename =
                rootMapAccessor
                        .get("/httpServer/ssl/keyStore/filename")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/filename"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/filename"
                                                        + " must not be blank")))
                        .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE));

        Instant lastModifiedTime = getLastModifiedTime(keyStoreFilename);

        String keyStoreType =
                rootMapAccessor
                        .get("/httpServer/ssl/keyStore/type")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/type"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/type"
                                                        + " must not be blank")))
                        .orElse(DEFAULT_KEYSTORE_TYPE);

        String keyStorePassword =
                rootMapAccessor
                        .get("/httpServer/ssl/keyStore/password")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/password"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/keyStore/password"
                                                        + " must not be blank")))
                        .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE_PASSWORD));

        // Resolve the password
        keyStorePassword = VariableResolver.resolveVariable(keyStorePassword);

        String certificateAlias =
                rootMapAccessor
                        .get("/httpServer/ssl/certificate/alias")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/certificate/alias"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/certificate/alias"
                                                        + " must not be blank")))
                        .orElseThrow(
                                ConfigurationException.supplier(
                                        "/httpServer/ssl/certificate/alias is a required"
                                                + " string"));

        return new KeyStoreProperties(
                keyStoreFilename,
                lastModifiedTime,
                keyStorePassword.toCharArray(),
                keyStoreType,
                certificateAlias);
    }

    private static Instant getLastModifiedTime(String filename) {
        try {
            return Files.readAttributes(Paths.get(filename), BasicFileAttributes.class)
                    .lastModifiedTime()
                    .toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    private static Optional<KeyStoreProperties> getTrustStoreProperties(
            MapAccessor rootMapAccessor) {
        final boolean mutualTLS = isMutualTls(rootMapAccessor);
        if (!mutualTLS) {
            return Optional.empty();
        }

        String trustStoreFilename =
                rootMapAccessor
                        .get("/httpServer/ssl/trustStore/filename")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/filename"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/filename"
                                                        + " must not be blank")))
                        .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE));

        Instant lastModifiedTime = getLastModifiedTime(trustStoreFilename);

        String trustStoreType =
                rootMapAccessor
                        .get("/httpServer/ssl/trustStore/type")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/type"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/type"
                                                        + " must not be blank")))
                        .orElse(DEFAULT_TRUST_STORE_TYPE);

        String trustStorePassword =
                rootMapAccessor
                        .get("/httpServer/ssl/trustStore/password")
                        .map(
                                new ToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/password"
                                                        + " must be a string")))
                        .map(
                                new StringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                        + " /httpServer/ssl/trustStore/password"
                                                        + " must not be blank")))
                        .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD));

        // Resolve the password
        trustStorePassword = VariableResolver.resolveVariable(trustStorePassword);

        return Optional.of(
                new KeyStoreProperties(
                        trustStoreFilename,
                        lastModifiedTime,
                        trustStorePassword.toCharArray(),
                        trustStoreType,
                        null));
    }

    private static boolean isMutualTls(MapAccessor rootMapAccessor) {
        return rootMapAccessor
                .get("/httpServer/ssl/mutualTLS")
                .map(
                        new ToString(
                                ConfigurationException.supplier(
                                        "Invalid configuration for"
                                                + " /httpServer/ssl/mutualTLS"
                                                + " must be a boolean")))
                .map(
                        new StringIsNotBlank(
                                ConfigurationException.supplier(
                                        "Invalid configuration for"
                                                + " /httpServer/ssl/mutualTLS"
                                                + " must not be blank")))
                .map(
                        new ToBoolean(
                                ConfigurationException.supplier(
                                        "Invalid configuration for"
                                                + " /httpServer/ssl/mutualTLS"
                                                + " must be a boolean")))
                .orElse(false);
    }

    private static Optional<String[]> getProtocolsProperties(MapAccessor rootMapAccessor) {
        return getPropertiesFromCommaSeparatedStringAsArray(rootMapAccessor, "protocols");
    }

    private static Optional<String[]> getCiphersProperties(MapAccessor rootMapAccessor) {
        return getPropertiesFromCommaSeparatedStringAsArray(rootMapAccessor, "ciphers");
    }

    private static Optional<String[]> getPropertiesFromCommaSeparatedStringAsArray(
            MapAccessor rootMapAccessor, String property) {
        return rootMapAccessor
                .get("/httpServer/ssl/" + property)
                .map(
                        new ToString(
                                ConfigurationException.supplier(
                                        "Invalid configuration for"
                                                + " /httpServer/ssl/"
                                                + property
                                                + " must be a string")))
                .map(
                        new StringIsNotBlank(
                                ConfigurationException.supplier(
                                        "Invalid configuration for"
                                                + " /httpServer/ssl/"
                                                + property
                                                + " must not be blank")))
                .map(value -> value.split(COMMA_SEPARATOR))
                .map(
                        values ->
                                Arrays.stream(values)
                                        .map(String::trim)
                                        .filter(value -> !value.isEmpty())
                                        .toArray(String[]::new))
                .filter(values -> values.length > 0);
    }

    private static Optional<KeyStoreProperties> getTrustStoreProperties() {
        return Optional.ofNullable(trustStoreProperties);
    }

    /**
     * Class to implement a named thread factory
     *
     * <p>Copied from the `prometheus/client_java` `HTTPServer` due to scoping issues / dependencies
     */
    private static class NamedDaemonThreadFactory implements ThreadFactory {

        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

        private final int poolNumber = POOL_NUMBER.getAndIncrement();
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final ThreadFactory delegate;
        private final boolean daemon;

        NamedDaemonThreadFactory(ThreadFactory delegate, boolean daemon) {
            this.delegate = delegate;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setName(format("prometheus-http-%d-%d", poolNumber, threadNumber.getAndIncrement()));
            t.setDaemon(daemon);
            return t;
        }

        static ThreadFactory defaultThreadFactory(boolean daemon) {
            return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    /** Class to implement a blocking RejectedExecutionHandler */
    private static class BlockingRejectedExecutionHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (!threadPoolExecutor.isShutdown()) {
                try {
                    threadPoolExecutor.getQueue().put(runnable);
                } catch (InterruptedException e) {
                    // INTENTIONALLY BLANK
                }
            }
        }
    }

    private static final class KeyStoreProperties {

        private final Path filename;
        private final Instant lastModifiedTime;
        private final char[] password;
        private final String type;
        private final String certificateAlias;

        private KeyStoreProperties(
                String filename,
                Instant lastModifiedTime,
                char[] password,
                String type,
                String certificateAlias) {

            this.filename = Paths.get(filename);
            this.lastModifiedTime = lastModifiedTime;
            this.password = password;
            this.type = type;
            this.certificateAlias = certificateAlias;
        }

        public Path getFilename() {
            return filename;
        }

        public Instant getLastModifiedTime() {
            return lastModifiedTime;
        }

        public char[] getPassword() {
            return password;
        }

        public String getType() {
            return type;
        }

        public Optional<String> getCertificateAlias() {
            return Optional.ofNullable(certificateAlias);
        }
    }
}
