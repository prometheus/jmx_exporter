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

package io.prometheus.jmx.test.support.throttle;

/** Class to implement ExponentialBackupThrottle */
@SuppressWarnings("PMD.EmptyCatchBlock")
public class ExponentialBackoffThrottle implements Throttle {

    private final long maximumMilliseconds;
    private long throttleMilliseconds;

    /**
     * Constructor
     *
     * @param minimumMilliseconds minimumMilliseconds
     * @param maximumMilliseconds maximumMilliseconds
     */
    public ExponentialBackoffThrottle(long minimumMilliseconds, long maximumMilliseconds) {
        if (minimumMilliseconds < 1) {
            throw new IllegalArgumentException(
                    "minimumMilliseconds [" + minimumMilliseconds + "] is less than 1");
        }

        if (maximumMilliseconds < 1) {
            throw new IllegalArgumentException(
                    "maximumMilliseconds [" + maximumMilliseconds + "] is less than 1");
        }

        if (minimumMilliseconds > maximumMilliseconds) {
            throw new IllegalArgumentException(
                    "minimumMilliseconds ["
                            + minimumMilliseconds
                            + "] is greater than maximumMilliseconds ["
                            + maximumMilliseconds
                            + "]");
        }

        this.throttleMilliseconds = minimumMilliseconds;
        this.maximumMilliseconds = maximumMilliseconds;
    }

    @Override
    public void throttle() {
        try {
            Thread.sleep(throttleMilliseconds);
        } catch (InterruptedException e) {
            // INTENTIONALLY BLANK
        }

        throttleMilliseconds = throttleMilliseconds * 2;
        if (throttleMilliseconds > maximumMilliseconds) {
            throttleMilliseconds = maximumMilliseconds;
        }
    }
}
