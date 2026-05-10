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

/**
 * Throttles execution with an exponential backoff delay, doubling the wait time on each call
 * up to a configured maximum.
 */
@SuppressWarnings("PMD.EmptyCatchBlock")
public class ExponentialBackoffThrottle implements Throttle {

    private final long maximumMilliseconds;
    private long throttleMilliseconds;

    /**
     * Creates an exponential backoff throttle starting at the specified minimum delay
     * and capping at the specified maximum delay.
     *
     * @param minimumMilliseconds the initial delay in milliseconds for the first throttle call
     * @param maximumMilliseconds the maximum delay in milliseconds that the backoff will not exceed
     * @throws IllegalArgumentException if either value is less than 1, or if minimum exceeds maximum
     */
    public ExponentialBackoffThrottle(long minimumMilliseconds, long maximumMilliseconds) {
        if (minimumMilliseconds < 1) {
            throw new IllegalArgumentException("minimumMilliseconds [" + minimumMilliseconds + "] is less than 1");
        }

        if (maximumMilliseconds < 1) {
            throw new IllegalArgumentException("maximumMilliseconds [" + maximumMilliseconds + "] is less than 1");
        }

        if (minimumMilliseconds > maximumMilliseconds) {
            throw new IllegalArgumentException("minimumMilliseconds ["
                    + minimumMilliseconds
                    + "] is greater than maximumMilliseconds ["
                    + maximumMilliseconds
                    + "]");
        }

        this.throttleMilliseconds = minimumMilliseconds;
        this.maximumMilliseconds = maximumMilliseconds;
    }

    /**
     * Blocks the current thread for the current backoff delay, then doubles the delay
     * for the next call up to the configured maximum.
     */
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
