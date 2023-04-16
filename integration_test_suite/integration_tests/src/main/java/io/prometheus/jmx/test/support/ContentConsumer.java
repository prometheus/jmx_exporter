package io.prometheus.jmx.test.support;

import java.util.function.Consumer;

public interface ContentConsumer extends Consumer<String> {

    void accept(String contents);
}
