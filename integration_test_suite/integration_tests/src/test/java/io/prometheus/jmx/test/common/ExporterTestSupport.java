/*
 * Copyright (C) 2024-present The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.common;

import java.util.Optional;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;
import org.verifyica.api.Trap;

/** Class to implement ExporterTestSupport */
public class ExporterTestSupport {

    /** Network configuration constant */
    public static final String NETWORK = "network";

    /** Constructor */
    private ExporterTestSupport() {
        // INTENTIONALLY BLANK
    }

    /**
     * Creates a ClassContext scoped Network
     *
     * @param classContext classContext
     */
    public static void getOrCreateNetwork(ClassContext classContext) {
        if (classContext.testArgumentParallelism() == 1) {
            // Create the network at the test class scope
            // Get the id to force the network creation
            Network network = Network.newNetwork();
            network.getId();

            classContext.map().put(NETWORK, network);
        }
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
                .testArgument(ExporterTestEnvironment.class)
                .payload()
                .initialize(testClass, network);
    }

    /**
     * Destroys the ExporterTestEnvironment
     *
     * @param argumentContext argumentContext
     */
    public static void destroyExporterTestEnvironment(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.testArgument(ExporterTestEnvironment.class))
                .ifPresent(
                        exporterTestEnvironmentArgument ->
                                exporterTestEnvironmentArgument.payload().destroy());
    }

    /**
     * Destroys an ArgumentContext scoped Network
     *
     * @param argumentContext argumentContext
     * @throws Throwable Throwable
     */
    public static void destroyNetwork(ArgumentContext argumentContext) throws Throwable {
        new Trap(
                        () ->
                                Optional.ofNullable(
                                                argumentContext
                                                        .map()
                                                        .removeAs(NETWORK, Network.class))
                                        .ifPresent(Network::close))
                .assertEmpty();
    }

    /**
     * Destroys a ClassContext scoped Network
     *
     * @param classContext classContext
     * @throws Throwable Throwable
     */
    public static void destroyNetwork(ClassContext classContext) throws Throwable {
        new Trap(
                        () ->
                                Optional.ofNullable(
                                                classContext.map().removeAs(NETWORK, Network.class))
                                        .ifPresent(Network::close))
                .assertEmpty();
    }
}
