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
import org.testcontainers.containers.output.OutputFrame;

/**
 * Consumes Testcontainers log output frames and prefixes each line with a label
 * and Docker image name for console display.
 */
public class ContainerLogConsumer implements Consumer<OutputFrame> {

    private final String prefix;
    private final String dockerImage;

    /**
     * Creates a container log consumer that prefixes log lines with the given label and image name.
     *
     * @param prefix the label to prepend to each log line, identifying the container role
     * @param dockerImage the Docker image name to include in the log prefix for identification
     */
    public ContainerLogConsumer(String prefix, String dockerImage) {
        this.prefix = prefix;
        this.dockerImage = dockerImage;
    }

    /**
     * Prints the output frame content to standard output with the configured prefix,
     * skipping blank lines.
     *
     * @param outputFrame the log output frame from the container
     */
    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
            if (!string.isBlank()) {
                System.out.println("[" + prefix + "] " + dockerImage + " | " + string);
            }
        }
    }
}
