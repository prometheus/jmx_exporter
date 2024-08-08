package io.prometheus.jmx.test.support.throttle;

/** Interface to implement Throttle */
public interface Throttle {

    /** Method to throttle the current thread */
    void throttle();
}
