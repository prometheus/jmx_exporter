package io.prometheus.jmx.test.support;

import java.util.function.Consumer;

public class ResponseCallback {

    private final Response response;

    public ResponseCallback(Response response) {
        this.response = response;
    }

    public ResponseCallback accept(Consumer<Response> consumer) {
        consumer.accept(response);
        return this;
    }
}
