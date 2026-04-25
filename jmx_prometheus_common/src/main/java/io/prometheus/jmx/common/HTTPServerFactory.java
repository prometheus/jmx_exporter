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
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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
import io.prometheus.metrics.config.PrometheusProperties;
import io.prometheus.metrics.exporter.httpserver.DefaultHandler;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.exporter.httpserver.HealthyHandler;
import io.prometheus.metrics.exporter.httpserver.MetricsHandler;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * Factory for creating and configuring HTTP servers for the JMX exporter.
 *
 * <p>This factory creates HTTP servers with support for:
 *
 * <ul>
 *   <li>Configurable thread pools
 *   <li>Basic authentication (plaintext, SHA, or PBKDF2)
 *   <li>Custom authenticator plugins
 *   <li>SSL/TLS with automatic certificate reloading
 *   <li>Two-way TLS (mTLS) client authentication
 * </ul>
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. Configuration loading and SSL certificate reloading
 * use synchronization where necessary.
 */
public class HTTPServerFactory {

    /**
     * System property for keystore path.
     */
    private static final String JAVAX_NET_SSL_KEY_STORE = "javax.net.ssl.keyStore";

    /**
     * System property for keystore type.
     */
    private static final String JAVAX_NET_SSL_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";

    /**
     * System property for keystore password.
     */
    private static final String JAVAX_NET_SSL_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";

    /**
     * Default keystore type, determined from system property or platform default.
     */
    private static final String DEFAULT_KEYSTORE_TYPE;

    /**
     * System property for truststore path.
     */
    private static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";

    /**
     * System property for truststore type.
     */
    private static final String JAVAX_NET_SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    /**
     * System property for truststore password.
     */
    private static final String JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    /**
     * Default truststore type, determined from system property or platform default.
     */
    private static final String DEFAULT_TRUST_STORE_TYPE;

    /**
     * Default minimum thread pool size.
     */
    private static final int DEFAULT_MINIMUM_THREADS = 1;

    /**
     * Default maximum thread pool size.
     */
    private static final int DEFAULT_MAXIMUM_THREADS = 10;

    /**
     * Default thread keep-alive time in seconds.
     */
    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 120;

    /**
     * HTTP authentication realm.
     */
    private static final String REALM = "/";

    /**
     * Default metrics endpoint path.
     */
    private static final String METRICS_PATH = "/metrics";

    /**
     * Default health endpoint path.
     */
    private static final String HEALTH_PATH = "/-/healthy";

    /**
     * Security header names and values.
     */
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    private static final String NOSNIFF = "nosniff";

    private static final String X_FRAME_OPTIONS = "X-Frame-Options";

    private static final String DENY = "DENY";

    private static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    private static final String STRICT_TRANSPORT_SECURITY_VALUE = "max-age=31536000";

    /**
     * Plaintext algorithm identifier for basic authentication.
     */
    private static final String PLAINTEXT = "plaintext";

    /**
     * Supported SHA algorithm names for password hashing.
     */
    private static final Set<String> SHA_ALGORITHMS;

    /**
     * Supported PBKDF2 algorithm names for password hashing.
     */
    private static final Set<String> PBKDF2_ALGORITHMS;

    /**
     * Default iteration counts for PBKDF2 algorithms.
     *
     * <p>Each algorithm has a recommended iteration count based on OWASP guidelines.
     */
    private static final Map<String, Integer> PBKDF2_ALGORITHM_ITERATIONS;

    /**
     * Default key length in bits for PBKDF2 key derivation.
     */
    private static final int PBKDF2_KEY_LENGTH_BITS = 128;

    /**
     * Scheduled executor for periodic SSL certificate reloading.
     *
     * <p>Checks for updated certificates every hour.
     */
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    /**
     * Comma separator for parsing SSL configuration values.
     */
    private static final String COMMA_SEPARATOR = ",";

    /**
     * Current keystore properties, updated when certificates are reloaded.
     */
    private static KeyStoreProperties keyStoreProperties;

    /**
     * Current truststore properties, updated when certificates are reloaded.
     */
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

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private HTTPServerFactory() {
        // INTENTIONALLY BLANK
    }

    /**
     * Creates and starts an HTTP server with the specified configuration.
     *
     * <p>The HTTP server is configured based on the YAML configuration file, including:
     * thread pool settings, authentication, and SSL/TLS.
     *
     * @param prometheusRegistry the Prometheus registry for metric collection, must not be
     *     {@code null}
     * @param inetAddress the network address to bind to, must not be {@code null}
     * @param port the port number to listen on, must be a valid port (0-65535)
     * @param exporterYamlFile the YAML configuration file, must not be {@code null}
     * @return the started HTTP server instance
     * @throws IOException if the server fails to start or configuration cannot be read
     * @throws ConfigurationException if the configuration is invalid
     */
    public static HTTPServer createAndStartHTTPServer(
            PrometheusRegistry prometheusRegistry, InetAddress inetAddress, int port, File exporterYamlFile)
            throws IOException {
        MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));
        AuthenticationConfiguration authenticationConfiguration = getAuthenticationConfiguration(rootMapAccessor);
        boolean sslEnabled = rootMapAccessor.containsPath("/httpServer/ssl");

        HTTPServer.Builder httpServerBuilder =
                HTTPServer.builder().inetAddress(inetAddress).port(port).registry(prometheusRegistry);

        configureThreads(rootMapAccessor, httpServerBuilder);
        configureAuthentication(authenticationConfiguration, httpServerBuilder);
        configureSSL(rootMapAccessor, httpServerBuilder);

        HTTPServer httpServer = httpServerBuilder.buildAndStart();
        configureSecurityHeaders(httpServer, prometheusRegistry, authenticationConfiguration, sslEnabled);
        return httpServer;
    }

    /**
     * Creates and starts an HTTP server with the specified configuration (testing variant).
     *
     * <p>This variant does not bind to a specific address and is primarily used for testing.
     * The HTTP server is configured based on the YAML configuration file.
     *
     * @param prometheusRegistry the Prometheus registry for metric collection, must not be
     *     {@code null}
     * @param exporterYamlFile the YAML configuration file, must not be {@code null}
     * @return the started HTTP server instance
     * @throws IOException if the server fails to start or configuration cannot be read
     * @throws ConfigurationException if the configuration is invalid
     */
    public static HTTPServer createAndStartHTTPServer(PrometheusRegistry prometheusRegistry, File exporterYamlFile)
            throws IOException {
        MapAccessor rootMapAccessor = MapAccessor.of(YamlSupport.loadYaml(exporterYamlFile));
        AuthenticationConfiguration authenticationConfiguration = getAuthenticationConfiguration(rootMapAccessor);
        boolean sslEnabled = rootMapAccessor.containsPath("/httpServer/ssl");

        HTTPServer.Builder httpServerBuilder = HTTPServer.builder().registry(prometheusRegistry);

        configureThreads(rootMapAccessor, httpServerBuilder);
        configureAuthentication(authenticationConfiguration, httpServerBuilder);
        configureSSL(rootMapAccessor, httpServerBuilder);

        HTTPServer httpServer = httpServerBuilder.buildAndStart();
        configureSecurityHeaders(httpServer, prometheusRegistry, authenticationConfiguration, sslEnabled);
        return httpServer;
    }

    /**
     * Configures the HTTP server thread pool based on YAML configuration.
     *
     * <p>Thread pool configuration is read from the {@code /httpServer/threads} path. If not
     * specified, default values are used: minimum=1, maximum=10, keepAliveTime=120 seconds.
     *
     * @param rootMapAccessor the root configuration map accessor, must not be {@code null}
     * @param httpServerBuilder the HTTP server builder to configure, must not be {@code null}
     * @throws ConfigurationException if thread pool configuration is invalid
     */
    private static void configureThreads(MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        int minimum = DEFAULT_MINIMUM_THREADS;
        int maximum = DEFAULT_MAXIMUM_THREADS;
        int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME_SECONDS;

        if (rootMapAccessor.containsPath("/httpServer/threads")) {
            MapAccessor httpServerThreadsMapAccessor = rootMapAccessor
                    .get("/httpServer/threads")
                    .map(new ToMapAccessor(ConfigurationException.supplier(
                            "Invalid configuration for /httpServer/threads" + " must be a map")))
                    .orElseThrow(ConfigurationException.supplier("/httpServer/threads must be a map"));

            minimum = httpServerThreadsMapAccessor
                    .get("/minimum")
                    .map(new ToInteger(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/threads/minimum must be an" + " integer")))
                    .map(new IntegerInRange(
                            1,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/minimum must be 1"
                                    + " or greater")))
                    .orElseThrow(ConfigurationException.supplier("/httpServer/threads/minimum is a required integer"));

            maximum = httpServerThreadsMapAccessor
                    .get("/maximum")
                    .map(new ToInteger(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/threads/maximum must be an" + " integer")))
                    .map(new IntegerInRange(
                            minimum,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/maxPoolSize must be"
                                    + " between greater than 0")))
                    .orElseThrow(ConfigurationException.supplier("/httpServer/threads/maximum is a required integer"));

            keepAliveTime = httpServerThreadsMapAccessor
                    .get("/keepAliveTime")
                    .map(new ToInteger(ConfigurationException.supplier("Invalid configuration for"
                            + " /httpServer/threads/keepAliveTime must"
                            + " be an integer")))
                    .map(new IntegerInRange(
                            1,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/keepAliveTime must"
                                    + " be greater than 0")))
                    .orElseThrow(ConfigurationException.supplier(
                            "/httpServer/threads/keepAliveTime is a required" + " integer"));
        }

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
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
     * Configures HTTP basic authentication based on YAML configuration.
     *
     * <p>Supports three authentication mechanisms:
     *
     * <ul>
     *   <li>Plaintext password (not recommended for production)
     *   <li>SHA-1/SHA-256/SHA-512 hashed passwords with salt
     *   <li>PBKDF2 hashed passwords with configurable iterations
     *   <li>Custom authenticator plugins via class name
     * </ul>
     *
     * @param rootMapAccessor the root configuration map accessor, must not be {@code null}
     * @param httpServerBuilder the HTTP server builder to configure, must not be {@code null}
     * @throws ConfigurationException if authentication configuration is invalid
     */
    private static void configureAuthentication(
            AuthenticationConfiguration authenticationConfiguration, HTTPServer.Builder httpServerBuilder) {
        if (authenticationConfiguration.getAuthenticator() != null) {
            httpServerBuilder.authenticator(authenticationConfiguration.getAuthenticator());
        }

        if (authenticationConfiguration.getSubjectAttributeName() != null) {
            httpServerBuilder.authenticatedSubjectAttributeName(authenticationConfiguration.getSubjectAttributeName());
        }
    }

    private static AuthenticationConfiguration getAuthenticationConfiguration(MapAccessor rootMapAccessor) {
        Authenticator authenticator = null;
        String subjectAttributeName = null;

        if (rootMapAccessor.containsPath("/httpServer/authentication")) {
            Optional<Object> authenticatorClassAttribute = rootMapAccessor.get("/httpServer/authentication/plugin");

            if (authenticatorClassAttribute.isPresent()) {
                MapAccessor httpServerAuthenticationCustomAuthenticatorMapAccessor = rootMapAccessor
                        .get("/httpServer/authentication/plugin")
                        .map(new ToMapAccessor(ConfigurationException.supplier(
                                "Invalid configuration for" + " /httpServer/authentication/plugin" + " must be a map")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/plugin" + " must be a map"));

                String authenticatorClass = httpServerAuthenticationCustomAuthenticatorMapAccessor
                        .get("/class")
                        .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/plugin/class"
                                + " must be a string")))
                        .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/plugin/class"
                                + " must not be blank")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/plugin/class must be a" + " string"));

                Optional<Object> subjectAttribute =
                        httpServerAuthenticationCustomAuthenticatorMapAccessor.get("/subjectAttributeName");

                if (subjectAttribute.isPresent()) {
                    subjectAttributeName = subjectAttribute
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/plugin/class/subjectAttributeName"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/plugin/subjectAttributeName"
                                    + " must not be blank")))
                            .get();
                }

                authenticator = loadAuthenticator(authenticatorClass);
            } else {
                MapAccessor httpServerAuthenticationBasicMapAccessor = rootMapAccessor
                        .get("/httpServer/authentication/basic")
                        .map(new ToMapAccessor(ConfigurationException.supplier(
                                "Invalid configuration for" + " /httpServer/authentication/basic")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/basic configuration" + " must be a map"));

                String username = httpServerAuthenticationBasicMapAccessor
                        .get("/username")
                        .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/username"
                                + " must be a string")))
                        .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/username"
                                + " must not be blank")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/basic/username is a" + " required string"));

                username = VariableResolver.resolveVariable(username);

                String algorithm = httpServerAuthenticationBasicMapAccessor
                        .get("/algorithm")
                        .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/algorithm"
                                + " must be a string")))
                        .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/algorithm"
                                + " must not be blank")))
                        .orElse(PLAINTEXT);

                if (PLAINTEXT.equalsIgnoreCase(algorithm)) {
                    String password = httpServerAuthenticationBasicMapAccessor
                            .get("/password")
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/password"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/password"
                                    + " must not be blank")))
                            .orElseThrow(ConfigurationException.supplier(
                                    "/httpServer/authentication/basic/password" + " is a required string"));

                    password = VariableResolver.resolveVariable(password);
                    authenticator = new PlaintextAuthenticator("/", username, password);
                } else if (SHA_ALGORITHMS.contains(algorithm) || PBKDF2_ALGORITHMS.contains(algorithm)) {
                    String hash = httpServerAuthenticationBasicMapAccessor
                            .get("/passwordHash")
                            .map(new ToString(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/passwordHash"
                                    + " must be a string")))
                            .map(new StringIsNotBlank(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/passwordHash"
                                    + " must not be blank")))
                            .orElseThrow(ConfigurationException.supplier(
                                    "/httpServer/authentication/basic/passwordHash" + " is a required string"));

                    if (SHA_ALGORITHMS.contains(algorithm)) {
                        authenticator = createMessageDigestAuthenticator(
                                httpServerAuthenticationBasicMapAccessor, REALM, username, hash, algorithm);
                    } else {
                        authenticator = createPBKDF2Authenticator(
                                httpServerAuthenticationBasicMapAccessor, REALM, username, hash, algorithm);
                    }
                } else {
                    throw new ConfigurationException(
                            format("Unsupported /httpServer/authentication/basic/algorithm" + " [%s]", algorithm));
                }
            }
        }

        return new AuthenticationConfiguration(authenticator, subjectAttributeName);
    }

    /**
     * Loads a custom authenticator class by name.
     *
     * <p>The authenticator class must have a no-argument constructor and extend or implement
     * {@link Authenticator}.
     *
     * @param className the fully qualified class name of the authenticator
     * @return a new instance of the authenticator
     * @throws ConfigurationException if the class cannot be loaded or instantiated
     */
    private static Authenticator loadAuthenticator(String className) {
        Class<?> clazz;

        try {
            clazz = HTTPServerFactory.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(format(
                    "configured /httpServer/authentication/authenticatorClass [%s]"
                            + " not found, loadClass resulted in [%s:%s]",
                    className, e.getClass(), e.getMessage()));
        }

        if (!Authenticator.class.isAssignableFrom(clazz)) {
            throw new ConfigurationException(format(
                    "configured /httpServer/authentication/authenticatorClass [%s]"
                            + " loadClass resulted in [%s] of the wrong type, is not assignable"
                            + " from Authenticator",
                    className, clazz.getCanonicalName()));
        }

        try {
            return (Authenticator) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ConfigurationException(format(
                    "configured /httpServer/authentication/authenticatorClass [%s] no arg"
                            + " constructor newInstance resulted in exception [%s:%s]",
                    className, e.getClass(), e.getMessage()));
        }
    }

    private static void configureSecurityHeaders(
            HTTPServer httpServer,
            PrometheusRegistry prometheusRegistry,
            AuthenticationConfiguration authenticationConfiguration,
            boolean sslEnabled) {
        com.sun.net.httpserver.HttpServer delegate = getDelegateHttpServer(httpServer);
        Authenticator securityHeadersAuthenticator =
                wrapAuthenticator(authenticationConfiguration.getAuthenticator(), sslEnabled);
        String subjectAttributeName = authenticationConfiguration.getSubjectAttributeName();

        replaceContext(
                delegate,
                "/",
                wrapHandler(new DefaultHandler(METRICS_PATH), sslEnabled, subjectAttributeName),
                securityHeadersAuthenticator);
        replaceContext(
                delegate,
                METRICS_PATH,
                wrapHandler(
                        new MetricsHandler(PrometheusProperties.get(), prometheusRegistry),
                        sslEnabled,
                        subjectAttributeName),
                securityHeadersAuthenticator);
        replaceContext(
                delegate,
                HEALTH_PATH,
                wrapHandler(new HealthyHandler(), sslEnabled, subjectAttributeName),
                securityHeadersAuthenticator);
    }

    private static void replaceContext(
            com.sun.net.httpserver.HttpServer httpServer,
            String path,
            HttpHandler handler,
            Authenticator authenticator) {
        try {
            httpServer.removeContext(path);
        } catch (IllegalArgumentException e) {
            // context not registered yet, ignore
        }

        HttpContext context = httpServer.createContext(path, handler);
        if (authenticator != null) {
            context.setAuthenticator(authenticator);
        }
    }

    private static com.sun.net.httpserver.HttpServer getDelegateHttpServer(HTTPServer httpServer) {
        try {
            Field field = HTTPServer.class.getDeclaredField("server");
            field.setAccessible(true);
            return (com.sun.net.httpserver.HttpServer) field.get(httpServer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to access underlying HTTP server", e);
        }
    }

    private static HttpHandler wrapHandler(HttpHandler handler, boolean sslEnabled, String subjectAttributeName) {
        return new SecurityHeadersHandler(handler, sslEnabled, subjectAttributeName);
    }

    private static Authenticator wrapAuthenticator(Authenticator authenticator, boolean sslEnabled) {
        if (authenticator == null) {
            return null;
        }

        return new SecurityHeadersAuthenticator(authenticator, sslEnabled);
    }

    private static void addSecurityHeaders(Headers headers, boolean sslEnabled) {
        headers.set(X_CONTENT_TYPE_OPTIONS, NOSNIFF);
        headers.set(X_FRAME_OPTIONS, DENY);
        if (sslEnabled) {
            headers.set(STRICT_TRANSPORT_SECURITY, STRICT_TRANSPORT_SECURITY_VALUE);
        }
    }

    private static void drainInputAndClose(HttpExchange httpExchange) throws IOException {
        InputStream inputStream = httpExchange.getRequestBody();
        byte[] bytes = new byte[4096];
        while (inputStream.read(bytes) != -1) {
            // INTENTIONALLY BLANK
        }
        inputStream.close();
    }

    /**
     * Creates a MessageDigestAuthenticator for SHA-based password hashing.
     *
     * @param httpServerAuthenticationBasicMapAccessor the authentication configuration accessor
     * @param realm the authentication realm
     * @param username the username
     * @param password the password hash
     * @param algorithm the SHA algorithm (SHA-1, SHA-256, or SHA-512)
     * @return the configured authenticator
     * @throws ConfigurationException if the algorithm is unsupported or salt is missing
     */
    private static Authenticator createMessageDigestAuthenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt = httpServerAuthenticationBasicMapAccessor
                .get("/salt")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " not be blank")))
                .orElseThrow(ConfigurationException.supplier(
                        "/httpServer/authentication/basic/salt is a required" + " string"));

        try {
            return new MessageDigestAuthenticator(realm, username, password, algorithm, salt);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(format(
                    "Invalid /httpServer/authentication/basic/algorithm, unsupported" + " algorithm [%s]", algorithm));
        }
    }

    /**
     * Creates a PBKDF2Authenticator for PBKDF2-based password hashing.
     *
     * @param httpServerAuthenticationBasicMapAccessor the authentication configuration accessor
     * @param realm the authentication realm
     * @param username the username
     * @param password the password hash
     * @param algorithm the PBKDF2 algorithm (PBKDF2WithHmacSHA1, PBKDF2WithHmacSHA256, or
     *     PBKDF2WithHmacSHA512)
     * @return the configured authenticator
     * @throws ConfigurationException if the algorithm is unsupported or required parameters are
     *     missing
     */
    private static Authenticator createPBKDF2Authenticator(
            MapAccessor httpServerAuthenticationBasicMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt = httpServerAuthenticationBasicMapAccessor
                .get("/salt")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be not blank")))
                .orElseThrow(ConfigurationException.supplier(
                        "/httpServer/authentication/basic/salt is a required" + " string"));

        int iterations = httpServerAuthenticationBasicMapAccessor
                .get("/iterations")
                .map(new ToInteger(ConfigurationException.supplier("Invalid configuration for"
                        + " /httpServer/authentication/basic/iterations"
                        + " must be an integer")))
                .map(new IntegerInRange(
                        1,
                        Integer.MAX_VALUE,
                        ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/iterations"
                                + " must be between greater than 0")))
                .orElse(PBKDF2_ALGORITHM_ITERATIONS.get(algorithm));

        int keyLength = httpServerAuthenticationBasicMapAccessor
                .get("/keyLength")
                .map(new ToInteger(ConfigurationException.supplier("Invalid configuration for"
                        + " /httpServer/authentication/basic/keyLength"
                        + " must be an integer")))
                .map(new IntegerInRange(
                        1,
                        Integer.MAX_VALUE,
                        ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/keyLength"
                                + " must be greater than 0")))
                .orElse(PBKDF2_KEY_LENGTH_BITS);

        try {
            return new PBKDF2Authenticator(realm, username, password, algorithm, salt, iterations, keyLength);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(format(
                    "Invalid /httpServer/authentication/basic/algorithm, unsupported" + " algorithm [%s]", algorithm));
        }
    }

    /**
     * Configures SSL/TLS for the HTTP server based on YAML configuration.
     *
     * <p>Supports:
     *
     * <ul>
     *   <li>Keystore-based SSL with optional password (can use system properties)
     *   <li>Truststore-based two-way TLS (mTLS)
     *   <li>Configurable protocols and cipher suites
     *   <li>Automatic certificate reloading (checked hourly)
     * </ul>
     *
     * @param rootMapAccessor the root configuration map accessor, must not be {@code null}
     * @param httpServerBuilder the HTTP server builder to configure, must not be {@code null}
     * @throws ConfigurationException if SSL configuration is invalid or certificates cannot be
     *     loaded
     */
    public static void configureSSL(MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        if (rootMapAccessor.containsPath("/httpServer/ssl")) {
            try {
                SSLFactory sslFactory = createSslFactory(rootMapAccessor);
                Runnable sslUpdater = () -> reloadSsl(sslFactory, rootMapAccessor);
                // check every hour for file changes and if it has been modified update the ssl
                // configuration
                EXECUTOR_SERVICE.scheduleAtFixedRate(sslUpdater, 1, 1, TimeUnit.HOURS);

                httpServerBuilder.httpsConfigurator(new HttpsConfigurator(sslFactory.getSslContext()));
            } catch (GenericException e) {
                String message = e.getMessage();
                if (message != null && !message.trim().isEmpty()) {
                    message = ", " + message.trim();
                } else {
                    message = "";
                }

                throw new ConfigurationException(format("Exception loading SSL configuration%s", message), e);
            }
        }
    }

    /**
     * Creates an SSLFactory from the configuration.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return the configured SSLFactory
     * @throws ConfigurationException if SSL configuration is invalid
     */
    private static SSLFactory createSslFactory(MapAccessor rootMapAccessor) {
        keyStoreProperties = getKeyStoreProperties(rootMapAccessor);
        Optional<KeyStoreProperties> trustProps = getTrustStoreProperties(rootMapAccessor);

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder()
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

    /**
     * Reloads SSL certificates if the keystore or truststore file contents have changed.
     *
     * <p>This method is called periodically by the scheduled executor to check for certificate
     * updates. If either keystore or truststore content has changed since last load, the SSL
     * context is updated.
     *
     * @param sslFactory the SSLFactory to reload
     * @param rootMapAccessor the root configuration map accessor
     */
    private static void reloadSsl(SSLFactory sslFactory, MapAccessor rootMapAccessor) {
        Optional<String> currentKeyStoreContentHash = getContentHash(keyStoreProperties.getFilename());
        if (!currentKeyStoreContentHash.isPresent()) {
            return;
        }

        Optional<KeyStoreProperties> currentTrustProps = getTrustStoreProperties();
        Optional<String> currentTrustStoreContentHash = Optional.empty();
        if (currentTrustProps.isPresent()) {
            currentTrustStoreContentHash =
                    getContentHash(currentTrustProps.get().getFilename());
            if (!currentTrustStoreContentHash.isPresent()) {
                return;
            }
        }

        boolean keyStoreChanged = !keyStoreProperties.getContentHash().equals(currentKeyStoreContentHash.get());
        boolean trustStoreChanged = currentTrustProps.isPresent()
                && !currentTrustProps
                        .get()
                        .getContentHash()
                        .equals(currentTrustStoreContentHash.orElseThrow(IllegalStateException::new));

        if (!keyStoreChanged && !trustStoreChanged) {
            return;
        }

        final KeyStoreProperties keyProps;
        final Optional<KeyStoreProperties> trustProps;
        try {
            keyProps = keyStoreChanged ? getKeyStoreProperties(rootMapAccessor) : keyStoreProperties;
            trustProps = trustStoreChanged ? getTrustStoreProperties(rootMapAccessor) : currentTrustProps;
        } catch (ConfigurationException e) {
            return;
        }

        boolean sslUpdated = false;
        SSLFactory.Builder updatedSslFactory = SSLFactory.builder();
        if (keyStoreChanged) {
            updatedSslFactory.withIdentityMaterial(
                    keyProps.getFilename(), keyProps.getPassword(), keyProps.getPassword(), keyProps.getType());
            sslUpdated = true;
        }

        if (trustStoreChanged && trustProps.isPresent()) {
            updatedSslFactory.withTrustMaterial(
                    trustProps.get().getFilename(),
                    trustProps.get().getPassword(),
                    trustProps.get().getType());
            sslUpdated = true;
        }

        if (sslUpdated) {
            try {
                SSLFactoryUtils.reload(sslFactory, updatedSslFactory.build());
                if (keyStoreChanged) {
                    keyStoreProperties = keyProps;
                }
                if (trustStoreChanged && trustProps.isPresent()) {
                    trustStoreProperties = trustProps.get();
                }
            } catch (RuntimeException e) {
                // Keep using the last successfully loaded SSL material.
            }
        }
    }

    /**
     * Extracts keystore properties from the configuration.
     *
     * <p>Keystore can be configured via YAML or system properties. System properties are used
     * as fallbacks when YAML values are not specified.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return the keystore properties
     * @throws ConfigurationException if required properties are missing
     */
    private static KeyStoreProperties getKeyStoreProperties(MapAccessor rootMapAccessor) {
        String keyStoreFilename = rootMapAccessor
                .get("/httpServer/ssl/keyStore/filename")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/filename" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/filename" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE));

        String contentHash = getContentHash(keyStoreFilename);

        String keyStoreType = rootMapAccessor
                .get("/httpServer/ssl/keyStore/type")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/type" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/type" + " must not be blank")))
                .orElse(DEFAULT_KEYSTORE_TYPE);

        String keyStorePassword = rootMapAccessor
                .get("/httpServer/ssl/keyStore/password")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/password" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/password" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE_PASSWORD));

        // Resolve the password
        keyStorePassword = VariableResolver.resolveVariable(keyStorePassword);

        String certificateAlias = rootMapAccessor
                .get("/httpServer/ssl/certificate/alias")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/certificate/alias" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/certificate/alias" + " must not be blank")))
                .orElseThrow(
                        ConfigurationException.supplier("/httpServer/ssl/certificate/alias is a required" + " string"));

        return new KeyStoreProperties(
                keyStoreFilename, contentHash, keyStorePassword.toCharArray(), keyStoreType, certificateAlias);
    }

    /**
     * Computes the SHA-256 content hash of a file.
     *
     * @param filename the file path
     * @return the SHA-256 content hash
     */
    private static String getContentHash(String filename) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];

            try (InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    messageDigest.update(buffer, 0, bytesRead);
                }
            }

            return toHex(messageDigest.digest());
        } catch (IOException e) {
            throw new ConfigurationException(format("Unable to read SSL store file: %s", filename), e);
        } catch (NoSuchAlgorithmException e) {
            throw new ConfigurationException("Unable to compute SSL store file hash using SHA-256", e);
        }
    }

    /**
     * Computes the SHA-256 content hash of a file for runtime reload checks.
     *
     * <p>If the file cannot be read while the server is already running, the reload should be
     * skipped and the last successfully loaded SSL material should continue to be used.
     *
     * @param filename the file path
     * @return the SHA-256 content hash, or empty if the file cannot be read
     */
    private static Optional<String> getContentHash(Path filename) {
        try {
            return Optional.of(getContentHash(filename.toString()));
        } catch (ConfigurationException e) {
            return Optional.empty();
        }
    }

    /**
     * Converts bytes to a lowercase hexadecimal string.
     *
     * @param bytes the bytes to convert
     * @return the hexadecimal string
     */
    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    /**
     * Extracts truststore properties from the configuration.
     *
     * <p>Truststore configuration is only required when mTLS is enabled. System properties are
     * used as fallbacks when YAML values are not specified.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return an Optional containing truststore properties, or empty if mTLS is disabled
     * @throws ConfigurationException if required properties are missing when mTLS is enabled
     */
    private static Optional<KeyStoreProperties> getTrustStoreProperties(MapAccessor rootMapAccessor) {
        final boolean mutualTLS = isMutualTls(rootMapAccessor);
        if (!mutualTLS) {
            return Optional.empty();
        }

        String trustStoreFilename = rootMapAccessor
                .get("/httpServer/ssl/trustStore/filename")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/filename" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/filename" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE));

        String contentHash = getContentHash(trustStoreFilename);

        String trustStoreType = rootMapAccessor
                .get("/httpServer/ssl/trustStore/type")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/type" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/type" + " must not be blank")))
                .orElse(DEFAULT_TRUST_STORE_TYPE);

        String trustStorePassword = rootMapAccessor
                .get("/httpServer/ssl/trustStore/password")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/password" + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/password" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD));

        // Resolve the password
        trustStorePassword = VariableResolver.resolveVariable(trustStorePassword);

        return Optional.of(new KeyStoreProperties(
                trustStoreFilename, contentHash, trustStorePassword.toCharArray(), trustStoreType, null));
    }

    /**
     * Determines if mutual TLS (mTLS) is enabled in the configuration.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return {@code true} if mTLS is enabled, {@code false} otherwise
     */
    private static boolean isMutualTls(MapAccessor rootMapAccessor) {
        return rootMapAccessor
                .get("/httpServer/ssl/mutualTLS")
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/mutualTLS" + " must be a boolean")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/mutualTLS" + " must not be blank")))
                .map(new ToBoolean(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/mutualTLS" + " must be a boolean")))
                .orElse(false);
    }

    /**
     * Gets the SSL protocols configuration.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return an Optional containing the protocols array, or empty if not configured
     */
    private static Optional<String[]> getProtocolsProperties(MapAccessor rootMapAccessor) {
        return getPropertiesFromCommaSeparatedStringAsArray(rootMapAccessor, "protocols");
    }

    /**
     * Gets the SSL cipher suites configuration.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @return an Optional containing the cipher suites array, or empty if not configured
     */
    private static Optional<String[]> getCiphersProperties(MapAccessor rootMapAccessor) {
        return getPropertiesFromCommaSeparatedStringAsArray(rootMapAccessor, "ciphers");
    }

    /**
     * Parses a comma-separated string from the SSL configuration into an array.
     *
     * @param rootMapAccessor the root configuration map accessor
     * @param property the property name (e.g., "protocols" or "ciphers")
     * @return an Optional containing the parsed array, or empty if not configured
     */
    private static Optional<String[]> getPropertiesFromCommaSeparatedStringAsArray(
            MapAccessor rootMapAccessor, String property) {
        return rootMapAccessor
                .get("/httpServer/ssl/" + property)
                .map(new ToString(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/" + property + " must be a string")))
                .map(new StringIsNotBlank(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/" + property + " must not be blank")))
                .map(value -> value.split(COMMA_SEPARATOR))
                .map(values -> Arrays.stream(values)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .toArray(String[]::new))
                .filter(values -> values.length > 0);
    }

    /**
     * Gets the current truststore properties.
     *
     * @return an Optional containing truststore properties, or empty if not configured
     */
    private static Optional<KeyStoreProperties> getTrustStoreProperties() {
        return Optional.ofNullable(trustStoreProperties);
    }

    /**
     * Thread factory for creating named daemon threads for the HTTP server thread pool.
     *
     * <p>Threads are named with the pattern {@code prometheus-http-{pool}-{thread}}.
     *
     * <p>Copied from {@code prometheus/client_java} HTTPServer due to scoping issues.
     */
    private static class NamedDaemonThreadFactory implements ThreadFactory {

        /**
         * Counter for generating unique pool numbers.
         */
        private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

        /**
         * The pool number for this factory instance.
         */
        private final int poolNumber = POOL_NUMBER.getAndIncrement();

        /**
         * Counter for generating unique thread numbers within the pool.
         */
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        /**
         * The delegate thread factory.
         */
        private final ThreadFactory delegate;

        /**
         * Whether created threads should be daemon threads.
         */
        private final boolean daemon;

        /**
         * Constructs a named daemon thread factory.
         *
         * @param delegate the delegate thread factory
         * @param daemon whether created threads should be daemon threads
         */
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

        /**
         * Creates a default thread factory that produces daemon threads.
         *
         * @param daemon whether created threads should be daemon threads
         * @return a new thread factory
         */
        static ThreadFactory defaultThreadFactory(boolean daemon) {
            return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
        }
    }

    private static final class AuthenticationConfiguration {

        private final Authenticator authenticator;

        private final String subjectAttributeName;

        private AuthenticationConfiguration(Authenticator authenticator, String subjectAttributeName) {
            this.authenticator = authenticator;
            this.subjectAttributeName = subjectAttributeName;
        }

        private Authenticator getAuthenticator() {
            return authenticator;
        }

        private String getSubjectAttributeName() {
            return subjectAttributeName;
        }
    }

    private static final class SecurityHeadersAuthenticator extends Authenticator {

        private final Authenticator delegate;

        private final boolean sslEnabled;

        private SecurityHeadersAuthenticator(Authenticator delegate, boolean sslEnabled) {
            this.delegate = delegate;
            this.sslEnabled = sslEnabled;
        }

        @Override
        public Result authenticate(HttpExchange exchange) {
            addSecurityHeaders(exchange.getResponseHeaders(), sslEnabled);
            return delegate.authenticate(exchange);
        }
    }

    private static final class SecurityHeadersHandler implements HttpHandler {

        private final HttpHandler delegate;

        private final boolean sslEnabled;

        private final String subjectAttributeName;

        private SecurityHeadersHandler(HttpHandler delegate, boolean sslEnabled, String subjectAttributeName) {
            this.delegate = delegate;
            this.sslEnabled = sslEnabled;
            this.subjectAttributeName = subjectAttributeName;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addSecurityHeaders(exchange.getResponseHeaders(), sslEnabled);

            if (subjectAttributeName == null) {
                delegate.handle(exchange);
                return;
            }

            Object authSubject = exchange.getAttribute(subjectAttributeName);
            if (authSubject instanceof javax.security.auth.Subject) {
                try {
                    javax.security.auth.Subject.doAs(
                            (javax.security.auth.Subject) authSubject,
                            (java.security.PrivilegedExceptionAction<Void>) () -> {
                                delegate.handle(exchange);
                                return null;
                            });
                } catch (java.security.PrivilegedActionException e) {
                    if (e.getException() != null) {
                        throw new IOException(e.getException());
                    } else {
                        throw new IOException(e);
                    }
                }
            } else {
                drainInputAndClose(exchange);
                exchange.sendResponseHeaders(403, -1);
            }
        }
    }

    /**
     * Rejected execution handler that blocks when the thread pool queue is full.
     *
     * <p>Instead of rejecting tasks when the queue is full, this handler attempts to put the
     * task into the queue, blocking until space is available.
     */
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

    /**
     * Immutable holder for keystore or truststore properties.
     *
     * <p>Stores the file path, content hash, password, type, and optional certificate alias.
     */
    private static final class KeyStoreProperties {

        /**
         * The keystore/truststore file path.
         */
        private final Path filename;

        /**
         * The SHA-256 content hash of the keystore/truststore file.
         */
        private final String contentHash;

        /**
         * The keystore/truststore password.
         */
        private final char[] password;

        /**
         * The keystore/truststore type (e.g., JKS, PKCS12).
         */
        private final String type;

        /**
         * The certificate alias (only for keystore, may be {@code null} for truststore).
         */
        private final String certificateAlias;

        /**
         * Constructs keystore/truststore properties.
         *
         * @param filename the keystore/truststore file path
         * @param contentHash the SHA-256 content hash of the file
         * @param password the password for the keystore/truststore
         * @param type the keystore/truststore type
         * @param certificateAlias the certificate alias for keystore, may be {@code null} for
         *     truststore
         */
        private KeyStoreProperties(
                String filename, String contentHash, char[] password, String type, String certificateAlias) {

            this.filename = Paths.get(filename);
            this.contentHash = contentHash;
            this.password = password;
            this.type = type;
            this.certificateAlias = certificateAlias;
        }

        /**
         * Returns the keystore/truststore file path.
         *
         * @return the file path
         */
        public Path getFilename() {
            return filename;
        }

        /**
         * Returns the SHA-256 content hash of the file.
         *
         * @return the content hash
         */
        public String getContentHash() {
            return contentHash;
        }

        /**
         * Returns the keystore/truststore password.
         *
         * @return the password
         */
        public char[] getPassword() {
            return password;
        }

        /**
         * Returns the keystore/truststore type.
         *
         * @return the type
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the certificate alias.
         *
         * @return an Optional containing the certificate alias, or empty if not set
         */
        public Optional<String> getCertificateAlias() {
            return Optional.ofNullable(certificateAlias);
        }
    }
}
