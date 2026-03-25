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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ExponentialBackoffThrottleTest {

    @Test
    void constructorThrowsWhenMinimumMillisecondsIsZero() {
        assertThatThrownBy(() -> new ExponentialBackoffThrottle(0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumMilliseconds [0] is less than 1");
    }

    @Test
    void constructorThrowsWhenMinimumMillisecondsIsNegative() {
        assertThatThrownBy(() -> new ExponentialBackoffThrottle(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumMilliseconds [-1] is less than 1");
    }

    @Test
    void constructorThrowsWhenMaximumMillisecondsIsZero() {
        assertThatThrownBy(() -> new ExponentialBackoffThrottle(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximumMilliseconds [0] is less than 1");
    }

    @Test
    void constructorThrowsWhenMaximumMillisecondsIsNegative() {
        assertThatThrownBy(() -> new ExponentialBackoffThrottle(1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximumMilliseconds [-1] is less than 1");
    }

    @Test
    void constructorThrowsWhenMinimumMillisecondsExceedsMaximumMilliseconds() {
        assertThatThrownBy(() -> new ExponentialBackoffThrottle(100, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minimumMilliseconds [100] is greater than maximumMilliseconds [99]");
    }

    @Test
    void constructorAcceptsEqualMinimumAndMaximumMilliseconds() {
        ExponentialBackoffThrottle throttle = new ExponentialBackoffThrottle(50, 50);
        assertThat(throttle).isNotNull();
    }

    @Test
    void throttleSleepsForMinimumMillisecondsOnFirstCall() throws InterruptedException {
        long minimum = 20;
        long maximum = 100;
        ExponentialBackoffThrottle throttle = new ExponentialBackoffThrottle(minimum, maximum);

        long start = System.currentTimeMillis();
        throttle.throttle();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(minimum - 5);
    }

    @Test
    void throttleDoublesThrottleMillisecondsAfterEachCall() throws InterruptedException {
        long minimum = 10;
        long maximum = 1000;
        ExponentialBackoffThrottle throttle = new ExponentialBackoffThrottle(minimum, maximum);

        long first = measureThrottleSleep(throttle, minimum);
        long second = measureThrottleSleep(throttle, minimum * 2);
        long third = measureThrottleSleep(throttle, minimum * 4);
        long fourth = measureThrottleSleep(throttle, minimum * 8);

        assertThat(second).isGreaterThanOrEqualTo(first);
        assertThat(third).isGreaterThanOrEqualTo(second);
        assertThat(fourth).isGreaterThanOrEqualTo(third);
    }

    @Test
    void throttleCapsAtMaximumMilliseconds() throws InterruptedException {
        long minimum = 10;
        long maximum = 40;
        ExponentialBackoffThrottle throttle = new ExponentialBackoffThrottle(minimum, maximum);

        long fifth = measureThrottleSleep(throttle, minimum * 16);

        assertThat(fifth).isLessThanOrEqualTo(maximum);
    }

    @Test
    void throttleStaysAtMaximumOnceReached() throws InterruptedException {
        long minimum = 10;
        long maximum = 40;
        ExponentialBackoffThrottle throttle = new ExponentialBackoffThrottle(minimum, maximum);

        for (int i = 0; i < 10; i++) {
            measureThrottleSleep(throttle, maximum);
        }

        long lastValue = measureThrottleSleep(throttle, maximum);
        assertThat(lastValue).isLessThanOrEqualTo(maximum);
    }

    private long measureThrottleSleep(ExponentialBackoffThrottle throttle, long expectedMin)
            throws InterruptedException {
        long start = System.currentTimeMillis();
        throttle.throttle();
        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isGreaterThanOrEqualTo(expectedMin - 5);
        return elapsed;
    }
}
