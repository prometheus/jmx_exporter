package io.prometheus.jmx;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Copied from {@code io.prometheus.client.exporter.HTTPServer}, we shouldn't merge this as-is, instead we should
 * release a new copy of {@code io.prometheus:simpleclient_httpserver}
 */
class NamedDaemonThreadFactory implements ThreadFactory {
  private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

  private final int poolNumber = POOL_NUMBER.getAndIncrement();
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final ThreadFactory delegate;
  private final boolean daemon;

  NamedDaemonThreadFactory(ThreadFactory delegate, boolean daemon) {
    this.delegate = delegate;
    this.daemon = daemon;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = delegate.newThread(r);
    t.setName(String.format("prometheus-http-%d-%d", poolNumber, threadNumber.getAndIncrement()));
    t.setDaemon(daemon);
    return t;
  }

  static ThreadFactory defaultThreadFactory(boolean daemon) {
    return new NamedDaemonThreadFactory(Executors.defaultThreadFactory(), daemon);
  }
}
