/*
 * Copyright (C) Verifyica project authors and contributors
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

// package org.verifyica.test.support;
package io.prometheus.jmx.test.support.util;

import static java.lang.String.format;

import java.util.concurrent.ThreadLocalRandom;

/** Class to implement Repeater */
@SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
public class Repeater {

    private final int iterations;
    private Throttle throttle;
    private ThrowableRunnable beforeEach;
    private ThrowableRunnable test;
    private ThrowableRunnable afterEach;
    private ThrowableConsumer throwableConsumer;

    /**
     * Constructor
     *
     * @param iterations iterations
     */
    public Repeater(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException(
                    format("iterations [%d] is less than 1", iterations));
        }

        this.iterations = iterations;
    }

    /**
     * Method to set a throttle time between tests
     *
     * @param milliseconds milliseconds
     * @return the Repeater
     */
    public Repeater throttle(long milliseconds) {
        if (milliseconds >= 0) {
            throttle = new FixedThrottle(milliseconds);
        }

        return this;
    }

    /**
     * Method to set a Throwable to throttle time between tests
     *
     * @param throttle throttle
     * @return the Repeater
     */
    public Repeater throttle(Throttle throttle) {
        if (throttle != null) {
            this.throttle = throttle;
        }

        return this;
    }

    /**
     * Method to run code before a test
     *
     * @param throwableRunnable throwableRunnable
     * @return the Repeater
     */
    public Repeater before(ThrowableRunnable throwableRunnable) {
        beforeEach = throwableRunnable;

        return this;
    }

    /**
     * Method to run the test. If code ran before the test fails, the test will be skipped.
     *
     * @param throwableRunnable throwableRunnable
     * @return the Repeater
     */
    public Repeater test(ThrowableRunnable throwableRunnable) {
        test = throwableRunnable;

        return this;
    }

    /**
     * Method to run code after the test. Always runs.
     *
     * @param throwableRunnable throwableRunnable
     * @return the Repeater
     */
    public Repeater after(ThrowableRunnable throwableRunnable) {
        afterEach = throwableRunnable;

        return this;
    }

    /**
     * Method to set a consumer
     *
     * @param throwableConsumer throwableConsumer
     * @return the Repeater
     */
    public Repeater accept(ThrowableConsumer throwableConsumer) {
        this.throwableConsumer = throwableConsumer;

        return this;
    }

    /**
     * Method to run the test
     *
     * @throws Throwable Throwable
     */
    public void run() throws Throwable {
        Throwable throwable = null;

        for (int i = 1; i <= iterations; i++) {
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

    /** Method to abort a test */
    public static void abort() {
        throw new RuntimeException("4dbf35e7-415c-4e66-a61e-f6a7057e382e");
    }

    /**
     * Method to rethrow a Throwable is not null
     *
     * @param throwable throwable
     * @throws Throwable Throwable
     */
    public static void rethrow(Throwable throwable) throws Throwable {
        if (throwable != null) {
            throw throwable;
        }
    }

    /** Interface to define code */
    public interface ThrowableRunnable {

        void run() throws Throwable;
    }

    /** Interface to consume the result */
    public interface ThrowableConsumer {

        void accept(int counter, Throwable throwable) throws Throwable;
    }

    /** Interface to implement a Throttle */
    public interface Throttle {

        void throttle();
    }

    /** Class to implement a fixed throttle */
    public static class FixedThrottle implements Throttle {

        private final long milliseconds;

        /**
         * Constructor
         *
         * @param milliseconds milliseconds
         */
        public FixedThrottle(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        @Override
        public void throttle() {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Class to implement a random throttle */
    public static class RandomThrottle implements Throttle {

        private final long minMilliseconds;
        private final long maxMilliseconds;

        /**
         * Constructor
         *
         * @param minMilliseconds the minimum number of milliseconds to throttle
         * @param maxMilliseconds the maximum number of milliseconds to throttle
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

        @Override
        public void throttle() {
            long sleep =
                    (minMilliseconds == maxMilliseconds)
                            ? minMilliseconds
                            : ThreadLocalRandom.current()
                                    .nextLong(minMilliseconds, maxMilliseconds + 1);
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /** Class to implement an exponential backoff throttle */
    public static class ExponentialBackoffThrottle implements Throttle {

        private final long maxMilliseconds;
        private long currentBackoff;

        /**
         * Constructor
         *
         * @param maxMilliseconds maxMilliseconds
         */
        public ExponentialBackoffThrottle(long maxMilliseconds) {
            this.maxMilliseconds = maxMilliseconds;
            this.currentBackoff = 1;
        }

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
