package io.prometheus.jmx.test.support;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ulimit;
import java.util.function.Consumer;

/** Class to implement ConfigureContainerCmd */
public class TestContainerConfigureCmd implements Consumer<CreateContainerCmd> {

    private static final long MEMORY_BYTES = 1073741824;
    private static final long MEMORY_SWAP_BYTES = 2 * MEMORY_BYTES;

    private static final String NOFILE = "nofile";
    private static final long UFILE_SOFT = 65536L;
    private static final long UFILE_HARD = 65536L;

    private static final TestContainerConfigureCmd SINGLETON = new TestContainerConfigureCmd();

    /** Constructor */
    private TestContainerConfigureCmd() {
        // INTENTIONALLY BLANK
    }

    @Override
    public void accept(CreateContainerCmd createContainerCmd) {
        HostConfig hostConfig = createContainerCmd.getHostConfig();
        if (hostConfig != null) {
            hostConfig
                    .withMemory(MEMORY_BYTES)
                    .withMemorySwap(MEMORY_SWAP_BYTES)
                    .withUlimits(new Ulimit[] {new Ulimit(NOFILE, UFILE_SOFT, UFILE_HARD)});
        }
    }

    /**
     * Method to get the singleton instance of TestContainerConfigureCmd
     *
     * @return the singleton instance of TestContainerConfigureCmd
     */
    public static TestContainerConfigureCmd getInstance() {
        return SINGLETON;
    }
}
