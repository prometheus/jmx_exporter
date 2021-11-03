package io.prometheus.jmx;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Common interface for testing the agent as well as the http server.
 */
public interface TestSetup extends AutoCloseable {
    List<String> scrape(long timeoutMillis);
    void copyConfig(String suffix) throws IOException, URISyntaxException;
}
