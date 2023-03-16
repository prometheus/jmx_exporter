/*
 * Copyright 2022-2023 Douglas Hoard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Class to implement common test utility methods
 */
public final class TestUtils {

    /**
     * Constructor
     */
    private TestUtils() {
        // DO NOTHING
    }

    /**
     * Method to close a network
     *
     * @param network
     * @return
     */
    public static Network close(Network network) {
        if (network != null) {
            network.close();
        }
        return null;
    }

    /**
     * Method to close a container
     *
     * @param genericContainer
     * @return
     */
    public static GenericContainer<?> close(GenericContainer<?> genericContainer) {
        if (genericContainer != null) {
            genericContainer.close();
        }
        return null;
    }
}
