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

public class ArgumentsTest {

    private static final boolean HTTP_ENABLED = true;
    private static final boolean HTTP_DISABLED = !HTTP_ENABLED;
    private static final boolean VALID_CONFIGURATION = true;

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
                "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "myhostname.sub-domain.prometheus.org",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "myhostname.sub_domain.prometheus.org",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "myhostname.sub_domain.prometheus.org",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "myhostname.sub_domain.prometheus.org",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "myhostname.sub_domain.prometheus.org",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "myhostname.sub_domain.prometheus.org",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/config.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/config_file.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/config_file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/config-file.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/jmx-exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "192.168.1.1:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
                HTTP_ENABLED,
                "192.168.1.1",
                12345,
                "/opt/prometheus/jmx_exporter/config-file.yaml"),
        new ArgumentsTestDefinition(
                VALID_CONFIGURATION,
                "[::/0]:12345:/opt/prometheus/config.yaml",
                HTTP_ENABLED,
                "::/0",
                12345,
                "/opt/prometheus/config.yaml"),
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
    };

    public static Stream<ArgumentsTestDefinition> arguments() {
        return Stream.of(ARGUMENTS_TEST_DEFINITIONS);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testsArgument(ArgumentsTestDefinition argumentsTestDefinition) {
        argumentsTestDefinition.assertValid();
    }

    public static class ArgumentsTestDefinition {

        private final String argument;
        private final boolean validConfiguration;
        private final boolean httpEnabled;
        private final String host;
        private final Integer port;
        private final String filename;

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

        public void assertValid() {
            if (validConfiguration) {
                Arguments arguments = Arguments.parse(argument);
                assertThat(arguments.isHttpEnabled()).isEqualTo(httpEnabled);
                assertThat(arguments.getHost()).isEqualTo(host);
                assertThat(arguments.getPort()).isEqualTo(port);
                assertThat(arguments.getFilename()).isEqualTo(filename);
            } else {
                assertThatExceptionOfType(ConfigurationException.class)
                        .isThrownBy(() -> Arguments.parse(argument));
            }
        }
    }
}
