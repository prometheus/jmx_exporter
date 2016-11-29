package io.prometheus.jmx;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebServerTest {
    @Test
    public void testParseSocketAddress() {
        assertEquals(1234, WebServer.parseSocketAddress("1234").getPort());
        assertEquals(1234, WebServer.parseSocketAddress("localhost:1234").getPort());
        assertEquals("localhost", WebServer.parseSocketAddress("localhost:1234").getHostName());
    }
}
