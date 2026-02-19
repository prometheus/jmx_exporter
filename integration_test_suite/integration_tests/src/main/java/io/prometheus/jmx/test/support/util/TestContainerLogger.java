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

package io.prometheus.jmx.test.support.util;

import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

/** Class to implement TestContainerLogger */
public class TestContainerLogger implements Consumer<OutputFrame> {

    private final String prefix;
    private final String dockerImage;

    /**
     * Constructor
     *
     * @param prefix the prefix to use for the log messages
     * @param dockerImage the docker image to use for the log messages
     */
    public TestContainerLogger(String prefix, String dockerImage) {
        this.prefix = prefix;
        this.dockerImage = dockerImage;
    }

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
