package io.prometheus.jmx.test.support;

import okhttp3.Headers;

import java.util.function.Consumer;

public interface HeadersConsumer extends Consumer<Headers> {

    void accept(Headers headers);
}
