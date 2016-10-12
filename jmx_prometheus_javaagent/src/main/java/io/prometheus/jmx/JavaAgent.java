package io.prometheus.jmx;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class JavaAgent {
    static Server server;

    private final static Timer watchTimer = new Timer("Prometheus config watch", true);

    private final static AtomicReference<FileInfo> lastChange = new AtomicReference<FileInfo>();

    static class FileInfo {
        final long lastChangeTs;
        final String fileName;
        final Throwable exception;

        FileInfo(long lastChangeTs, String fileName) {
            this(lastChangeTs, fileName, null);
        }

        public FileInfo(long lastChangeTs, Throwable exception) {
            this(lastChangeTs, null, exception);
        }

        public FileInfo(long lastChangeTs, String fileName, Throwable exception) {
            this.lastChangeTs = lastChangeTs;
            this.fileName = fileName;
            this.exception = exception;
        }

    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        final String[] args = agentArgument.split(":");
        if (args.length < 2) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=<port>:<yaml configuration file>[:<config watch timeout - default 30s>]");
            System.exit(1);
        }

        long timeout = 30l;

        if (args.length > 2) {
            timeout = Long.parseLong(args[2]);
        }

        final CountDownLatch jmxDone = new CountDownLatch(1);
        watchTimer.scheduleAtFixedRate(new JmxConfigurer(jmxDone, new File(args[1])), 0l, TimeUnit.SECONDS.toMillis(timeout));
        DefaultExports.initialize();

        try {
            jmxDone.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed waiting for JMX registry", e);
        }

        FileInfo info = lastChange.get();
        if (info.exception != null) {
            throw new IllegalStateException("Failed to initialise agent", info.exception);
        }

        int port = Integer.parseInt(args[0]);
        server = new Server(port);
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setDaemon(true);
        server.setThreadPool(pool);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
        server.start();
    }

    static class JmxConfigurer extends TimerTask {
        final CountDownLatch startLatch;
        final File fileToWatch;
        AtomicReference<Collector> collectorRef = new AtomicReference<Collector>();

        JmxConfigurer(CountDownLatch startLatch, File fileToWatch) {
            this.startLatch = startLatch;
            this.fileToWatch = fileToWatch;
        }

        @Override
        public void run() {
            FileReader reader = null;
            FileInfo info = null;
            try {
                FileInfo lastLoad = lastChange.get();
                long lastModified = fileToWatch.lastModified();
                if (lastLoad != null && lastLoad.lastChangeTs == lastModified) {
                    return;
                }

                //NB: logic below can result in duplicated metric blip for short time
                reader = new FileReader(fileToWatch);
                JmxCollector collector = new JmxCollector(reader);
                Collector old = collectorRef.get();
                if (old != null) {
                    CollectorRegistry.defaultRegistry.unregister(old);
                }
                collectorRef.set(collector.register());

                //NB: here we speculate that lastModified used for loading collector is same as file API returned previously
                info = new FileInfo(lastModified, fileToWatch.getCanonicalPath());
            } catch (Throwable e) {
                info = new FileInfo(new Date().getTime(), e);
            } finally {
                lastChange.set(info);
                startLatch.countDown();
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

}
