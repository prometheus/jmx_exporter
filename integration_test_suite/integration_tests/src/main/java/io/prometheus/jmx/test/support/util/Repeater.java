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

import static java.lang.String.format;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Repeats a test action for a configurable number of iterations with optional
 * before/after hooks, throttling, and error handling between runs.
 */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class Repeater {

    private final int iterations;
    private Throttle throttle;
    private ThrowableRunnable beforeEach;
    private ThrowableRunnable test;
    private ThrowableRunnable afterEach;
    private ThrowableConsumer throwableConsumer;

    /**
     * Creates a repeater that will run the test the specified number of times.
     *
     * @param iterations the number of iterations to run; must be at least 1
     * @throws IllegalArgumentException if iterations is less than 1
     */
    public Repeater(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException(format("iterations [%d] is less than 1", iterations));
        }

        this.iterations = iterations;
    }

    /**
     * Sets a fixed throttle delay in milliseconds between test iterations.
     *
     * @param milliseconds the delay in milliseconds; if negative, the throttle is not set
     * @return this repeater for method chaining
     */
    public Repeater throttle(long milliseconds) {
        if (milliseconds >= 0) {
            throttle = new FixedThrottle(milliseconds);
        }

        return this;
    }

    /**
     * Sets a custom throttle strategy for controlling delays between iterations.
     *
     * @param throttle the throttle strategy; if {@code null}, the throttle is not changed
     * @return this repeater for method chaining
     */
    public Repeater throttle(Throttle throttle) {
        if (throttle != null) {
            this.throttle = throttle;
        }

        return this;
    }

    /**
     * Sets a callback to run before each test iteration.
     *
     * @param throwableRunnable the callback to execute before each iteration
     * @return this repeater for method chaining
     */
    public Repeater before(ThrowableRunnable throwableRunnable) {
        beforeEach = throwableRunnable;

        return this;
    }

    /**
     * Sets the test action to execute on each iteration.
     *
     * @param throwableRunnable the test action to run
     * @return this repeater for method chaining
     */
    public Repeater test(ThrowableRunnable throwableRunnable) {
        test = throwableRunnable;

        return this;
    }

    /**
     * Sets a callback to run after each test iteration, executed even if the test throws.
     *
     * @param throwableRunnable the callback to execute after each iteration
     * @return this repeater for method chaining
     */
    public Repeater after(ThrowableRunnable throwableRunnable) {
        afterEach = throwableRunnable;

        return this;
    }

    /**
     * Sets a consumer that receives the iteration number and any thrown exception after each iteration.
     *
     * @param throwableConsumer the consumer for iteration results
     * @return this repeater for method chaining
     */
    public Repeater accept(ThrowableConsumer throwableConsumer) {
        this.throwableConsumer = throwableConsumer;

        return this;
    }

    /**
     * Executes the test for the configured number of iterations, running before/after hooks
     * and applying throttling between iterations.
     *
     * @throws Throwable if the test or after-hook throws and no consumer is set
     */
    public void run() throws Throwable {
        Throwable throwable = null;

        for (int i = 1; i <= iterations; i++) {
            throwable = null;
            try {
                if (beforeEach != null) {
                    beforeEach.run();
                }
                test.run();
            } catch (Throwable t) {
                throwable = t;
            } finally {
                try {
                    if (afterEach != null) {
                        afterEach.run();
                    }
                } catch (Throwable t) {
                    throwable = throwable != null ? throwable : t;
                }
            }

            if (throwableConsumer != null) {
                try {
                    throwableConsumer.accept(i, throwable);
                } catch (RuntimeException e) {
                    if ("4dbf35e7-415c-4e66-a61e-f6a7057e382e".equals(e.getMessage())) {
                        // break
                        return;
                    } else {
                        throw e;
                    }
                }
            } else if (throwable != null) {
                throw throwable;
            }

            if (i < iterations && throttle != null) {
                throttle.throttle();
            }
        }

        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * Aborts the current repeater run by throwing a runtime exception.
     *
     * @throws RuntimeException always, to signal early termination
     */
    public static void abort() {
        throw new RuntimeException("4dbf35e7-415c-4e66-a61e-f6a7057e382e");
    }

    /**
     * Rethrows the specified throwable if it is not {@code null}.
     *
     * @param throwable the throwable to rethrow, or {@code null} to do nothing
     * @throws Throwable the specified throwable if it is not {@code null}
     */
    public static void rethrow(Throwable throwable) throws Throwable {
        if (throwable != null) {
            throw throwable;
        }
    }

    /**
     * A functional interface for code that may throw a checked exception.
     */
    public interface ThrowableRunnable {

        /**
         * Executes the action.
         *
         * @throws Throwable if the action fails
         */
        void run() throws Throwable;
    }

    /**
     * A functional interface for consuming iteration results with the ability to throw checked exceptions.
     */
    public interface ThrowableConsumer {

        /**
         * Accepts the iteration number and any exception that occurred during that iteration.
         *
         * @param counter the 1-based iteration number
         * @param throwable the exception thrown during the iteration, or {@code null} if none
         * @throws Throwable if the consumer fails
         */
        void accept(int counter, Throwable throwable) throws Throwable;
    }

    /**
     * Controls the delay between repeated test iterations.
     */
    public interface Throttle {

        /**
         * Blocks the current thread for the configured delay.
         */
        void throttle();
    }

    /**
     * Throttles execution with a fixed delay between iterations.
     */
    public static class FixedThrottle implements Throttle {

        private final long milliseconds;

        /**
         * Creates a fixed throttle with the specified delay.
         *
         * @param milliseconds the delay in milliseconds between iterations
         */
        public FixedThrottle(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        /**
         * Blocks the current thread for the configured fixed delay.
         */
        @Override
        public void throttle() {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Throttles execution with a random delay between a minimum and maximum value.
     */
    public static class RandomThrottle implements Throttle {

        private final long minMilliseconds;
        private final long maxMilliseconds;

        /**
         * Creates a random throttle with the specified delay range.
         *
         * @param minMilliseconds the minimum delay in milliseconds; must be non-negative
         * @param maxMilliseconds the maximum delay in milliseconds; must be at least minMilliseconds
         * @throws IllegalArgumentException if minMilliseconds is negative or maxMilliseconds is less than minMilliseconds
         */
        public RandomThrottle(long minMilliseconds, long maxMilliseconds) {
            if (minMilliseconds < 0) {
                throw new IllegalArgumentException("minMilliseconds must be >= 0");
            }

            if (maxMilliseconds < minMilliseconds) {
                throw new IllegalArgumentException("maxMilliseconds must be >= minMilliseconds");
            }

            this.minMilliseconds = minMilliseconds;
            this.maxMilliseconds = maxMilliseconds;
        }

        /**
         * Blocks the current thread for a random duration within the configured range.
         */
        @Override
        public void throttle() {
            long sleep = (minMilliseconds == maxMilliseconds)
                    ? minMilliseconds
                    : ThreadLocalRandom.current().nextLong(minMilliseconds, maxMilliseconds + 1);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Throttles execution with an exponential backoff delay, starting at 1 millisecond
     * and doubling on each call up to the specified maximum.
     */
    public static class ExponentialBackoffThrottle implements Throttle {

        private final long maxMilliseconds;
        private long currentBackoff;

        /**
         * Creates an exponential backoff throttle with the specified maximum delay.
         *
         * @param maxMilliseconds the maximum delay in milliseconds that the backoff will not exceed
         */
        public ExponentialBackoffThrottle(long maxMilliseconds) {
            this.maxMilliseconds = maxMilliseconds;
            this.currentBackoff = 1;
        }

        /**
         * Blocks the current thread for the current backoff delay, then doubles the delay
         * for the next call up to the configured maximum.
         */
        @Override
        public void throttle() {
            try {
                Thread.sleep(currentBackoff);
                currentBackoff = Math.min(currentBackoff * 2, maxMilliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
