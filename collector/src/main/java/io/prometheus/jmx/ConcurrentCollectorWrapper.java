package io.prometheus.jmx;

import io.prometheus.client.Collector;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A collector wrapper to workaround bad design of CollectorRegistry
 * This wrapper allows to replace one collector with another collector without affecting underlying collection of metrics
 */
public class ConcurrentCollectorWrapper extends Collector {

    private AtomicReference<Collector> wrapperAround = new AtomicReference<Collector>();

    public ConcurrentCollectorWrapper () {
    }

    /**
     * Allows setting wrapped collector with another instance
     * @param other
     */
    public Collector set (Collector other) {
        if (other == null) {
            throw new IllegalArgumentException("Can not be null");
        }
        wrapperAround.set(other);

        return this;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        if (wrapperAround.get() == null) {
            throw new IllegalArgumentException("Collector was not initialised yet");
        }
        return wrapperAround.get().collect();
    }

    @Override
    public String toString() {
        return "ConcurrentCollectorWrapper{" +
                "wrapperAround=" + wrapperAround.get() +
                '}';
    }
}
