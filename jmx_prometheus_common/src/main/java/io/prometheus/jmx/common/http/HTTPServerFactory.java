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
import com.sun.net.httpserver.HttpsConfigurator;
import io.prometheus.jmx.common.configuration.ConvertToInteger;
import io.prometheus.jmx.common.configuration.ConvertToMapAccessor;
import io.prometheus.jmx.common.configuration.ConvertToString;
import io.prometheus.jmx.common.configuration.ValidateIntegerInRange;
import io.prometheus.jmx.common.configuration.ValidateStringIsNotBlank;
import io.prometheus.jmx.common.http.authenticator.MessageDigestAuthenticator;
import io.prometheus.jmx.common.http.authenticator.PBKDF2Authenticator;
import io.prometheus.jmx.common.http.authenticator.PlaintextAuthenticator;
import io.prometheus.jmx.common.http.ssl.SSLContextFactory;
import io.prometheus.jmx.common.yaml.YamlMapAccessor;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.yaml.snakeyaml.Yaml;

/**
 * Class to create the HTTPServer used by both the Java agent exporter and the standalone exporter
 */
public class HTTPServerFactory {

    private static final int DEFAULT_MINIMUM_THREADS = 1;
    private static final int DEFAULT_MAXIMUM_THREADS = 10;
    private static final int DEFAULT_KEEP_ALIVE_TIME_SECONDS = 120;

    private static final String REALM = "/";
    private static final String PLAINTEXT = "plaintext";
    private static final Set<String> SHA_ALGORITHMS;
    private static final Set<String> PBKDF2_ALGORITHMS;
    private static final Map<String, Integer> PBKDF2_ALGORITHM_ITERATIONS;
    private static final String JAVAX_NET_SSL_KEY_STORE = "javax.net.ssl.keyStore";
    private static final String JAVAX_NET_SSL_KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";

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

    /** Constructor */
    public HTTPServerFactory() {
        // DO NOTHING
    }

    /**
     * Method to create an HTTPServer using the supplied arguments
     *
     * @param inetAddress inetAddress
     * @param port port
     * @param prometheusRegistry prometheusRegistry
     * @param exporterYamlFile exporterYamlFile
     * @return an HTTPServer
     * @throws IOException IOException
     */
    public HTTPServer createHTTPServer(
            InetAddress inetAddress,
            int port,
            PrometheusRegistry prometheusRegistry,
            File exporterYamlFile)
            throws IOException {

        HTTPServer.Builder httpServerBuilder =
                HTTPServer.builder()
                        .inetAddress(inetAddress)
                        .port(port)
                        .registry(prometheusRegistry);

        createMapAccessor(exporterYamlFile);
        configureThreads(httpServerBuilder);
        configureAuthentication(httpServerBuilder);
        configureSSL(httpServerBuilder);

        return httpServerBuilder.buildAndStart();
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
                    String.format("Exception loading exporter YAML file [%s]", exporterYamlFile),
                    t);
        }
    }

    /**
     * Method to configure the HTTPServer thread pool
     *
     * @param httpServerBuilder httpServerBuilder
     */
    private void configureThreads(HTTPServer.Builder httpServerBuilder) {
        int minimum = DEFAULT_MINIMUM_THREADS;
        int maximum = DEFAULT_MAXIMUM_THREADS;
        int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME_SECONDS;

        if (rootYamlMapAccessor.containsPath("/httpServer/threads")) {
            YamlMapAccessor httpServerThreadsMapAccessor =
                    rootYamlMapAccessor
                            .get("/httpServer/threads")
                            .map(
                                    new ConvertToMapAccessor(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /httpServer/threads")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads configuration values are"
                                                    + " required"));

            minimum =
                    httpServerThreadsMapAccessor
                            .get("/minimum")
                            .map(
                                    new ConvertToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/minimum must be an"
                                                        + " integer")))
                            .map(
                                    new ValidateIntegerInRange(
                                            0,
                                            Integer.MAX_VALUE,
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/minimum must be 0"
                                                        + " or greater")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/threads/minimum is a required integer"));

            maximum =
                    httpServerThreadsMapAccessor
                            .get("/maximum")
                            .map(
                                    new ConvertToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/maximum must be an"
                                                        + " integer")))
                            .map(
                                    new ValidateIntegerInRange(
                                            1,
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
                                    new ConvertToInteger(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/threads/keepAliveTime must"
                                                        + " be an integer")))
                            .map(
                                    new ValidateIntegerInRange(
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

            if (maximum < minimum) {
                throw new ConfigurationException(
                        "/httpServer/threads/maximum must be greater than or equal to"
                                + " /httpServer/threads/minimum");
            }
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
     * @param httpServerBuilder httpServerBuilder
     */
    private void configureAuthentication(HTTPServer.Builder httpServerBuilder) {
        if (rootYamlMapAccessor.containsPath("/httpServer/authentication")) {
            YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor =
                    rootYamlMapAccessor
                            .get("/httpServer/authentication/basic")
                            .map(
                                    new ConvertToMapAccessor(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                            + " /httpServer/authentication/basic")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/authentication/basic configuration values"
                                                    + " are required"));

            String username =
                    httpServerAuthenticationBasicYamlMapAccessor
                            .get("/username")
                            .map(
                                    new ConvertToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/authentication/basic/username"
                                                        + " must be a string")))
                            .map(
                                    new ValidateStringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/authentication/basic/username"
                                                        + " must not be blank")))
                            .orElseThrow(
                                    ConfigurationException.supplier(
                                            "/httpServer/authentication/basic/username is a"
                                                    + " required string"));

            String algorithm =
                    httpServerAuthenticationBasicYamlMapAccessor
                            .get("/algorithm")
                            .map(
                                    new ConvertToString(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/authentication/basic/algorithm"
                                                        + " must be a string")))
                            .map(
                                    new ValidateStringIsNotBlank(
                                            ConfigurationException.supplier(
                                                    "Invalid configuration for"
                                                        + " /httpServer/authentication/basic/algorithm"
                                                        + " must not be blank")))
                            .orElse(PLAINTEXT);

            Authenticator authenticator;

            if (PLAINTEXT.equalsIgnoreCase(algorithm)) {
                String password =
                        httpServerAuthenticationBasicYamlMapAccessor
                                .get("/password")
                                .map(
                                        new ConvertToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/password"
                                                            + " must be a string")))
                                .map(
                                        new ValidateStringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/password"
                                                            + " must not be blank")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/basic/password is a"
                                                        + " required string"));

                authenticator = new PlaintextAuthenticator("/", username, password);
            } else if (SHA_ALGORITHMS.contains(algorithm)
                    || PBKDF2_ALGORITHMS.contains(algorithm)) {
                String hash =
                        httpServerAuthenticationBasicYamlMapAccessor
                                .get("/passwordHash")
                                .map(
                                        new ConvertToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/passwordHash"
                                                            + " must be a string")))
                                .map(
                                        new ValidateStringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/authentication/basic/passwordHash"
                                                            + " must not be blank")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/authentication/basic/passwordHash is a"
                                                        + " required string"));

                if (SHA_ALGORITHMS.contains(algorithm)) {
                    authenticator =
                            createMessageDigestAuthenticator(
                                    httpServerAuthenticationBasicYamlMapAccessor,
                                    REALM,
                                    username,
                                    hash,
                                    algorithm);
                } else {
                    authenticator =
                            createPBKDF2Authenticator(
                                    httpServerAuthenticationBasicYamlMapAccessor,
                                    REALM,
                                    username,
                                    hash,
                                    algorithm);
                }
            } else {
                throw new ConfigurationException(
                        String.format(
                                "Unsupported /httpServer/authentication/basic/algorithm [%s]",
                                algorithm));
            }

            httpServerBuilder.authenticator(authenticator);
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
            YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/salt")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
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
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported"
                                    + " algorithm [%s]",
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
            YamlMapAccessor httpServerAuthenticationBasicYamlMapAccessor,
            String realm,
            String username,
            String password,
            String algorithm) {
        String salt =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/salt")
                        .map(
                                new ConvertToString(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be a string")))
                        .map(
                                new ValidateStringIsNotBlank(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/salt must"
                                                    + " be not blank")))
                        .orElseThrow(
                                ConfigurationException.supplier(
                                        "/httpServer/authentication/basic/salt is a required"
                                                + " string"));

        int iterations =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/iterations")
                        .map(
                                new ConvertToInteger(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/iterations"
                                                    + " must be an integer")))
                        .map(
                                new ValidateIntegerInRange(
                                        1,
                                        Integer.MAX_VALUE,
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/iterations"
                                                    + " must be between greater than 0")))
                        .orElse(PBKDF2_ALGORITHM_ITERATIONS.get(algorithm));

        int keyLength =
                httpServerAuthenticationBasicYamlMapAccessor
                        .get("/keyLength")
                        .map(
                                new ConvertToInteger(
                                        ConfigurationException.supplier(
                                                "Invalid configuration for"
                                                    + " /httpServer/authentication/basic/keyLength"
                                                    + " must be an integer")))
                        .map(
                                new ValidateIntegerInRange(
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
                    String.format(
                            "Invalid /httpServer/authentication/basic/algorithm, unsupported"
                                    + " algorithm [%s]",
                            algorithm));
        }
    }

    /**
     * Method to configure SSL
     *
     * @param httpServerBuilder httpServerBuilder
     */
    public void configureSSL(HTTPServer.Builder httpServerBuilder) {
        if (rootYamlMapAccessor.containsPath("/httpServer/ssl")) {
            try {
                String keyStoreFilename =
                        rootYamlMapAccessor
                                .get("/httpServer/ssl/keyStore/filename")
                                .map(
                                        new ConvertToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/keyStore/filename"
                                                            + " must be a string")))
                                .map(
                                        new ValidateStringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/keyStore/filename"
                                                            + " must not be blank")))
                                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE));

                String keyStorePassword =
                        rootYamlMapAccessor
                                .get("/httpServer/ssl/keyStore/password")
                                .map(
                                        new ConvertToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/keyStore/password"
                                                            + " must be a string")))
                                .map(
                                        new ValidateStringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/keyStore/password"
                                                            + " must not be blank")))
                                .orElse(System.getProperty(JAVAX_NET_SSL_KEY_STORE_PASSWORD));

                String certificateAlias =
                        rootYamlMapAccessor
                                .get("/httpServer/ssl/certificate/alias")
                                .map(
                                        new ConvertToString(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/certificate/alias"
                                                            + " must be a string")))
                                .map(
                                        new ValidateStringIsNotBlank(
                                                ConfigurationException.supplier(
                                                        "Invalid configuration for"
                                                            + " /httpServer/ssl/certificate/alias"
                                                            + " must not be blank")))
                                .orElseThrow(
                                        ConfigurationException.supplier(
                                                "/httpServer/ssl/certificate/alias is a required"
                                                        + " string"));

                httpServerBuilder.httpsConfigurator(
                        new HttpsConfigurator(
                                SSLContextFactory.createSSLContext(
                                        keyStoreFilename, keyStorePassword, certificateAlias)));
            } catch (GeneralSecurityException | IOException e) {
                String message = e.getMessage();
                if (message != null && !message.trim().isEmpty()) {
                    message = ", " + message.trim();
                } else {
                    message = "";
                }

                throw new ConfigurationException(
                        String.format("Exception loading SSL configuration%s", message), e);
            }
        }
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
            t.setName(
                    String.format(
                            "prometheus-http-%d-%d", poolNumber, threadNumber.getAndIncrement()));
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
                    // DO NOTHING
                }
            }
        }
    }
}
