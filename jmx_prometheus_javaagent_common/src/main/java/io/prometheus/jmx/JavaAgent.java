package io.prometheus.jmx;

import java.io.File;
import java.io.FileReader;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import org.yaml.snakeyaml.Yaml;

import static io.prometheus.jmx.Config.loadConfig;

public class JavaAgent {

    static HTTPServer server;

    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        try {
            AgentConfig agentConfig = parseConfig(agentArgument, host);

            File file = new File(agentConfig.file);
            Config config = loadConfig((Map<String, Object>)new Yaml().load(new FileReader(file)), file);

            new BuildInfoCollector().register();
            new JmxCollector(config, JmxCollector.Mode.AGENT).register();
            DefaultExports.initialize();

            int minThreads = config.minThreads == null ? 5 : config.minThreads;
            int maxThreads = config.maxThreads == null ? 5 : config.maxThreads;
            server = new HTTPServer.Builder()
                    .withInetSocketAddress(agentConfig.socket)
                    .withRegistry(CollectorRegistry.defaultRegistry)
                    .withDaemonThreads(true)
                    .withExecutorService(new ThreadPoolExecutor(minThreads, maxThreads, 0L,
                      TimeUnit.MILLISECONDS, new LinkedBlockingQueue(),
                      NamedDaemonThreadFactory.defaultThreadFactory(true)))
                    .build();
        }
        catch (IllegalArgumentException e) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file> " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a javaagent as
     * {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code <CONFIG>} portion.
     * @param args provided agent args
     * @param ifc default bind interface
     * @return configuration to use for our application
     */
    public static AgentConfig parseConfig(String args, String ifc) {
        Pattern pattern = Pattern.compile(
                "^(?:((?:[\\w.-]+)|(?:\\[.+])):)?" + // host name, or ipv4, or ipv6 address in brackets
                        "(\\d{1,5}):" +              // port
                        "(.+)");                     // config file

        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed arguments - " + args);
        }

        String givenHost = matcher.group(1);
        String givenPort = matcher.group(2);
        String givenConfigFile = matcher.group(3);

        int port = Integer.parseInt(givenPort);

        InetSocketAddress socket;
        if (givenHost != null && !givenHost.isEmpty()) {
            socket = new InetSocketAddress(givenHost, port);
        }
        else {
            socket = new InetSocketAddress(ifc, port);
            givenHost = ifc;
        }

        return new AgentConfig(givenHost, port, givenConfigFile, socket);
    }

    static class AgentConfig {
        String host;
        int port;
        String file;
        InetSocketAddress socket;

        AgentConfig(String host, int port, String file, InetSocketAddress socket) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
        }
    }
}
