package io.prometheus.jmx;

/**
 * ShutdownHook class to handle the shutdown of an auto-closeable resource.
 *
 * <p>This class extends the Thread class and implements a shutdown hook that closes an
 * AutoCloseable resource when the application is shutting down.
 */
public class AutoClosableShutdownHook extends Thread {

    private final AutoCloseable autoCloseable;

    /**
     * Constructor for ShutdownHook.
     *
     * @param autoCloseable The AutoCloseable resource to be closed on shutdown
     */
    public AutoClosableShutdownHook(AutoCloseable autoCloseable) {
        this.autoCloseable = autoCloseable;
    }

    @Override
    public void run() {
        try {
            autoCloseable.close();
        } catch (Throwable t) {
            // INTENTIONALLY BLANK
        }
    }
}
