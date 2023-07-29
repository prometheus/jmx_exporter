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

package io.prometheus.jmx;

import static org.assertj.core.api.Assertions.fail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class JavaAgentConfigurationRegexTest {

    private static final String[] ARGUMENTS = {
        "12345:/opt/prometheus/config.yaml",
        "12345:/opt/prometheus/config_file.yaml",
        "12345:/opt/prometheus/config-file.yaml",
        "12345:/opt/prometheus/jmx-exporter/config-file.yaml",
        "12345:/opt/prometheus/jmx_exporter/config-file.yaml",
        "myhost.domain.com:12345:/opt/prometheus/config.yaml",
        "myhost.domain.com:12345:/opt/prometheus/config_file.yaml",
        "myhost.domain.com:12345:/opt/prometheus/config-file.yaml",
        "myhost.domain.com:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
        "myhost.domain.com:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
        "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config.yaml",
        "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config_file.yaml",
        "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/config-file.yaml",
        "myhostname.sub-domain.prometheus.org:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
        "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config.yaml",
        "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config_file.yaml",
        "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/config-file.yaml",
        "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
        "myhostname.sub_domain.prometheus.org:12345:/opt/prometheus/jmx_exporter/config-file.yaml",
        "192.168.1.1:12345:/opt/prometheus/config.yaml",
        "192.168.1.1:12345:/opt/prometheus/config_file.yaml",
        "192.168.1.1:12345:/opt/prometheus/config-file.yaml",
        "192.168.1.1:12345:/opt/prometheus/jmx-exporter/config-file.yaml",
        "192.168.1.1:12345:/opt/prometheus/jmx_exporter/config-file.yaml"
    };

    @Test
    public void testConfigurationRegex() {
        Pattern pattern = Pattern.compile(JavaAgent.CONFIGURATION_REGEX);
        Matcher matcher = pattern.matcher("");

        for (String argument : ARGUMENTS) {
            matcher.reset(argument);
            if (!matcher.matches()) {
                fail(String.format("Expected a match for [%s]", argument));
            }
        }
    }
}
