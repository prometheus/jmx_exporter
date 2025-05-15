/*
 * Copyright (C) The Prometheus jmx_exporter Authors
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

package io.prometheus.jmx.test.support;

import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

/** Class to implement SystemOutOutputFrameLogger */
public class TestContainerLogger implements Consumer<OutputFrame> {

    /** Singleton instance */
    private static final TestContainerLogger SINGLETON = new TestContainerLogger();

    /** Constructor */
    private TestContainerLogger() {
        // INTENTIONALLY BLANK
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
            if (!string.isBlank()) {
                System.out.println("> " + string);
            }
        }
    }

    /**
     * Method to get the singleton instance of SystemOutOutputFrameLogger
     *
     * @return the singleton instance of SystemOutOutputFrameLogger
     */
    public static TestContainerLogger getInstance() {
        return SINGLETON;
    }
}
