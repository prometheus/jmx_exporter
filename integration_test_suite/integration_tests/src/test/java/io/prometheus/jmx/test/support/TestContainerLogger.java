package io.prometheus.jmx.test.support;

import java.util.function.Consumer;
import org.testcontainers.containers.output.OutputFrame;

/** Class to implement SystemOutOutputFrameLogger */
public class TestContainerLogger implements Consumer<OutputFrame> {

    /** Singleton instance */
    private static final TestContainerLogger SINGLETON = new TestContainerLogger();

    /** Constructor */
    private TestContainerLogger() {
        // INTENTIONALLY BLANK
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String string = outputFrame.getUtf8StringWithoutLineEnding().trim();
            if (!string.isBlank()) {
                System.out.println("> " + string);
            }
        }
    }

    /**
     * Method to get the singleton instance of SystemOutOutputFrameLogger
     *
     * @return the singleton instance of SystemOutOutputFrameLogger
     */
    public static TestContainerLogger getInstance() {
        return SINGLETON;
    }
}
