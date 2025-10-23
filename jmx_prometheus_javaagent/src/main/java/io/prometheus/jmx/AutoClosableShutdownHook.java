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
 * ShutdownHook class to handle the shutdown of an auto-closeable resource.
 *
 * <p>This class extends the Thread class and implements a shutdown hook that closes an
 * AutoCloseable resource when the application is shutting down.
 */
public class AutoClosableShutdownHook extends Thread {

    private final AutoCloseable autoCloseable;

    /**
     * Constructor for ShutdownHook.
     *
     * @param autoCloseable The AutoCloseable resource to be closed on shutdown
     */
    public AutoClosableShutdownHook(AutoCloseable autoCloseable) {
        this.autoCloseable = autoCloseable;
    }

    @Override
    public void run() {
        try {
            autoCloseable.close();
        } catch (Throwable t) {
            // INTENTIONALLY BLANK
        }
    }
}
