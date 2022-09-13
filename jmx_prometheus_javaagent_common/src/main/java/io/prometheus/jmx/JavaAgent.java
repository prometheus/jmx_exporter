package io.prometheus.jmx;

import com.sun.net.httpserver.BasicAuthenticator;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

public class JavaAgent {

    static HTTPServer server;

    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        try {
            Config config = parseConfig(agentArgument, host);

            new BuildInfoCollector().register();
            new JmxCollector(new File(config.file), JmxCollector.Mode.AGENT).register();
            DefaultExports.initialize();

            BasicAuthenticator authenticator = new SimpleAuthenticator("Basic", config.username, config.password);
            HTTPServer.Builder serverBuilder = new HTTPServer.Builder();
            serverBuilder.withAuthenticator(authenticator)
                    .withDaemonThreads(true)
                    .withInetSocketAddress(config.socket)
                    .withRegistry(CollectorRegistry.defaultRegistry);

            server = serverBuilder.build();
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
    public static Config parseConfig(String args, String ifc) {
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

        String username = null;
        String password = null;
        try {
            Map<String, Object> map = new Yaml().load(new FileReader(givenConfigFile));
            username = map.containsKey("username") ? map.get("username").toString() : null;
            password = map.containsKey("password") ? map.get("password").toString() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Config(givenHost, port, givenConfigFile, socket, username, password);
    }

    static class Config {
        String host;
        int port;
        String file;
        InetSocketAddress socket;

        String username;
        String password;

        Config(String host, int port, String file, InetSocketAddress socket, String username, String password) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
            this.username = username;
            this.password = password;
        }
    }
}
