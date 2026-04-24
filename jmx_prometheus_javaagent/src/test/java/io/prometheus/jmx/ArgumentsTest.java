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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.prometheus.jmx.common.ConfigurationException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parameterized test for parsing Java agent arguments.
 *
 * <p>Tests various argument formats including:
 *
 * <ul>
 *   <li>Port-only format: {@code port:configFile}
 *   <li>Host and port format: {@code host:port:configFile}
 *   <li>IPv6 addresses in brackets: {@code [ipv6]:port:configFile}
 *   <li>Config-file-only format: {@code configFile}
 * </ul>
 *
 * <p>Validates that the parser correctly extracts HTTP enablement, host, port, and filename from
 * each argument string.
 */
public class ArgumentsTest {

    /**
     * Constant for HTTP enabled state in test definitions.
     */
    private static final boolean HTTP_ENABLED = true;

    /**
     * Constant for HTTP disabled state in test definitions.
     */
    private static final boolean HTTP_DISABLED = !HTTP_ENABLED;

    /**
     * Constant for valid configuration state in test definitions.
     */
    private static final boolean VALID_CONFIGURATION = true;

    /**
     * Constant for invalid configuration state in test definitions.
     */
    private static final boolean INVALID_CONFIGURATION = false;

    /**
     * Test data array containing various valid and invalid argument configurations.
     */
    private static final ArgumentsTestDefinition[] ARGUMENTS_TEST_DEFINITIONS = {
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "0.0.0.0",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "0.0.0.0",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "0.0.0.0",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "0.0.0.0",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "0.0.0.0",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhost.domain.com:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "myhost.domain.com",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhost.domain.com:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "myhost.domain.com",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhost.domain.com:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "myhost.domain.com",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhost.domain.com:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "myhost.domain.com",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhost.domain.com:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "myhost.domain.com",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "myhostname.sub-domain.prometheus.org",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "myhostname.sub-domain.prometheus.org",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "myhostname.sub-domain.prometheus.org",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "127.0.0.1:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "127.0.0.1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "127.0.0.1:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "127.0.0.1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "127.0.0.1:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "127.0.0.1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "::",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "::",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "::",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::1]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "::1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::1]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "::1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::1]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "::1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::ffff:192.168.1.1]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "::ffff:192.168.1.1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::ffff:192.168.1.1]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "::ffff:192.168.1.1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::ffff:192.168.1.1]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "::ffff:192.168.1.1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[fe80::1]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "fe80::1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[fe80::1]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "fe80::1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[fe80::1]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "fe80::1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:db8::1]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "2001:db8::1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:db8::1]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "2001:db8::1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:db8::1]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "2001:db8::1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "2001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "2001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[2001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "2001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "001:0db8:0a0b:12f0:0000:0000:0000:0001",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "/opt/prometheus/config.yaml",
                HTTP_DISABLED,
                null,
                null,
                "/opt/prometheus/config.yaml"),
        // Invalid port - port 0
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION, "0:/opt/prometheus/config.yaml", HTTP_ENABLED, null, null, null),
        // Invalid port - port 65536
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION, "65536:/opt/prometheus/config.yaml", HTTP_ENABLED, null, null, null),
        // Invalid port - port 99999
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION, "99999:/opt/prometheus/config.yaml", HTTP_ENABLED, null, null, null),
        // Invalid port - host with port 0
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "myhost.domain.com:0:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
        // Invalid port - host with port 65536
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "myhost.domain.com:65536:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
        // Invalid port - host with port 99999
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "myhost.domain.com:99999:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
        // Invalid port - IPv6 with port 0
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:0:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
        // Invalid port - IPv6 with port 65536
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:65536:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
        // Invalid port - IPv6 with port 99999
        new ArgumentsTestDefinition(
                INVALID_CONFIGURATION,
                "[001:0db8:0a0b:12f0:0000:0000:0000:0001]:99999:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                null,
                null,
                null),
    };

    /**
     * Provides test data for parameterized tests.
     *
     * @return a stream of ArgumentsTestDefinition instances
     */
    public static Stream<ArgumentsTestDefinition> arguments() {
        return Stream.of(ARGUMENTS_TEST_DEFINITIONS);
    }

    /**
     * Tests argument parsing with various valid and invalid argument strings.
     *
     * @param argumentsTestDefinition the test definition containing expected results
     */
    @ParameterizedTest
    @MethodSource("arguments")
    public void testsArgument(ArgumentsTestDefinition argumentsTestDefinition) {
        argumentsTestDefinition.assertValid();
    }

    /**
     * Test definition for argument parsing tests.
     *
     * <p>Encapsulates the expected outcomes for parsing a specific argument string, including
     * whether the configuration is valid, HTTP enablement, host, port, and filename.
     */
    public static class ArgumentsTestDefinition {

        /**
         * The raw argument string to parse.
         */
        private final String argument;

        /**
         * Whether the argument represents a valid configuration.
         */
        private final boolean validConfiguration;

        /**
         * Expected HTTP enabled state.
         */
        private final boolean httpEnabled;

        /**
         * Expected host address, or {@code null} if HTTP is disabled.
         */
        private final String host;

        /**
         * Expected port number, or {@code null} if HTTP is disabled.
         */
        private final Integer port;

        /**
         * Expected configuration file path.
         */
        private final String filename;

        /**
         * Constructs a test definition with expected parsing results.
         *
         * @param validConfiguration whether the argument is valid
         * @param argument the raw argument string to test
         * @param httpEnabled expected HTTP enabled state
         * @param host expected host address
         * @param port expected port number
         * @param filename expected configuration file path
         */
        public ArgumentsTestDefinition(
                boolean validConfiguration,
                String argument,
                boolean httpEnabled,
                String host,
                Integer port,
                String filename) {
            this.argument = argument;
            this.validConfiguration = validConfiguration;
            this.httpEnabled = httpEnabled;
            this.host = host;
            this.port = port;
            this.filename = filename;
        }

        /**
         * Asserts that parsing the argument produces the expected results.
         *
         * <p>If the configuration is valid, asserts that the parsed arguments match the expected
         * httpEnabled, host, port, and filename values. If invalid, asserts that parsing throws a
         * ConfigurationException.
         */
        public void assertValid() {
            if (validConfiguration) {
                Arguments arguments = Arguments.parse(argument);
                assertThat(arguments.isHttpEnabled()).isEqualTo(httpEnabled);
                assertThat(arguments.getHost()).isEqualTo(host);
                assertThat(arguments.getPort()).isEqualTo(port);
                assertThat(arguments.getFilename()).isEqualTo(filename);
            } else {
                assertThatExceptionOfType(ConfigurationException.class).isThrownBy(() -> Arguments.parse(argument));
            }
        }
    }
}
