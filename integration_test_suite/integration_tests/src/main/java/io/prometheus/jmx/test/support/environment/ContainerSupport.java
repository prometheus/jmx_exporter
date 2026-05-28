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
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Utility methods for orchestrating Docker container and network shutdown, ensuring graceful
 * termination before Testcontainers cleanup.
 */
public class ContainerSupport {

    private static final int NETWORK_CLOSE_RETRIES = 5;
    private static final long NETWORK_CLOSE_RETRY_DELAY_MS = 100L;

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

    /**
     * Waits for a Docker network to close, retrying up to {@value #NETWORK_CLOSE_RETRIES} times
     * with a {@value #NETWORK_CLOSE_RETRY_DELAY_MS} ms delay between attempts. Returns immediately
     * if the network is {@code null} or has no network ID.
     *
     * @param network the Docker network to close; may be {@code null}
     * @throws EnvironmentException if the network fails to close after all retries
     */
    public static void waitForShutdown(Network network) {
        if (network == null) {
            return;
        }

        String networkId = network.getId();
        if (networkId == null || networkId.isBlank()) {
            return;
        }

        Throwable lastFailure = null;

        for (int attempt = 1; attempt <= NETWORK_CLOSE_RETRIES; attempt++) {
            try {
                network.close();
            } catch (Exception e) {
                lastFailure = e;
            }

            if (isNetworkRemoved(networkId)) {
                return;
            }

            if (attempt < NETWORK_CLOSE_RETRIES) {
                try {
                    Thread.sleep(NETWORK_CLOSE_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EnvironmentException(
                            "Interrupted while waiting for Docker network to close: " + networkId);
                }
            }
        }

        throw new EnvironmentException(
                "Failed to close Docker network after %d retries: %s".formatted(NETWORK_CLOSE_RETRIES, networkId),
                lastFailure);
    }

    private static boolean isNetworkRemoved(String networkId) {
        try {
            DockerClientFactory.lazyClient()
                    .inspectNetworkCmd()
                    .withNetworkId(networkId)
                    .exec();
            return false;
        } catch (NotFoundException ignored) {
            return true;
        }
    }
}
