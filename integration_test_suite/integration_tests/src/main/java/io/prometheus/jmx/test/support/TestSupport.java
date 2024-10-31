package io.prometheus.jmx.test.support;

import java.util.Optional;
import org.testcontainers.containers.Network;
import org.verifyica.api.ArgumentContext;
import org.verifyica.api.ClassContext;

/** Class to implement TestSupport */
public class TestSupport {

    /** Network configuration constant */
    public static final String NETWORK = "network";

    /** Constructor */
    private TestSupport() {
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
     * Initializes the OpenTelemetryTestEnvironment
     *
     * @param argumentContext argumentContext
     * @param network network
     * @param testClass testClass
     */
    public static void initializeOpenTelemetryTestEnvironment(
            ArgumentContext argumentContext, Network network, Class<?> testClass) {
        argumentContext
                .testArgument(OpenTelemetryTestEnvironment.class)
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
     * Destroys the OpenTelemetryTestEnvironment
     *
     * @param argumentContext argumentContext
     */
    public static void destroyOpenTelemetryTestEnvironment(ArgumentContext argumentContext) {
        Optional.ofNullable(argumentContext.testArgument(OpenTelemetryTestEnvironment.class))
                .ifPresent(
                        openTelemetryTestEnvironment ->
                                openTelemetryTestEnvironment.payload().destroy());
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
     * Destroys a ClassContext scoped Network
     *
     * @param classContext classContext
     */
    public static void destroyNetwork(ClassContext classContext) {
        Optional.ofNullable(classContext.map().removeAs(NETWORK, Network.class))
                .ifPresent(Network::close);
    }
}
