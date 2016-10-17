package io.prometheus.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.Counter;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * A test
 */
public class ConcurrentCollectorWrapperTest {

    private Counter c = Counter.build().name("Simple").help("Boo!").create();

    private ConcurrentCollectorWrapper wrapper = new ConcurrentCollectorWrapper();

    @Before
    public void setUp() {
        c.inc();
    }

    @Test
    public void testSimple() {

        Collector cl = wrapper.set(c);

        assertEquals(c.collect(), cl.collect());
    }

    @Test
    public void testNoBlips() throws Exception {
        Counter other = Counter.build().name("Sample").help("Boo!").create();
        other.inc();

        final AtomicInteger missingSwaps = new AtomicInteger(0);
        final CountDownLatch c = new CountDownLatch(1);
        int totalExecs = 10000;

        final CountDownLatch done = new CountDownLatch(totalExecs);
        ExecutorService svc = Executors.newFixedThreadPool(10);
        for (int i = 0; i < totalExecs; i++) {
            svc.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    try {
                        c.await();
                        List<Collector.MetricFamilySamples> list = wrapper.collect();
                        if (list == null || list.size() != 1 ) {
                            missingSwaps.incrementAndGet();
                        }

                    } catch (InterruptedException e) {
                        //ignore
                    } finally {
                        done.countDown();
                    }

                    return null;
                }
            });
        }

        c.countDown();
        wrapper.set(other);


        done.await();

        assertEquals(0, missingSwaps.get());

        assertEquals(other.collect(), wrapper.collect());
    }


}