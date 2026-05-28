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
import io.prometheus.jmx.common.util.BlockingRejectedExecutionHandler;
import io.prometheus.jmx.common.util.MapAccessor;
import io.prometheus.jmx.common.util.YamlSupport;
import io.prometheus.jmx.common.util.functions.IntegerInRange;
import io.prometheus.jmx.common.util.functions.StringIsNotBlank;
import io.prometheus.jmx.common.util.functions.ToBoolean;
import io.prometheus.jmx.common.util.functions.ToInteger;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
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
     * Logger for configuration warnings.
     */
    private static final Logger LOGGER = Logger.getLogger(HTTPServerFactory.class.getName());

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
     * Minimum recommended effective key length in bits for PBKDF2 key derivation.
     */
    private static final int MINIMUM_RECOMMENDED_PBKDF2_KEY_LENGTH_BITS = PBKDF2_KEY_LENGTH_BITS;

    /**
     * Scheduled executor for periodic SSL certificate reloading.
     *
     * <p>Checks for updated certificates every hour.
     */
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    /**
     * Root path segment used when building configuration keys.
     */
    private static final String ROOT = "/";

    /**
     * Base configuration key for HTTP server settings.
     */
    private static final String HTTP_SERVER = ROOT + "httpServer";

    /**
     * Base configuration key for metrics endpoint settings.
     */
    private static final String HTTP_SERVER_METRICS = HTTP_SERVER + "/metrics";

    /**
     * Configuration key for the metrics endpoint path.
     */
    private static final String HTTP_SERVER_METRICS_PATH = HTTP_SERVER_METRICS + "/path";

    /**
     * Base configuration key for thread pool settings.
     */
    private static final String HTTP_SERVER_THREADS = HTTP_SERVER + "/threads";

    /**
     * Configuration key for minimum worker thread count.
     */
    private static final String HTTP_SERVER_THREADS_MINIMUM = HTTP_SERVER_THREADS + "/minimum";

    /**
     * Configuration key for maximum worker thread count.
     */
    private static final String HTTP_SERVER_THREADS_MAXIMUM = HTTP_SERVER_THREADS + "/maximum";

    /**
     * Configuration key for worker thread keep-alive time.
     */
    private static final String HTTP_SERVER_THREADS_KEEP_ALIVE_TIME = HTTP_SERVER_THREADS + "/keepAliveTime";

    /**
     * Base configuration key for authentication settings.
     */
    private static final String HTTP_SERVER_AUTHENTICATION = HTTP_SERVER + "/authentication";

    /**
     * Base configuration key for custom authentication plugin settings.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_PLUGIN = HTTP_SERVER_AUTHENTICATION + "/plugin";

    /**
     * Configuration key for custom authentication plugin class name.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_PLUGIN_CLASS = HTTP_SERVER_AUTHENTICATION_PLUGIN + "/class";

    /**
     * Configuration key for Subject attribute name exposed by the authentication plugin.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_PLUGIN_SUBJECT_ATTRIBUTE_NAME =
            HTTP_SERVER_AUTHENTICATION_PLUGIN + "/subjectAttributeName";

    /**
     * Base configuration key for basic authentication settings.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC = HTTP_SERVER_AUTHENTICATION + "/basic";

    /**
     * Configuration key for basic authentication username.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_USERNAME =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/username";

    /**
     * Configuration key for basic authentication password algorithm.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_ALGORITHM =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/algorithm";

    /**
     * Configuration key for basic authentication plaintext password.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_PASSWORD =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/password";

    /**
     * Configuration key for basic authentication password hash.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_PASSWORD_HASH =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/passwordHash";

    /**
     * Configuration key for basic authentication salt value.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_SALT = HTTP_SERVER_AUTHENTICATION_BASIC + "/salt";

    /**
     * Configuration key for basic authentication PBKDF2 iteration count.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_ITERATIONS =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/iterations";

    /**
     * Configuration key for basic authentication PBKDF2 key length.
     */
    private static final String HTTP_SERVER_AUTHENTICATION_BASIC_KEY_LENGTH =
            HTTP_SERVER_AUTHENTICATION_BASIC + "/keyLength";

    /**
     * Base configuration key for SSL settings.
     */
    private static final String HTTP_SERVER_SSL = HTTP_SERVER + "/ssl";

    /**
     * Base configuration key for SSL keystore settings.
     */
    private static final String HTTP_SERVER_SSL_KEY_STORE = HTTP_SERVER_SSL + "/keyStore";

    /**
     * Configuration key for SSL keystore filename.
     */
    private static final String HTTP_SERVER_SSL_KEY_STORE_FILENAME = HTTP_SERVER_SSL_KEY_STORE + "/filename";

    /**
     * Configuration key for SSL keystore type.
     */
    private static final String HTTP_SERVER_SSL_KEY_STORE_TYPE = HTTP_SERVER_SSL_KEY_STORE + "/type";

    /**
     * Configuration key for SSL keystore password.
     */
    private static final String HTTP_SERVER_SSL_KEY_STORE_PASSWORD = HTTP_SERVER_SSL_KEY_STORE + "/password";

    /**
     * Base configuration key for SSL certificate settings.
     */
    private static final String HTTP_SERVER_SSL_CERTIFICATE = HTTP_SERVER_SSL + "/certificate";

    /**
     * Configuration key for SSL certificate alias.
     */
    private static final String HTTP_SERVER_SSL_CERTIFICATE_ALIAS = HTTP_SERVER_SSL_CERTIFICATE + "/alias";

    /**
     * Base configuration key for SSL truststore settings.
     */
    private static final String HTTP_SERVER_SSL_TRUST_STORE = HTTP_SERVER_SSL + "/trustStore";

    /**
     * Configuration key for SSL truststore filename.
     */
    private static final String HTTP_SERVER_SSL_TRUST_STORE_FILENAME = HTTP_SERVER_SSL_TRUST_STORE + "/filename";

    /**
     * Configuration key for SSL truststore type.
     */
    private static final String HTTP_SERVER_SSL_TRUST_STORE_TYPE = HTTP_SERVER_SSL_TRUST_STORE + "/type";

    /**
     * Configuration key for SSL truststore password.
     */
    private static final String HTTP_SERVER_SSL_TRUST_STORE_PASSWORD = HTTP_SERVER_SSL_TRUST_STORE + "/password";

    /**
     * Configuration key for enabling mutual TLS.
     */
    private static final String HTTP_SERVER_SSL_MUTUAL_TLS = HTTP_SERVER_SSL + "/mutualTLS";

    /**
     * Prefix used for extracting SSL-related properties.
     */
    private static final String HTTP_SERVER_SSL_PROPERTY_PREFIX = HTTP_SERVER_SSL + "/";

    /**
     * Path suffix for plugin/authentication class name.
     */
    private static final String PATH_CLASS = "/class";

    /**
     * Path suffix for Subject attribute name.
     */
    private static final String PATH_SUBJECT_ATTRIBUTE_NAME = "/subjectAttributeName";

    /**
     * Path suffix for username.
     */
    private static final String PATH_USERNAME = "/username";

    /**
     * Path suffix for algorithm.
     */
    private static final String PATH_ALGORITHM = "/algorithm";

    /**
     * Path suffix for password.
     */
    private static final String PATH_PASSWORD = "/password";

    /**
     * Path suffix for password hash.
     */
    private static final String PATH_PASSWORD_HASH = "/passwordHash";

    /**
     * Path suffix for salt.
     */
    private static final String PATH_SALT = "/salt";

    /**
     * Path suffix for PBKDF2 iterations.
     */
    private static final String PATH_ITERATIONS = "/iterations";

    /**
     * Path suffix for PBKDF2 key length.
     */
    private static final String PATH_KEY_LENGTH = "/keyLength";

    /**
     * Path suffix for minimum value settings.
     */
    private static final String PATH_MINIMUM = "/minimum";

    /**
     * Path suffix for maximum value settings.
     */
    private static final String PATH_MAXIMUM = "/maximum";

    /**
     * Path suffix for keep-alive-time settings.
     */
    private static final String PATH_KEEP_ALIVE_TIME = "/keepAliveTime";

    /**
     * Path suffix for endpoint path settings.
     */
    private static final String PATH_PATH = "/path";

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
        // Intentionally empty
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
        boolean sslEnabled = rootMapAccessor.containsPath(HTTP_SERVER_SSL);

        HTTPServer.Builder httpServerBuilder =
                HTTPServer.builder().inetAddress(inetAddress).port(port).registry(prometheusRegistry);

        configureMetricsPath(rootMapAccessor, httpServerBuilder);
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
        boolean sslEnabled = rootMapAccessor.containsPath(HTTP_SERVER_SSL);

        HTTPServer.Builder httpServerBuilder = HTTPServer.builder().registry(prometheusRegistry);

        configureMetricsPath(rootMapAccessor, httpServerBuilder);
        configureThreads(rootMapAccessor, httpServerBuilder);
        configureAuthentication(authenticationConfiguration, httpServerBuilder);
        configureSSL(rootMapAccessor, httpServerBuilder);

        HTTPServer httpServer = httpServerBuilder.buildAndStart();
        configureSecurityHeaders(httpServer, prometheusRegistry, authenticationConfiguration, sslEnabled);
        return httpServer;
    }

    /**
     * Configures the HTTP server metrics path based on YAML configuration.
     *
     * <p>Metrics path configuration is read from the {@code /httpServer/metrics} path. If not
     * specified, default values are used: {@code path =} {@value #METRICS_PATH}
     *
     * @param rootMapAccessor the root configuration map accessor, must not be {@code null}
     * @param httpServerBuilder the HTTP server builder to configure, must not be {@code null}
     * @throws ConfigurationException if the metrics path configuration is invalid
     */
    private static void configureMetricsPath(MapAccessor rootMapAccessor, HTTPServer.Builder httpServerBuilder) {
        String metricsPath = METRICS_PATH;

        if (rootMapAccessor.containsPath(HTTP_SERVER_METRICS, Map.class)) {
            MapAccessor httpServerMetricsMapAccessor = rootMapAccessor
                    .getPath(HTTP_SERVER_METRICS, Map.class)
                    .map(value -> MapAccessor.of((Map<Object, Object>) value))
                    .orElseThrow(ConfigurationException.supplier(HTTP_SERVER_METRICS + " must be a map"));

            metricsPath = httpServerMetricsMapAccessor
                    .getPath(PATH_PATH)
                    .map(ToString.of(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/metrics/path" + " must be a string")))
                    .map(StringIsNotBlank.of(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/metrics/path" + " must not be blank")))
                    .orElseThrow(ConfigurationException.supplier(HTTP_SERVER_METRICS_PATH + " is a required string"));
        }

        httpServerBuilder.metricsHandlerPath(metricsPath);
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

        if (rootMapAccessor.containsPath(HTTP_SERVER_THREADS, Map.class)) {
            MapAccessor httpServerThreadsMapAccessor = rootMapAccessor
                    .getPath(HTTP_SERVER_THREADS, Map.class)
                    .map(value -> MapAccessor.of((Map<Object, Object>) value))
                    .orElseThrow(ConfigurationException.supplier(HTTP_SERVER_THREADS + " must be a map"));

            minimum = httpServerThreadsMapAccessor
                    .getPath(PATH_MINIMUM)
                    .map(ToInteger.of(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/threads/minimum must be an" + " integer")))
                    .map(IntegerInRange.of(
                            1,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/minimum must be 1"
                                    + " or greater")))
                    .orElseThrow(
                            ConfigurationException.supplier(HTTP_SERVER_THREADS_MINIMUM + " is a required integer"));

            maximum = httpServerThreadsMapAccessor
                    .getPath(PATH_MAXIMUM)
                    .map(ToInteger.of(ConfigurationException.supplier(
                            "Invalid configuration for" + " /httpServer/threads/maximum must be an" + " integer")))
                    .map(IntegerInRange.of(
                            minimum,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/maxPoolSize must be"
                                    + " between greater than 0")))
                    .orElseThrow(
                            ConfigurationException.supplier(HTTP_SERVER_THREADS_MAXIMUM + " is a required integer"));

            keepAliveTime = httpServerThreadsMapAccessor
                    .getPath(PATH_KEEP_ALIVE_TIME)
                    .map(ToInteger.of(ConfigurationException.supplier("Invalid configuration for"
                            + " /httpServer/threads/keepAliveTime must"
                            + " be an integer")))
                    .map(IntegerInRange.of(
                            1,
                            Integer.MAX_VALUE,
                            ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/threads/keepAliveTime must"
                                    + " be greater than 0")))
                    .orElseThrow(ConfigurationException.supplier(
                            HTTP_SERVER_THREADS_KEEP_ALIVE_TIME + " is a required integer"));
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
        Authenticator authenticator = authenticationConfiguration.getAuthenticator();
        if (authenticator != null) {
            httpServerBuilder.authenticator(authenticator);
        }

        String subjectAttributeName = authenticationConfiguration.getSubjectAttributeName();
        if (subjectAttributeName != null) {
            httpServerBuilder.authenticatedSubjectAttributeName(subjectAttributeName);
        }
    }

    /**
     * Resolves authentication configuration from the YAML root configuration.
     *
     * <p>Determines whether to use a custom authenticator plugin or basic authentication. For basic
     * authentication, selects among plaintext, SHA, or PBKDF2 algorithms based on the configured
     * {@code algorithm} value.
     *
     * @param rootMapAccessor the root configuration map accessor, must not be {@code null}
     * @return the resolved authentication configuration, with a {@code null} authenticator when
     *     authentication is not configured
     * @throws ConfigurationException if authentication configuration is invalid or incomplete
     */
    private static AuthenticationConfiguration getAuthenticationConfiguration(MapAccessor rootMapAccessor) {
        Authenticator authenticator = null;
        String subjectAttributeName = null;

        if (rootMapAccessor.containsPath(HTTP_SERVER_AUTHENTICATION)) {
            Optional<Object> authenticatorClassAttribute = rootMapAccessor.getPath(HTTP_SERVER_AUTHENTICATION_PLUGIN);

            if (authenticatorClassAttribute.isPresent()) {
                MapAccessor httpServerAuthenticationCustomAuthenticatorMapAccessor = authenticatorClassAttribute
                        .filter(Map.class::isInstance)
                        .map(value -> MapAccessor.of((Map<Object, Object>) value))
                        .orElseThrow(
                                ConfigurationException.supplier(HTTP_SERVER_AUTHENTICATION_PLUGIN + " must be a map"));

                String authenticatorClass = httpServerAuthenticationCustomAuthenticatorMapAccessor
                        .getPath(PATH_CLASS)
                        .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/plugin/class"
                                + " must be a string")))
                        .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/plugin/class"
                                + " must not be blank")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/plugin/class must be a" + " string"));

                Optional<Object> subjectAttribute =
                        httpServerAuthenticationCustomAuthenticatorMapAccessor.getPath(PATH_SUBJECT_ATTRIBUTE_NAME);

                if (subjectAttribute.isPresent()) {
                    subjectAttributeName = subjectAttribute
                            .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/plugin/class/subjectAttributeName"
                                    + " must be a string")))
                            .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/plugin/subjectAttributeName"
                                    + " must not be blank")))
                            .get();
                }

                authenticator = loadAuthenticator(authenticatorClass);
            } else {
                MapAccessor httpServerAuthenticationBasicMapAccessor = rootMapAccessor
                        .getPath(HTTP_SERVER_AUTHENTICATION_BASIC, Map.class)
                        .map(value -> MapAccessor.of((Map<Object, Object>) value))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/basic configuration" + " must be a map"));

                String username = httpServerAuthenticationBasicMapAccessor
                        .getPath(PATH_USERNAME)
                        .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/username"
                                + " must be a string")))
                        .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/username"
                                + " must not be blank")))
                        .orElseThrow(ConfigurationException.supplier(
                                "/httpServer/authentication/basic/username is a" + " required string"));

                username = VariableResolver.resolveVariable(username);

                String algorithm = httpServerAuthenticationBasicMapAccessor
                        .getPath(PATH_ALGORITHM)
                        .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/algorithm"
                                + " must be a string")))
                        .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/algorithm"
                                + " must not be blank")))
                        .orElse(PLAINTEXT);

                if (PLAINTEXT.equalsIgnoreCase(algorithm)) {
                    String password = httpServerAuthenticationBasicMapAccessor
                            .getPath(PATH_PASSWORD)
                            .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/password"
                                    + " must be a string")))
                            .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/password"
                                    + " must not be blank")))
                            .orElseThrow(ConfigurationException.supplier(
                                    HTTP_SERVER_AUTHENTICATION_BASIC_PASSWORD + " is a required string"));

                    password = VariableResolver.resolveVariable(password);
                    authenticator = new PlaintextAuthenticator("/", username, password);
                } else if (SHA_ALGORITHMS.contains(algorithm) || PBKDF2_ALGORITHMS.contains(algorithm)) {
                    String hash = httpServerAuthenticationBasicMapAccessor
                            .getPath(PATH_PASSWORD_HASH)
                            .map(ToString.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/passwordHash"
                                    + " must be a string")))
                            .map(StringIsNotBlank.of(ConfigurationException.supplier("Invalid configuration for"
                                    + " /httpServer/authentication/basic/passwordHash"
                                    + " must not be blank")))
                            .orElseThrow(ConfigurationException.supplier(
                                    HTTP_SERVER_AUTHENTICATION_BASIC_PASSWORD_HASH + " is a required string"));

                    boolean isShaAlgorithm = SHA_ALGORITHMS.contains(algorithm);
                    if (isShaAlgorithm) {
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

    /**
     * Replaces all HTTP server contexts with wrapped handlers and authenticators that inject
     * security response headers.
     *
     * <p>Adds {@code X-Content-Type-Options}, {@code X-Frame-Options}, and optionally
     * {@code Strict-Transport-Security} headers to all responses.
     *
     * @param httpServer the HTTP server whose contexts should be replaced, must not be {@code null}
     * @param prometheusRegistry the Prometheus registry for metric collection, must not be
     *     {@code null}
     * @param authenticationConfiguration the authentication configuration containing the
     *     authenticator and subject attribute name, must not be {@code null}
     * @param sslEnabled whether SSL is enabled, used to determine if HSTS headers should be added
     */
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

    /**
     * Replaces an HTTP context by removing the existing context at the given path and creating a
     * new one with the specified handler and optional authenticator.
     *
     * @param httpServer the HTTP server to modify, must not be {@code null}
     * @param path the context path to replace, must not be {@code null}
     * @param handler the new HTTP handler, must not be {@code null}
     * @param authenticator the authenticator to set, or {@code null} if no authentication is needed
     */
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

    /**
     * Reflectively accesses the underlying {@link com.sun.net.httpserver.HttpServer} from the
     * Prometheus {@link HTTPServer} wrapper.
     *
     * @param httpServer the Prometheus HTTP server instance, must not be {@code null}
     * @return the delegate HTTP server
     * @throws IllegalStateException if the underlying server field cannot be accessed
     */
    private static com.sun.net.httpserver.HttpServer getDelegateHttpServer(HTTPServer httpServer) {
        try {
            Field field = HTTPServer.class.getDeclaredField("server");
            field.setAccessible(true);
            return (com.sun.net.httpserver.HttpServer) field.get(httpServer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to access underlying HTTP server", e);
        }
    }

    /**
     * Wraps an HTTP handler with security headers and optional Subject.doAs delegation.
     *
     * @param handler the delegate handler, must not be {@code null}
     * @param sslEnabled whether SSL is enabled
     * @param subjectAttributeName the request attribute name for the authenticated Subject, or
     *     {@code null} if Subject delegation is not needed
     * @return the wrapping handler
     */
    private static HttpHandler wrapHandler(HttpHandler handler, boolean sslEnabled, String subjectAttributeName) {
        return new SecurityHeadersHandler(handler, sslEnabled, subjectAttributeName);
    }

    /**
     * Wraps an authenticator with security header injection.
     *
     * @param authenticator the delegate authenticator, or {@code null} if no authentication is
     *     configured
     * @param sslEnabled whether SSL is enabled
     * @return a wrapping authenticator, or {@code null} if the input authenticator is {@code null}
     */
    private static Authenticator wrapAuthenticator(Authenticator authenticator, boolean sslEnabled) {
        if (authenticator == null) {
            return null;
        }

        return new SecurityHeadersAuthenticator(authenticator, sslEnabled);
    }

    /**
     * Adds security response headers to the given headers map.
     *
     * @param headers the response headers to modify, must not be {@code null}
     * @param sslEnabled whether SSL is enabled, controls addition of HSTS header
     */
    private static void addSecurityHeaders(Headers headers, boolean sslEnabled) {
        headers.set(X_CONTENT_TYPE_OPTIONS, NOSNIFF);
        headers.set(X_FRAME_OPTIONS, DENY);
        if (sslEnabled) {
            headers.set(STRICT_TRANSPORT_SECURITY, STRICT_TRANSPORT_SECURITY_VALUE);
        }
    }

    /**
     * Drains and closes the request input stream to ensure the HTTP exchange can be reused.
     *
     * @param httpExchange the HTTP exchange whose request body should be drained, must not be
     *     {@code null}
     * @throws IOException if reading or closing the input stream fails
     */
    private static void drainInputAndClose(HttpExchange httpExchange) throws IOException {
        InputStream inputStream = httpExchange.getRequestBody();
        byte[] bytes = new byte[4096];
        while (inputStream.read(bytes) != -1) {
            // Intentionally empty
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
                .getPath(PATH_SALT)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " not be blank")))
                .orElseThrow(ConfigurationException.supplier(
                        "/httpServer/authentication/basic/salt is a required" + " string"));

        try {
            return new MessageDigestAuthenticator(realm, username, password, algorithm, salt);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(format(
                    "Invalid /httpServer/authentication/basic/algorithm, unsupported" + " algorithm [%s]", algorithm));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage());
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
                .getPath(PATH_SALT)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/authentication/basic/salt must" + " be not blank")))
                .orElseThrow(ConfigurationException.supplier(
                        "/httpServer/authentication/basic/salt is a required" + " string"));

        int iterations = httpServerAuthenticationBasicMapAccessor
                .getPath(PATH_ITERATIONS)
                .map(ToInteger.of(ConfigurationException.supplier("Invalid configuration for"
                        + " /httpServer/authentication/basic/iterations"
                        + " must be an integer")))
                .map(IntegerInRange.of(
                        1,
                        Integer.MAX_VALUE,
                        ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/iterations"
                                + " must be between greater than 0")))
                .orElse(PBKDF2_ALGORITHM_ITERATIONS.get(algorithm));

        int keyLength = httpServerAuthenticationBasicMapAccessor
                .getPath(PATH_KEY_LENGTH)
                .map(ToInteger.of(ConfigurationException.supplier("Invalid configuration for"
                        + " /httpServer/authentication/basic/keyLength"
                        + " must be an integer")))
                .map(IntegerInRange.of(
                        1,
                        Integer.MAX_VALUE,
                        ConfigurationException.supplier("Invalid configuration for"
                                + " /httpServer/authentication/basic/keyLength"
                                + " must be greater than 0")))
                .orElse(PBKDF2_KEY_LENGTH_BITS);

        warnIfWeakPBKDF2Configuration(algorithm, password, iterations, keyLength);

        try {
            return new PBKDF2Authenticator(realm, username, password, algorithm, salt, iterations, keyLength);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException(format(
                    "Invalid /httpServer/authentication/basic/algorithm, unsupported" + " algorithm [%s]", algorithm));
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e.getMessage());
        }
    }

    /**
     * Logs warnings for PBKDF2 configurations below the recommended default settings.
     *
     * @param algorithm the PBKDF2 algorithm
     * @param passwordHash the configured password hash
     * @param iterations the configured iteration count
     * @param keyLength the configured key length
     */
    private static void warnIfWeakPBKDF2Configuration(
            String algorithm, String passwordHash, int iterations, int keyLength) {
        int recommendedIterations = PBKDF2_ALGORITHM_ITERATIONS.get(algorithm);
        if (iterations < recommendedIterations) {
            LOGGER.log(
                    Level.WARNING,
                    "Configured /httpServer/authentication/basic/iterations [{0}] for algorithm [{1}] "
                            + "is lower than the recommended value [{2}]",
                    new Object[] {iterations, algorithm, recommendedIterations});
        }

        int effectiveKeyLengthBits = getEffectivePBKDF2KeyLengthBits(passwordHash, keyLength);
        if (effectiveKeyLengthBits < MINIMUM_RECOMMENDED_PBKDF2_KEY_LENGTH_BITS) {
            LOGGER.log(
                    Level.WARNING,
                    "Configured /httpServer/authentication/basic/keyLength [{0}] for algorithm [{1}] "
                            + "results in an effective key length [{2}] bits, which is lower than "
                            + "the recommended value [{3}] bits",
                    new Object[] {
                        keyLength, algorithm, effectiveKeyLengthBits, MINIMUM_RECOMMENDED_PBKDF2_KEY_LENGTH_BITS
                    });
        }
    }

    /**
     * Returns the effective PBKDF2 key length in bits for warning purposes.
     *
     * <p>The authenticator accepts both documented bit semantics and legacy byte semantics. If the
     * configured value appears to use legacy byte semantics for the decoded hash length, the decoded
     * hash size determines the effective key length.
     *
     * @param passwordHash the configured password hash
     * @param keyLength the configured key length
     * @return the effective key length in bits
     */
    private static int getEffectivePBKDF2KeyLengthBits(String passwordHash, int keyLength) {
        String normalizedPasswordHash = passwordHash.replace(":", "");
        int passwordHashLengthBits = (normalizedPasswordHash.length() / 2) * Byte.SIZE;
        long legacyKeyLengthBits = (long) keyLength * Byte.SIZE;

        if (normalizedPasswordHash.length() % 2 == 0 && passwordHashLengthBits == legacyKeyLengthBits) {
            return passwordHashLengthBits;
        }

        return keyLength;
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
        if (rootMapAccessor.containsPath(HTTP_SERVER_SSL)) {
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
        char[] keyStorePassword = keyStoreProperties.getPassword();
        Optional<KeyStoreProperties> trustProps = getTrustStoreProperties(rootMapAccessor);

        SSLFactory.Builder sslFactoryBuilder = SSLFactory.builder()
                .withSwappableIdentityMaterial()
                .withIdentityMaterial(
                        keyStoreProperties.getFilename(),
                        keyStorePassword,
                        keyStorePassword,
                        keyStoreProperties.getType());

        if (trustProps.isPresent()) {
            KeyStoreProperties trustStoreProps = trustProps.get();
            trustStoreProperties = trustStoreProps;
            sslFactoryBuilder
                    .withSwappableTrustMaterial()
                    .withTrustMaterial(
                            trustStoreProps.getFilename(), trustStoreProps.getPassword(), trustStoreProps.getType());
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
        boolean hasCurrentTrustProps = currentTrustProps.isPresent();
        Optional<String> currentTrustStoreContentHash = Optional.empty();
        if (hasCurrentTrustProps) {
            KeyStoreProperties currentTrustStoreProps = currentTrustProps.get();
            currentTrustStoreContentHash = getContentHash(currentTrustStoreProps.getFilename());
            if (!currentTrustStoreContentHash.isPresent()) {
                return;
            }
        }

        boolean keyStoreChanged = !keyStoreProperties.getContentHash().equals(currentKeyStoreContentHash.get());
        boolean trustStoreChanged = hasCurrentTrustProps
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
            char[] keyPassword = keyProps.getPassword();
            updatedSslFactory.withIdentityMaterial(
                    keyProps.getFilename(), keyPassword, keyPassword, keyProps.getType());
            sslUpdated = true;
        }

        boolean hasTrustProps = trustProps.isPresent();
        if (trustStoreChanged && hasTrustProps) {
            KeyStoreProperties trustStoreProps = trustProps.get();
            updatedSslFactory.withTrustMaterial(
                    trustStoreProps.getFilename(), trustStoreProps.getPassword(), trustStoreProps.getType());
            sslUpdated = true;
        }

        if (sslUpdated) {
            try {
                SSLFactoryUtils.reload(sslFactory, updatedSslFactory.build());
                if (keyStoreChanged) {
                    keyStoreProperties = keyProps;
                }
                if (trustStoreChanged && hasTrustProps) {
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
                .getPath(HTTP_SERVER_SSL_KEY_STORE_FILENAME)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/filename" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/filename" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE));

        if (keyStoreFilename == null) {
            throw new ConfigurationException(
                    "SSL keyStore filename must be configured via /httpServer/ssl/keyStore/filename"
                            + " or system property javax.net.ssl.keyStore");
        }

        String contentHash = getContentHash(keyStoreFilename);

        String keyStoreType = rootMapAccessor
                .getPath(HTTP_SERVER_SSL_KEY_STORE_TYPE)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/type" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/type" + " must not be blank")))
                .orElse(DEFAULT_KEYSTORE_TYPE);

        String keyStorePassword = rootMapAccessor
                .getPath(HTTP_SERVER_SSL_KEY_STORE_PASSWORD)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/password" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/keyStore/password" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE_PASSWORD));

        // Resolve the password
        keyStorePassword = VariableResolver.resolveVariable(keyStorePassword);

        if (keyStorePassword == null) {
            throw new ConfigurationException(
                    "SSL keyStore password must be configured via /httpServer/ssl/keyStore/password"
                            + " or system property javax.net.ssl.keyStorePassword");
        }

        String certificateAlias = rootMapAccessor
                .getPath(HTTP_SERVER_SSL_CERTIFICATE_ALIAS)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/certificate/alias" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
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
                .getPath(HTTP_SERVER_SSL_TRUST_STORE_FILENAME)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/filename" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/filename" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE));

        if (trustStoreFilename == null) {
            throw new ConfigurationException(
                    "SSL trustStore filename must be configured via /httpServer/ssl/trustStore/filename"
                            + " or system property javax.net.ssl.trustStore");
        }

        String contentHash = getContentHash(trustStoreFilename);

        String trustStoreType = rootMapAccessor
                .getPath(HTTP_SERVER_SSL_TRUST_STORE_TYPE)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/type" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/type" + " must not be blank")))
                .orElse(DEFAULT_TRUST_STORE_TYPE);

        String trustStorePassword = rootMapAccessor
                .getPath(HTTP_SERVER_SSL_TRUST_STORE_PASSWORD)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/password" + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/trustStore/password" + " must not be blank")))
                .orElse(System.getProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD));

        // Resolve the password
        trustStorePassword = VariableResolver.resolveVariable(trustStorePassword);

        if (trustStorePassword == null) {
            throw new ConfigurationException(
                    "SSL trustStore password must be configured via /httpServer/ssl/trustStore/password"
                            + " or system property javax.net.ssl.trustStorePassword");
        }

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
                .getPath(HTTP_SERVER_SSL_MUTUAL_TLS)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/mutualTLS" + " must be a boolean")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/mutualTLS" + " must not be blank")))
                .map(ToBoolean.of(ConfigurationException.supplier(
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
                .getPath(HTTP_SERVER_SSL_PROPERTY_PREFIX + property)
                .map(ToString.of(ConfigurationException.supplier(
                        "Invalid configuration for" + " /httpServer/ssl/" + property + " must be a string")))
                .map(StringIsNotBlank.of(ConfigurationException.supplier(
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

        /**
         * Creates a new thread with a Prometheus-style name and the configured daemon flag.
         *
         * @param r the runnable to execute in the new thread
         * @return the newly created thread
         */
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

    /**
     * Immutable holder for an authenticator and optional subject attribute name.
     *
     * <p>Used to pass authentication configuration between resolution and application stages.
     */
    private static final class AuthenticationConfiguration {

        /**
         * The configured authenticator, or {@code null} when authentication is not enabled.
         */
        private final Authenticator authenticator;

        /**
         * The request attribute name used to store the authenticated Subject, or {@code null}
         * when Subject delegation is not configured.
         */
        private final String subjectAttributeName;

        /**
         * Constructs an authentication configuration.
         *
         * @param authenticator the authenticator, may be {@code null} when authentication is disabled
         * @param subjectAttributeName the subject attribute name, may be {@code null}
         */
        private AuthenticationConfiguration(Authenticator authenticator, String subjectAttributeName) {
            this.authenticator = authenticator;
            this.subjectAttributeName = subjectAttributeName;
        }

        /**
         * Returns the configured authenticator.
         *
         * @return the authenticator, or {@code null} when authentication is disabled
         */
        private Authenticator getAuthenticator() {
            return authenticator;
        }

        /**
         * Returns the subject attribute name.
         *
         * @return the subject attribute name, or {@code null} when Subject delegation is not
         *     configured
         */
        private String getSubjectAttributeName() {
            return subjectAttributeName;
        }
    }

    /**
     * Delegating authenticator that injects security response headers before performing
     * authentication.
     *
     * <p>Ensures security headers are present even on 401 Unauthorized responses.
     */
    private static final class SecurityHeadersAuthenticator extends Authenticator {

        /**
         * The delegate authenticator that performs the actual credential check.
         */
        private final Authenticator delegate;

        /**
         * Whether SSL is enabled, controls addition of HSTS header.
         */
        private final boolean sslEnabled;

        /**
         * Constructs a security headers authenticator.
         *
         * @param delegate the delegate authenticator, must not be {@code null}
         * @param sslEnabled whether SSL is enabled
         */
        private SecurityHeadersAuthenticator(Authenticator delegate, boolean sslEnabled) {
            this.delegate = delegate;
            this.sslEnabled = sslEnabled;
        }

        /**
         * Injects security headers and delegates authentication.
         *
         * @param exchange the HTTP exchange to authenticate
         * @return the authentication result from the delegate
         */
        @Override
        public Result authenticate(HttpExchange exchange) {
            addSecurityHeaders(exchange.getResponseHeaders(), sslEnabled);
            return delegate.authenticate(exchange);
        }
    }

    /**
     * Delegating HTTP handler that injects security response headers and optionally performs
     * Subject.doAs delegation for authenticated requests.
     *
     * <p>When a subject attribute name is configured and the authenticated Subject is available,
     * the delegate handler is invoked within a {@link javax.security.auth.Subject#doAs} call.
     * If the Subject is not available, the request is rejected with a 403 response.
     */
    private static final class SecurityHeadersHandler implements HttpHandler {

        /**
         * The delegate handler that processes the actual request.
         */
        private final HttpHandler delegate;

        /**
         * Whether SSL is enabled, controls addition of HSTS header.
         */
        private final boolean sslEnabled;

        /**
         * The request attribute name used to look up the authenticated Subject, or {@code null}
         * when Subject delegation is not configured.
         */
        private final String subjectAttributeName;

        /**
         * Constructs a security headers handler.
         *
         * @param delegate the delegate handler, must not be {@code null}
         * @param sslEnabled whether SSL is enabled
         * @param subjectAttributeName the request attribute name for Subject lookup, may be
         *     {@code null}
         */
        private SecurityHeadersHandler(HttpHandler delegate, boolean sslEnabled, String subjectAttributeName) {
            this.delegate = delegate;
            this.sslEnabled = sslEnabled;
            this.subjectAttributeName = subjectAttributeName;
        }

        /**
         * Injects security headers, then delegates to the wrapped handler with optional
         * Subject.doAs invocation.
         *
         * @param exchange the HTTP exchange to handle
         * @throws IOException if the delegate handler or Subject.doAs fails
         */
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
