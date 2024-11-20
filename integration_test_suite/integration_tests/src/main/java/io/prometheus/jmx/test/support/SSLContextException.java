package io.prometheus.jmx.test.support;

/** Class to implement SSLContextException */
public class SSLContextException extends RuntimeException {

    /**
     * Constructor
     *
     * @param message message
     */
    public SSLContextException(String message) {
        super(message);
    }

    /**
     * Constructor
     *
     * @param message message
     * @param throwable throwable
     */
    public SSLContextException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
