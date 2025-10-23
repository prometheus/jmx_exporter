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

package io.prometheus.jmx.test.support.util;

import io.prometheus.jmx.test.support.environment.IsolatorExporterTestEnvironment;
import io.prometheus.jmx.test.support.environment.JmxExporterMode;
import io.prometheus.jmx.test.support.environment.JmxExporterTestEnvironment;
import java.util.Optional;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;

/** Class to implement TestSupport */
public class TestSupport {

    /** Network configuration constant */
    public static final String NETWORK = "network";

    private static final String BUILD_INFO_JAVAAGENT = "jmx_prometheus_javaagent";

    private static final String BUILD_INFO_STANDALONE = "jmx_prometheus_standalone";

    /** Constructor */
    private TestSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Creates an ArgumentContext scoped Network if not created at the ClassContext scope
     *
     * @param argumentContext argumentContext
     * @return a Network
     */
    public static Network getOrCreateNetwork(ArgumentContext argumentContext) {
        Network network = argumentContext.classContext().map().getAs(NETWORK);
        if (network == null) {
            // Create the network at the test argument scope
            // Get the id to force the network creation
            network = Network.newNetwork();
            network.getId();

            argumentContext.map().put(NETWORK, network);
        }
        return network;
    }

    /**
     * Initializes the ExporterTestEnvironment
     *
     * @param argumentContext argumentContext
     * @param network network
     * @param testClass testClass
     */
    public static void initializeExporterTestEnvironment(
            ArgumentContext argumentContext, Network network, Class<?> testClass) {
        argumentContext
                .testArgument(JmxExporterTestEnvironment.class)
                .payload()
                .initialize(testClass, network);
    }

    /**
     * Initializes the IsolatorExporterTestEnvironment
     *
     * @param argumentContext argumentContext
     * @param network network
     * @param testClass testClass
     */
    public static void initializeIsolatorExporterTestEnvironment(
            ArgumentContext argumentContext, Network network, Class<?> testClass) {
        argumentContext
                .testArgument(IsolatorExporterTestEnvironment.class)
                .payload()
                .initialize(testClass, network);
    }

    /**
     * Destroys the ExporterTestEnvironment
     *
     * @param argumentContext argumentContext
     */
    public static void destroyExporterTestEnvironment(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.testArgument(JmxExporterTestEnvironment.class))
                .ifPresent(
                        exporterTestEnvironmentArgument ->
                                exporterTestEnvironmentArgument.payload().destroy());
    }

    /**
     * Destroys the IsolatorExporterTestEnvironment
     *
     * @param argumentContext argumentContext
     */
    public static void destroyIsolatorExporterTestEnvironment(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.testArgument(IsolatorExporterTestEnvironment.class))
                .ifPresent(
                        exporterTestEnvironmentArgument ->
                                exporterTestEnvironmentArgument.payload().destroy());
    }

    /**
     * Destroys an ArgumentContext scoped Network
     *
     * @param argumentContext argumentContext
     */
    public static void destroyNetwork(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.map().removeAs(NETWORK, Network.class))
                .ifPresent(Network::close);
    }

    /**
     * Method to get the build info name based on the JMX exporter mode
     *
     * @param jmxExporterMode jmxExporterMode
     * @return the build info name
     */
    public static String getBuildInfoName(JmxExporterMode jmxExporterMode) {
        return jmxExporterMode == JmxExporterMode.JavaAgent
                ? BUILD_INFO_JAVAAGENT
                : BUILD_INFO_STANDALONE;
    }
}
