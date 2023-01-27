package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

import javax.management.MalformedObjectNameException;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Random;

public class JavaAgentMultiPort {
    static HTTPServer server;
    static Random RNG = new Random();
    static HashMap<Integer, Integer> failedAttempts = new HashMap<Integer, Integer>();

    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation)  {
        // Bind to all interfaces by default (this includes IPv6).
        try {
            Config config = parseConfig(agentArgument);
            new BuildInfoCollector().register();
            new JmxCollector(new File(config.configFile), JmxCollector.Mode.AGENT).register();
            DefaultExports.initialize();
            startMultipleServers(config, config.portEnd - config.portStart + 1, config.backoffMin, config.backoffMax);
        } catch (Exception e) {
            System.err.println("Example Usage: -javaagent:/path/to/JavaAgent.jar=hostname=some_host.dc,portStart=222,portEnd=333,timeout=500,backoffMin=2000,backoffMax=4000,configFile=/some/path.yaml \n" + e.getMessage());
            System.exit(1);
        }
    }

    public static InetSocketAddress findAvailableSocket(final String host, final int timeout, int port, int portLookupAttempts) throws ConnectException {
        System.out.println("Looking up free port. Checking: " + port + ", remaining ports in range: " + portLookupAttempts);
        if (portLookupAttempts == 0) {
            System.err.println("Run out of ports to try. Agent won't start.");
            return null;
        }
        try {
            if (failedAttempts.containsKey(port) && failedAttempts.get(port) >= 3) {
                /* On some occasions, the agent/webserver tried to start on one particular port and despite the port
                 * being free, it kept failing. This will make it step over the port.
                 */
                System.out.println("Skipping " + port + ", because off too many retries on it");
                port += 1;
                portLookupAttempts -= 1;
            }
            Socket socket = new Socket();
            final InetAddress inetAddress = InetAddress.getByName(host);
            final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, port);
            // if it will make the connection, it means something else is running on that port already
            socket.connect(inetSocketAddress, timeout);
            System.out.println("Port " + port + " is used. Trying next one.");
            socket.close();
            return findAvailableSocket(host, timeout, port + 1, portLookupAttempts - 1);
        } catch (IOException e) {
            // when it fails to make connection the socket, it is considered as free.
            System.out.println("Found free port " + port);
            return JavaAgentMultiPort.createInetSocket(host, port);
        }
    }

    /**
     * Starts number of agents/servers that provides metrics
     *
     * @param config  config
     * @param retries indicating how many times the agent will attempt to launch after a 'port collision' with another
     *                agent. Value is set to the same number as ports in the range.
     * @throws IOException
     * @throws InterruptedException
     */
    public static void startMultipleServers(Config config, int retries, int minBackoff, int maxBackoff) throws IOException, InterruptedException {
        int portLookupAttemnpts = config.portEnd - config.portStart + 1;
        // Randomize start to prevent race conditions on startup
        if (portLookupAttemnpts > 1) {
            backoff(minBackoff, maxBackoff, "server start");
        }
        /*
         * Retries indicate how many times the agent will try to start (including port search). Sometimes a port is
         * selected, but more than 1 agent has picked it and only 1 will get it. The ones who lose out, will be given
         * the <retries> to start again.
         *
         * portLookupAttempts is "just" for scanning the range of ports.
         */
        InetSocketAddress socketUpdate = findAvailableSocket(config.host, config.timeout, config.portStart, portLookupAttemnpts);
        config.setSocket(socketUpdate); //this does not update config.port
        if (config.socket != null) {
            try {
                System.out.println("Trying to start JMX agent on " + config.socket.getPort());
                server = new HTTPServer(config.socket, CollectorRegistry.defaultRegistry, true);
                System.out.println("Started JMX on " + config.socket.getPort() + ". (retries left: " + retries + ")");
            } catch (IOException e) {
                if (retries <= 0) {
                    System.out.println("Ran out of retries.");
                    throw e;
                }
                System.out.println("Port has been taken before server started - retrying to start." + "(retries left: " + retries + ")");
                /*
                 * This has to be introduced, because of competition for resources. There is 'n' number of executors/agents
                 * which will compete for first port in range. It can happen that port is determined to be free, but
                 * before server start, it will be taken both a different agent. Each agent acts independently.
                 */
                if (failedAttempts.containsKey(config.socket.getPort())) {
                    failedAttempts.put(config.socket.getPort(), failedAttempts.get(config.socket.getPort()) + 1);
                } else {
                    failedAttempts.put(config.socket.getPort(), 1);
                }
                System.out.println("Start on port " + config.socket.getPort() + " failed " + failedAttempts.get(config.socket.getPort()) + "x");
                startMultipleServers(config, retries - 1, minBackoff, maxBackoff);
            }
        } else {
            throw new IOException("Cannot start server - all ports are used");
        }
    }

    private static void backoff(int min, int max, String source) throws InterruptedException {
        int backoff = RNG.nextInt(max - min + 1) + min;
        System.out.println("Backing off at " + source + " for " + backoff + "ms");
        Thread.sleep(backoff);
    }

    public static InetSocketAddress createInetSocket(String givenHost, int port) {

        InetSocketAddress socket;
        if (givenHost != null && !givenHost.isEmpty()) {
            socket = new InetSocketAddress(givenHost, port);
        } else {
            socket = new InetSocketAddress("0.0.0.0", port);
        }
        return socket;
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a javaagent as
     * {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code <CONFIG>} portion.
     *
     * @param args provided agent args
     * @return configuration to use for our application
     */
    public static Config parseConfig(String args) {
        String[] requiredParameters = "portStart,portEnd,configFile".split(",");

        String[] argsSplits = args.split(",");
        HashMap<String, String> configMap = new HashMap<String, String>();
        for (String arg : argsSplits) {
            String[] kv = arg.split("=");
            configMap.put(kv[0], kv[1]);
        }

        for (String requiredParameter : requiredParameters) {
            if (!configMap.containsKey(requiredParameter)) {
                throw new IllegalArgumentException("Argument " + requiredParameter + " is missing");
            }
        }

        // Defaults
        if (!configMap.containsKey("timeout")) {
            configMap.put("timeout", "500");
        }

        if (!configMap.containsKey("backoffMin")) {
            configMap.put("backoffMin", "2000");
        }

        if (!configMap.containsKey("backoffMax")) {
            configMap.put("backoffMax", "4000");
        }

        return new Config(configMap.get("hostname"),
                Integer.parseInt(configMap.get("portStart")),
                Integer.parseInt(configMap.get("portEnd")),
                Integer.parseInt(configMap.get("timeout")),
                Integer.parseInt(configMap.get("backoffMin")),
                Integer.parseInt(configMap.get("backoffMax")),
                configMap.get("configFile"),
                createInetSocket(configMap.get("hostname"), Integer.parseInt(configMap.get("portStart"))));
    }

    static class Config {
        String host;
        int portStart;
        int portEnd;
        int timeout;
        int backoffMin;
        int backoffMax;
        String configFile;
        InetSocketAddress socket;

        Config(String host, int portStart, int portEnd, int timeout, int backoffMin, int backoffMax, String configFile, InetSocketAddress socket) {
            this.host = host;
            this.portStart = portStart;
            this.portEnd = portEnd;
            this.timeout = timeout;
            this.backoffMin = backoffMin;
            this.backoffMax = backoffMax;
            this.configFile = configFile;
            this.socket = socket;
        }

        public void setSocket(InetSocketAddress socket) {
            this.socket = socket;
        }

        @Override
        public String toString() {
            return MessageFormat.format("Address: {0}:{1}{2} configFile: {3}, timout: {4}, backoff (min: {5}, max: {6})", host, portStart, portEnd, configFile, timeout, backoffMin, backoffMax);
        }
    }
}
