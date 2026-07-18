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

import io.prometheus.jmx.logger.Logger;
import io.prometheus.jmx.logger.LoggerFactory;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Java agent for loading and running multiple JMX exporter agents in isolated classloaders.
 *
 * <p>This agent allows multiple JMX exporter instances to be loaded with different versions or
 * configurations, each in its own isolated classloader. This avoids classpath conflicts between
 * different exporter versions.
 *
 * <p>The agent argument format is a comma-separated list of jar paths with arguments:
 *
 * <pre>{@code
 * /path/to/exporter1.jar=config1,/path/to/exporter2.jar=config2
 * }</pre>
 *
 * <p>Each JMX exporter is loaded in a separate classloader and started in its own thread to
 * prevent classloader conflicts.
 *
 * <p>This class is not instantiable and all methods are static.
 *
 * <p>Thread-safety: This class is thread-safe. Each agent is started in its own thread.
 */
@SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.UselessPureMethodCall"})
public class IsolatorJavaAgent {

    static {
        LoggerFactory.setDefaultBackend(LoggerFactory.Backend.NATIVE);
    }

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolatorJavaAgent.class);

    /**
     * Timeout in milliseconds for agent startup.
     */
    private static final long TIMEOUT_MILLISECONDS = 60000;

    /**
     * Fully qualified class name of the Java agent to load.
     */
    private static final String JAVA_AGENT_CLASS_NAME = "io.prometheus.jmx.JavaAgent";

    /**
     * Name of the agent main method to invoke.
     */
    private static final String AGENT_MAIN_METHOD = "agentmain";

    /**
     * Thread name for agent startup threads.
     */
    private static final String THREAD_NAME = "isolator-javaagent";

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This is a utility class with only static methods.
     */
    private IsolatorJavaAgent() {
        // Intentionally empty
    }

    /**
     * Java agent entry point for runtime attachment.
     *
     * <p>Called when the agent is attached to a running JVM via the attach API. Delegates to
     * {@link #premain(String, Instrumentation)} for actual initialization.
     *
     * @param agentArgument the agent argument string containing comma-separated jar configurations,
     *     must not be {@code null} or empty
     * @param instrumentation the instrumentation instance provided by the JVM, may be {@code null}
     *     in some environments
     */
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        premain(agentArgument, instrumentation);
    }

    /**
     * Java agent entry point for JVM startup.
     *
     * <p>Called by the JVM when the agent is loaded at startup via the {@code -javaagent} flag.
     * Parses the agent arguments, creates isolated classloaders for each JMX exporter, and starts
     * each in a separate thread.
     *
     * <p>On failure, logs the error and exits the JVM with status code 1. All exceptions are
     * handled internally and do not propagate to the caller.
     *
     * @param agentArgument the agent argument string containing comma-separated jar configurations,
     *     must not be {@code null} or empty
     * @param instrumentation the instrumentation instance provided by the JVM, may be {@code null}
     */
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        Package pkg = IsolatorJavaAgent.class.getPackage();
        String version = pkg.getImplementationVersion();

        LOGGER.info("IsolatorJavaAgent v%s", version);
        LOGGER.info("Starting ...");
        LOGGER.info("agent argument [%s]", agentArgument);

        try {
            if (agentArgument == null || agentArgument.trim().isEmpty()) {
                throw new IllegalArgumentException("Agent argument must not be null or empty; expected format: "
                        + "/path/to/exporter1.jar=config1,/path/to/exporter2.jar=config2");
            }

            List<String> javaAgentArguments =
                    Arrays.stream(agentArgument.split(",")).map(String::trim).collect(Collectors.toList());

            int argumentCount = javaAgentArguments.size();
            LOGGER.info("%s JMX Exporter%s defined", argumentCount, argumentCount == 1 ? "" : "s");

            for (int i = 0; i < javaAgentArguments.size(); i++) {
                int index = i + 1;
                String javaAgentArgument = javaAgentArguments.get(i);
                int equalsIndex = javaAgentArgument.indexOf("=");
                String jarPath = javaAgentArgument.substring(0, equalsIndex);
                String options = javaAgentArgument.substring(equalsIndex + 1);

                LOGGER.info("JMX Exporter[%d] configuration ...", index);
                LOGGER.info("jar [%s]", jarPath);
                LOGGER.info("agent arguments [%s]", options);

                LOGGER.info("Starting JMX Exporter[%d] ...", index);

                ClassLoader classLoader = new JarClassLoader(jarPath, ClassLoader.getSystemClassLoader());

                runJavaAgent(options, instrumentation, classLoader);

                LOGGER.info("JMX Exporter[%d] running", index);
            }

            LOGGER.info("Running");
        } catch (Throwable t) {
            synchronized (System.err) {
                System.err.println("Failed to start Prometheus JMX Exporter ...");
                System.err.println();
                t.printStackTrace(System.err);
                System.err.println();
                System.err.println("Prometheus JMX Exporter exiting");
                System.err.flush();
            }

            System.exit(1);
        }
    }

    /**
     * Runs a JMX exporter agent in an isolated classloader.
     *
     * <p>Creates a new daemon thread with the agent's classloader as the context classloader, loads
     * the Java agent class, and invokes its {@code agentmain} method. The calling thread blocks
     * with a timeout of {@value #TIMEOUT_MILLISECONDS} ms for agent startup to complete.
     *
     * <p>If the agent thread does not complete within the timeout, it is interrupted via {@link
     * Thread#interrupt()} and a warning is logged. The daemon thread will not block JVM shutdown.
     *
     * <p>Thread-safety: This method is thread-safe. Each invocation creates and manages its own
     * thread and {@link AtomicReference}.
     *
     * @param agentArgument the agent argument to pass to the agent, must not be {@code null}
     * @param instrumentation the instrumentation instance from the JVM, may be {@code null}
     * @param classLoader the isolated classloader for the agent, must not be {@code null}
     * @throws Throwable if agent startup fails within the timeout period
     */
    private static void runJavaAgent(String agentArgument, Instrumentation instrumentation, ClassLoader classLoader)
            throws Throwable {
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        Thread thread =
                new Thread(() -> runAgentTask(agentArgument, instrumentation, classLoader, throwableAtomicReference));

        thread.setName(THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
        thread.join(TIMEOUT_MILLISECONDS, 0);

        if (thread.isAlive()) {
            thread.interrupt();
            LOGGER.warn("Agent startup thread timed out after %d ms and was interrupted", TIMEOUT_MILLISECONDS);
        }

        if (throwableAtomicReference.get() != null) {
            throw throwableAtomicReference.get();
        }
    }

    /**
     * Runs the agent main method within the isolated classloader.
     *
     * <p>Sets the context classloader, loads the agent class, resolves the {@code agentmain}
     * method, and invokes it. Any thrown exception is captured in the provided {@link
     * AtomicReference} for propagation to the calling thread.
     *
     * <p>This method is designed for use as a {@link Runnable} target, extracted from a
     * lambda for clarity per Java 8 method reference idioms.
     *
     * @param agentArgument the agent argument to pass to the agent, must not be {@code null}
     * @param instrumentation the instrumentation instance from the JVM, may be {@code null}
     * @param classLoader the isolated classloader for the agent, must not be {@code null}
     * @param errorRef the {@link AtomicReference} to capture any thrown exception
     */
    private static void runAgentTask(
            String agentArgument,
            Instrumentation instrumentation,
            ClassLoader classLoader,
            AtomicReference<Throwable> errorRef) {
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class<?> javaAgentClass = classLoader.loadClass(JAVA_AGENT_CLASS_NAME);
            Method javaAgentMainMethod =
                    javaAgentClass.getMethod(AGENT_MAIN_METHOD, String.class, Instrumentation.class);
            javaAgentMainMethod.invoke(null, agentArgument, instrumentation);
        } catch (Throwable t) {
            errorRef.set(t);
        }
    }
}
