package io.prometheus.jmx;

public interface Consumer<T> {
    void accept(T t);
}
