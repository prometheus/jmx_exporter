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
            // DO NOTHING
        }

        throttleMilliseconds = throttleMilliseconds * 2;
        if (throttleMilliseconds > maximumMilliseconds) {
            throttleMilliseconds = maximumMilliseconds;
        }
    }
}
