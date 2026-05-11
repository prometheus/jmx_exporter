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

/**
 * Shutdown hook for closing AutoCloseable resources during JVM shutdown.
 *
 * <p>This class extends {@link Thread} and implements a shutdown hook that ensures AutoCloseable
 * resources (such as HTTP servers and OpenTelemetry exporters) are properly closed when the JVM
 * terminates. Instances should be registered with {@link Runtime#addShutdownHook(Thread)}.
 *
 * <p>Thread-safety: This class is thread-safe. The shutdown hook may be called concurrently with
 * other shutdown hooks.
 */
public class AutoClosableShutdownHook extends Thread {

    /**
     * The AutoCloseable resource to be closed on shutdown.
     *
     * <p>May be any resource that implements {@link AutoCloseable}, such as {@code HTTPServer} or
     * {@code OpenTelemetryExporter}.
     */
    private final AutoCloseable autoCloseable;

    /**
     * Constructs a shutdown hook for the specified AutoCloseable resource.
     *
     * <p>The shutdown hook will call {@link AutoCloseable#close()} on the resource when the JVM
     * shuts down. Any exceptions thrown during closure are silently ignored to ensure other
     * shutdown hooks can complete.
     *
     * @param autoCloseable the AutoCloseable resource to close on shutdown, must not be
     *     {@code null}
     * @throws NullPointerException if {@code autoCloseable} is {@code null}
     */
    public AutoClosableShutdownHook(AutoCloseable autoCloseable) {
        this.autoCloseable = autoCloseable;
    }

    /**
     * Closes the wrapped AutoCloseable resource during JVM shutdown.
     *
     * <p>Any exceptions thrown during closure are silently ignored to ensure other shutdown
     * hooks can complete without disruption.
     */
    @Override
    public void run() {
        try {
            autoCloseable.close();
        } catch (Throwable t) {
            // INTENTIONALLY BLANK
        }
    }
}
