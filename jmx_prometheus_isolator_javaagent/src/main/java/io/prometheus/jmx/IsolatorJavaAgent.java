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
     * Default constructor for IsolatorJavaAgent.
     *
     * <p>This constructor is intentionally empty as this is a utility class with only static
     * methods.
     */
    public IsolatorJavaAgent() {
        // INTENTIONALLY BLANK
    }

    /**
     * Java agent entry point for runtime attachment.
     *
     * <p>Called when the agent is attached to a running JVM via the attach API. Delegates to
     * {@link #premain(String, Instrumentation)} for actual initialization.
     *
     * @param agentArgument the agent argument string containing comma-separated jar configurations,
     *     may be {@code null} or empty
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
     * <p>On failure, logs the error and exits the JVM with status code 1.
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
            List<String> javaAgentArguments =
                    Arrays.stream(agentArgument.split(",")).map(String::trim).collect(Collectors.toList());

            LOGGER.info(
                    "%s JMX Exporter%s defined", javaAgentArguments.size(), javaAgentArguments.size() == 1 ? "" : "s");

            for (int i = 0; i < javaAgentArguments.size(); i++) {
                int index = i + 1;
                String javaAgentArgument = javaAgentArguments.get(i);
                String jarPath = javaAgentArgument.substring(0, javaAgentArgument.indexOf("="));
                String options = javaAgentArgument.substring(javaAgentArgument.indexOf("=") + 1);

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
     * <p>Creates a new thread with the agent's classloader as the context classloader, loads the
     * Java agent class, and invokes its {@code agentmain} method. The thread blocks with a timeout
     * to ensure agent startup completes.
     *
     * @param agentArgument the agent argument to pass to the agent
     * @param instrumentation the instrumentation instance from the JVM
     * @param classLoader the isolated classloader for the agent
     * @throws Throwable if agent startup fails or times out
     */
    private static void runJavaAgent(String agentArgument, Instrumentation instrumentation, ClassLoader classLoader)
            throws Throwable {
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                // Set the context class loader to the new URLClassLoader
                // so that any spawned threads have the correct classloader
                Thread.currentThread().setContextClassLoader(classLoader);

                // Load the Java agent class
                Class<?> javaAgentClass = classLoader.loadClass(JAVA_AGENT_CLASS_NAME);

                // Resolve the Java agent main method
                Method javaAgentMainMethod =
                        javaAgentClass.getMethod(AGENT_MAIN_METHOD, String.class, Instrumentation.class);

                // Invoke the Java agent main method
                javaAgentMainMethod.invoke(null, agentArgument, instrumentation);
            } catch (Throwable t) {
                throwableAtomicReference.set(t);
            }
        });

        thread.setName(THREAD_NAME);
        thread.start();
        thread.join(TIMEOUT_MILLISECONDS, 0);

        if (throwableAtomicReference.get() != null) {
            throw throwableAtomicReference.get();
        }
    }
}
