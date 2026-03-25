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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RepeaterTest {

    @Test
    void constructorThrowsIllegalArgumentExceptionWhenIterationsIsZero() {
        assertThatThrownBy(() -> new Repeater(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iterations [0] is less than 1");
    }

    @Test
    void constructorThrowsIllegalArgumentExceptionWhenIterationsIsNegative() {
        assertThatThrownBy(() -> new Repeater(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iterations [-1] is less than 1");
    }

    @Test
    void throttleWithMillisecondsSetsFixedThrottle() {
        Repeater repeater = new Repeater(1);
        Repeater result = repeater.throttle(100L);
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void throttleWithNegativeMillisecondsDoesNotSetThrottle() {
        Repeater repeater = new Repeater(1);
        Repeater.Throttle originalThrottle = new Repeater.FixedThrottle(50);
        repeater.throttle(originalThrottle);
        repeater.throttle(-1L);
    }

    @Test
    void throttleWithNullThrottleDoesNotSetThrottle() {
        Repeater repeater = new Repeater(1);
        repeater.throttle((Repeater.Throttle) null);
    }

    @Test
    void throttleWithThrottleSetsThrottle() {
        Repeater repeater = new Repeater(1);
        Repeater.Throttle throttle = new Repeater.FixedThrottle(100);
        Repeater result = repeater.throttle(throttle);
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void beforeSetsBeforeEachHook() {
        Repeater repeater = new Repeater(1);
        Repeater result = repeater.before(() -> {});
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void testSetsTestHook() {
        Repeater repeater = new Repeater(1);
        Repeater result = repeater.test(() -> {});
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void afterSetsAfterEachHook() {
        Repeater repeater = new Repeater(1);
        Repeater result = repeater.after(() -> {});
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void acceptSetsThrowableConsumer() {
        Repeater repeater = new Repeater(1);
        Repeater result = repeater.accept((i, t) -> {});
        assertThat(result).isSameAs(repeater);
    }

    @Test
    void runExecutesTestSuccessfully() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        new Repeater(3).test(counter::incrementAndGet).run();
        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    void runExecutesBeforeEachBeforeTest() throws Throwable {
        AtomicInteger executionOrder = new AtomicInteger();
        new Repeater(1)
                .before(() -> executionOrder.set(1))
                .test(() -> executionOrder.set(2))
                .run();
        assertThat(executionOrder.get()).isEqualTo(2);
    }

    @Test
    void runExecutesAfterEachAfterTest() throws Throwable {
        AtomicInteger executionOrder = new AtomicInteger();
        new Repeater(1)
                .test(() -> executionOrder.set(1))
                .after(() -> executionOrder.set(2))
                .run();
        assertThat(executionOrder.get()).isEqualTo(2);
    }

    @Test
    void runExecutesAfterEachEvenWhenTestFails() throws Throwable {
        AtomicBoolean afterExecuted = new AtomicBoolean();
        new Repeater(1)
                .test(() -> {
                    throw new RuntimeException("test failure");
                })
                .after(() -> afterExecuted.set(true))
                .run();
        assertThat(afterExecuted.get()).isTrue();
    }

    @Test
    void runThrowsExceptionWhenTestFailsAndNoConsumer() {
        assertThatThrownBy(() -> new Repeater(1)
                        .test(() -> {
                            throw new RuntimeException("test failure");
                        })
                        .run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test failure");
    }

    @Test
    void runPropagatesFirstExceptionWhenTestFailsOnFirstIteration() {
        assertThatThrownBy(() -> new Repeater(3)
                        .test(() -> {
                            throw new RuntimeException("first failure");
                        })
                        .run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("first failure");
    }

    @Test
    void runConsumerReceivesExceptionOnFailure() throws Throwable {
        AtomicInteger lastThrowableCount = new AtomicInteger();
        new Repeater(3)
                .test(() -> {
                    if (lastThrowableCount.get() < 2) {
                        throw new RuntimeException("failure");
                    }
                })
                .accept((i, t) -> {
                    if (t != null) {
                        lastThrowableCount.set(i);
                        Repeater.abort();
                    }
                })
                .run();
        assertThat(lastThrowableCount.get()).isEqualTo(3);
    }

    @Test
    void abortStopsIterationEarly() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        new Repeater(100)
                .test(counter::incrementAndGet)
                .accept((i, t) -> {
                    if (i >= 5) {
                        Repeater.abort();
                    }
                })
                .run();
        assertThat(counter.get()).isEqualTo(5);
    }

    @Test
    void rethrowThrowsWhenThrowableIsNotNull() {
        RuntimeException exception = new RuntimeException("test");
        assertThatThrownBy(() -> Repeater.rethrow(exception))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("test");
    }

    @Test
    void rethrowDoesNotThrowWhenThrowableIsNull() throws Throwable {
        Repeater.rethrow(null);
    }

    @Test
    void fixedThrottleSleepsForConfiguredTime() throws Throwable {
        long milliseconds = 50;
        Repeater.FixedThrottle throttle = new Repeater.FixedThrottle(milliseconds);

        long start = System.currentTimeMillis();
        throttle.throttle();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(milliseconds - 5);
    }

    @Test
    void randomThrottleThrowsWhenMinMillisecondsIsNegative() {
        assertThatThrownBy(() -> new Repeater.RandomThrottle(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minMilliseconds must be >= 0");
    }

    @Test
    void randomThrottleThrowsWhenMaxMillisecondsIsLessThanMin() {
        assertThatThrownBy(() -> new Repeater.RandomThrottle(50, 49))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxMilliseconds must be >= minMilliseconds");
    }

    @Test
    void randomThrottleSleepsWithinRange() throws Throwable {
        long min = 10;
        long max = 20;
        Repeater.RandomThrottle throttle = new Repeater.RandomThrottle(min, max);

        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            throttle.throttle();
            long elapsed = System.currentTimeMillis() - start;

            assertThat(elapsed).isBetween(min - 5, max + 5);
        }
    }

    @Test
    void exponentialBackoffThrottleDoublesBackoff() throws Throwable {
        Repeater.ExponentialBackoffThrottle throttle = new Repeater.ExponentialBackoffThrottle(100);

        long first = measureThrottleSleep(throttle);
        long second = measureThrottleSleep(throttle);
        long third = measureThrottleSleep(throttle);

        assertThat(second).isGreaterThanOrEqualTo(first);
        assertThat(third).isGreaterThanOrEqualTo(second);
    }

    @Test
    void exponentialBackoffThrottleCapsAtMaxMilliseconds() throws Throwable {
        long max = 100;
        Repeater.ExponentialBackoffThrottle throttle = new Repeater.ExponentialBackoffThrottle(max);

        long lastValue = 0;
        for (int i = 0; i < 10; i++) {
            lastValue = measureThrottleSleep(throttle);
        }

        assertThat(lastValue).isLessThanOrEqualTo(max);
    }

    @Test
    void exponentialBackoffThrottleStartsAtOne() throws Throwable {
        Repeater.ExponentialBackoffThrottle throttle = new Repeater.ExponentialBackoffThrottle(1000);

        long first = measureThrottleSleep(throttle);
        assertThat(first).isEqualTo(1);
    }

    private long measureThrottleSleep(Repeater.ExponentialBackoffThrottle throttle) throws InterruptedException {
        long start = System.currentTimeMillis();
        throttle.throttle();
        return System.currentTimeMillis() - start;
    }
}
