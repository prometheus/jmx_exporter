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

package io.prometheus.jmx.test.support.filter;

import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Filters JMX exporter test environments to exclude Docker images that do not support
 * PKCS12 keystore configurations.
 */
public class PKCS12KeyStoreExporterTestEnvironmentFilter implements Predicate<JmxExporterTestEnvironment> {

    private final Set<String> filteredDockerImages;

    /**
     * Creates a filter that excludes Java 8 and IBM Java images incompatible with PKCS12 keystores.
     */
    public PKCS12KeyStoreExporterTestEnvironmentFilter() {
        filteredDockerImages = new HashSet<>();
        filteredDockerImages.add("eclipse-temurin:8-alpine");
        filteredDockerImages.add("ghcr.io/graalvm/jdk:java8");
        filteredDockerImages.add("ibmjava:8");
        filteredDockerImages.add("ibmjava:8-jre");
        filteredDockerImages.add("ibmjava:8-sdk");
        filteredDockerImages.add("ibmjava:8-sfj");
        filteredDockerImages.add("ibmjava:11");
    }

    /**
     * Returns {@code true} if the test environment's Docker image supports PKCS12 keystores.
     *
     * @param jmxExporterTestEnvironment the test environment to evaluate
     * @return {@code true} if the environment is compatible, {@code false} if it should be filtered out
     */
    @Override
    public boolean test(JmxExporterTestEnvironment jmxExporterTestEnvironment) {
        return !filteredDockerImages.contains(jmxExporterTestEnvironment.getJavaDockerImage());
    }
}
