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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * IsolatorJavaAgent Java agent for loading and running multiple Java agents.
 *
 * <p>This class is responsible for loading Java agents from specified jar files and executing their
 * main methods. It uses a custom classloader to ensure that the agent classes are loaded correctly.
 */
@SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.UselessPureMethodCall"})
public class IsolatorJavaAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsolatorJavaAgent.class);

    private static final long TIMEOUT_MILLISECONDS = 60000;

    private static final String JAVA_AGENT_CLASS_NAME = "io.prometheus.jmx.JavaAgent";

    private static final String AGENT_MAIN_METHOD = "agentmain";

    private static final String THREAD_NAME = "isolator-javaagent";

    static {
        // Get the platform MBean server to ensure that
        // it's initialized prior to the application
        ManagementFactory.getPlatformMBeanServer();
    }

    /** Default constructor for IsolatorJavaAgent. */
    public IsolatorJavaAgent() {
        // INTENTIONALLY BLANK
    }

    /**
     * Java agent main
     *
     * @param agentArgument agentArgument
     * @param instrumentation instrumentation
     */
    public static void agentmain(String agentArgument, Instrumentation instrumentation) {
        premain(agentArgument, instrumentation);
    }

    /**
     * Java agent premain
     *
     * @param agentArgument agentArgument
     * @param instrumentation instrumentation
     */
    public static void premain(String agentArgument, Instrumentation instrumentation) {
        Package pkg = IsolatorJavaAgent.class.getPackage();
        String version = pkg.getImplementationVersion();

        LOGGER.info("IsolatorJavaAgent v%s", version);
        LOGGER.info("Starting ...");
        LOGGER.info("agent argument [%s]", agentArgument);

        try {
            List<String> javaAgentArguments =
                    Arrays.stream(agentArgument.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());

            LOGGER.info(
                    "%s JMX Exporter%s defined",
                    javaAgentArguments.size(), javaAgentArguments.size() == 1 ? "" : "s");

            for (int i = 0; i < javaAgentArguments.size(); i++) {
                int index = i + 1;
                String javaAgentArgument = javaAgentArguments.get(i);
                String jarPath = javaAgentArgument.substring(0, javaAgentArgument.indexOf("="));
                String options = javaAgentArgument.substring(javaAgentArgument.indexOf("=") + 1);

                LOGGER.info("JMX Exporter[%d] configuration ...", index);
                LOGGER.info("jar [%s]", jarPath);
                LOGGER.info("agent arguments [%s]", options);

                LOGGER.info("Starting JMX Exporter[%d] ...", index);

                ClassLoader classLoader =
                        new JarClassLoader(jarPath, ClassLoader.getSystemClassLoader());

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
     * Run the Java agent.
     *
     * @param agentArgument the agent argument to pass to the Java agent
     * @param instrumentation the Instrumentation instance
     * @param classLoader the ClassLoader to use for loading the Java agent
     * @throws Throwable if an error occurs during Java agent execution
     */
    private static void runJavaAgent(
            String agentArgument, Instrumentation instrumentation, ClassLoader classLoader)
            throws Throwable {
        final AtomicReference<Throwable> throwableAtomicReference = new AtomicReference<>();

        Thread thread =
                new Thread(
                        () -> {
                            try {
                                // Set the context class loader to the new URLClassLoader
                                // so that any spawned threads have the correct classloader
                                Thread.currentThread().setContextClassLoader(classLoader);

                                // Load the Java agent class
                                Class<?> javaAgentClass =
                                        classLoader.loadClass(JAVA_AGENT_CLASS_NAME);

                                // Resolve the Java agent main method
                                Method javaAgentMainMethod =
                                        javaAgentClass.getMethod(
                                                AGENT_MAIN_METHOD,
                                                String.class,
                                                Instrumentation.class);

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
