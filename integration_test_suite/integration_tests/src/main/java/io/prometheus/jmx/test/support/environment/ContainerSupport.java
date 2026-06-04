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

import com.github.dockerjava.api.command.WaitContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import org.testcontainers.containers.GenericContainer;

/**
 * Utility methods for orchestrating Docker container shutdown, ensuring graceful
 * termination before Testcontainers cleanup.
 */
public class ContainerSupport {

    private ContainerSupport() {
        // Intentionally empty
    }

    /**
     * Waits for a generic container to shut down, returning immediately if the container
     * is {@code null} or has no container ID.
     *
     * @param container the container to wait for; may be {@code null}
     */
    public static void waitForShutdown(GenericContainer<?> container) {
        if (container == null) {
            return;
        }

        String containerId = container.getContainerId();
        if (containerId == null || containerId.isBlank()) {
            return;
        }

        try (WaitContainerCmd waitCommand = container.getDockerClient().waitContainerCmd(containerId)) {
            waitCommand.start().awaitStatusCode();
        } catch (NotFoundException ignored) {
            // Container already removed by Ryuk/Testcontainers cleanup
        }
    }
}
