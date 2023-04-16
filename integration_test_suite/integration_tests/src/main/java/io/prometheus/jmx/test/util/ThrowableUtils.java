package io.prometheus.jmx.test.util;

public class ThrowableUtils {

    public static void throwUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            throw ((RuntimeException) t);
        } else {
            throw new RuntimeException(t);
        }
    }
}
