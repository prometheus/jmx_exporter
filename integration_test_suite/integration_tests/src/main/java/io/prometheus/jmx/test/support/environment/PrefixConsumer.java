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

package io.prometheus.jmx.test.support.environment;

import java.util.function.Consumer;
import org.altcontainers.api.OutputFrame;

/**
 * A conditional {@link Consumer} of {@link OutputFrame} that prints container log lines to
 * {@link System#out} with a prefix and Docker image label.
 *
 * <p>Output is suppressed by default. Set the environment variable
 * {@code INTEGRATION_TESTS_CONTAINER_LOGS} or the system property
 * {@code integration.tests.container.logs} to {@code true} to enable printing.
 *
 * <p>This class is not instantiable.
 */
public final class PrefixConsumer {

    private static final String CONTAINER_LOGS_KEY = "integration.tests.container.logs";

    private static final Consumer<OutputFrame> NOOP = frame -> {};

    private static boolean ENABLED = resolveEnabled();

    private PrefixConsumer() {}

    /**
     * Returns a consumer that prints {@link OutputFrame} lines to {@link System#out} with the
     * given prefix and image label, or a no-op consumer if container log output is not enabled.
     *
     * @param prefix the log prefix (e.g. {@code "PROMETHEUS"})
     * @param image the Docker image name for the log label
     * @return a printing or no-op consumer
     */
    public static Consumer<OutputFrame> of(String prefix, String image) {
        if (!ENABLED) {
            return NOOP;
        }
        return frame -> System.out.println("[" + prefix + "] " + image + " | " + frame.utf8StringWithoutLineEnding());
    }

    private static boolean resolveEnabled() {
        String envValue = System.getenv(CONTAINER_LOGS_KEY.toUpperCase().replace('.', '_'));
        if (envValue != null) {
            return Boolean.parseBoolean(envValue);
        }
        return Boolean.getBoolean(CONTAINER_LOGS_KEY);
    }
}
