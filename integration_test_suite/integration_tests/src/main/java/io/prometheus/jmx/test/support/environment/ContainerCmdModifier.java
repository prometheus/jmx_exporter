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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ulimit;
import java.util.function.Consumer;

/** Class to implement ContainerCmdModifier */
public class ContainerCmdModifier implements Consumer<CreateContainerCmd> {

    private static final long MEMORY_BYTES = 1073741824;
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;

    private static final String NOFILE = "nofile";
    private static final long UFILE_SOFT = 65536L;
    private static final long UFILE_HARD = 65536L;

    private static final ContainerCmdModifier SINGLETON = new ContainerCmdModifier();

    /** Constructor */
    private ContainerCmdModifier() {
        // INTENTIONALLY BLANK
    }

    @Override
    public void accept(CreateContainerCmd createContainerCmd) {
        HostConfig hostConfig = createContainerCmd.getHostConfig();
        if (hostConfig != null) {
            hostConfig
                    .withMemory(MEMORY_BYTES)
                    .withMemorySwap(MEMORY_SWAP_BYTES)
                    .withUlimits(new Ulimit[] {new Ulimit(NOFILE, UFILE_SOFT, UFILE_HARD)});
        }
    }

    /**
     * Method to get the singleton instance of TestContainerConfigureCmd
     *
     * @return the singleton instance of TestContainerConfigureCmd
     */
    public static ContainerCmdModifier getInstance() {
        return SINGLETON;
    }
}
