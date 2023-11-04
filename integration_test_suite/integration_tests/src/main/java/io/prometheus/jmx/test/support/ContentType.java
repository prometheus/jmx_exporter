package io.prometheus.jmx.test.support;

public class ContentType {

    public static final String TEXT_PLAIN = "text/plain";

    public static final String PROTOBUF = "application/vnd.google.protobuf; proto=io.prometheus.client.MetricFamily; encoding=delimited";

    private ContentType() {
        // DO NOTHING
    }
}
