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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;

/**
 * Creates and tears down Docker networks for integration test container communication.
 *
 * <p>Unlike {@link Network#close()}, which is guarded by a single-use flag and delegates to
 * Testcontainers' resource reaper, {@link #close(Network)} calls the Docker API directly to
 * remove the network. This enables true retry behavior when endpoints are still detaching.
 */
public final class NetworkSupport {

    private static final int CLOSE_RETRIES = 10;
    private static final long CLOSE_RETRY_DELAY_MS = 200L;

    private NetworkSupport() {
        // Intentionally empty
    }

    /**
     * Creates a new Docker network and eagerly initializes it by calling {@link Network#getId()}.
     *
     * @return the newly created network
     */
    public static Network create() {
        var network = Network.newNetwork();
        network.getId();
        return network;
    }

    /**
     * Removes the given Docker network via the Docker API, retrying up to {@value #CLOSE_RETRIES}
     * times with a {@value #CLOSE_RETRY_DELAY_MS} ms delay between attempts. Returns immediately
     * if the network is {@code null}, has no network ID, or has already been removed.
     *
     * <p>This method bypasses {@link Network#close()} and calls
     * {@code removeNetworkCmd(networkId).exec()} directly so that every retry attempt actually
     * issues a removal request rather than being gated by Testcontainers' single-use flag.
     *
     * @param network the Docker network to remove; may be {@code null}
     * @throws EnvironmentException if the network cannot be removed after all retries
     */
    public static void close(Network network) {
        if (network == null) {
            return;
        }

        String networkId = network.getId();
        if (networkId == null || networkId.isBlank()) {
            return;
        }

        DockerClient dockerClient = DockerClientFactory.lazyClient();

        for (int attempt = 1; attempt <= CLOSE_RETRIES; attempt++) {
            try {
                dockerClient.removeNetworkCmd(networkId).exec();
            } catch (NotFoundException e) {
                return;
            } catch (Exception ignored) {
                // transient failure (e.g. endpoints still attached) — retry
            }

            if (isNetworkGone(dockerClient, networkId)) {
                return;
            }

            if (attempt < CLOSE_RETRIES) {
                try {
                    Thread.sleep(CLOSE_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new EnvironmentException("Interrupted while closing Docker network: " + networkId);
                }
            }
        }

        throw new EnvironmentException(
                "Failed to close Docker network after " + CLOSE_RETRIES + " retries: " + networkId);
    }

    private static boolean isNetworkGone(DockerClient dockerClient, String networkId) {
        try {
            dockerClient.inspectNetworkCmd().withNetworkId(networkId).exec();
            return false;
        } catch (NotFoundException e) {
            return true;
        }
    }
}
