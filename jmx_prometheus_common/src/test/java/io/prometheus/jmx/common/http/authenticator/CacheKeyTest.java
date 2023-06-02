package io.prometheus.jmx.common.http.authenticator;

import org.junit.Test;

import java.util.LinkedList;

import static org.junit.Assert.assertTrue;

public class CacheKeyTest {

    @Test
    public void testEquals() {
        String username = "Prometheus";
        String password = "secret";

        CacheKey cacheKey1 = new CacheKey(username, password);
        CacheKey cacheKey2 = new CacheKey(username, password);

        assertTrue(cacheKey1.equals(cacheKey2));
    }

    @Test
    public void testList() {
        String username = "Prometheus";
        String password = "secret";
        LinkedList<CacheKey> linkedList = new LinkedList<>();

        CacheKey cacheKey1 = new CacheKey(username, password);
        linkedList.add(cacheKey1);

        CacheKey cacheKey2 = new CacheKey(username, password);
        assertTrue(linkedList.contains(cacheKey2));
    }
}
