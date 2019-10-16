package io.prometheus.jmx;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
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
            new JmxCollector(new File(config.file)).register();
            DefaultExports.initialize();
            server = new HTTPServer(createHttpsServer(config), CollectorRegistry.defaultRegistry, true);
        }
        catch (IllegalArgumentException e) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file> " + e.getMessage());
            System.exit(1);
        }
    }

    static HttpServer createHttpsServer(Config config) throws IOException, GeneralSecurityException {
        // Load keystore.
        KeyStore ks = KeyStore.getInstance("PKCS12");
        FileInputStream fis = new FileInputStream(config.keyStorePath);
        ks.load(fis, config.keyStorePassword);

        // Configure KeyManagers.
        KeyManagerFactory.getInstance("X509").init(ks, config.keyStorePassword);

        // Init SSLContext.
        SSLContext sslContext = SSLContext.getInstance("TLS1.2");
        sslContext.init(KeyManagerFactory.getInstance("X509").getKeyManagers(), null, null);

        // Create https server.
        HttpServer server = HttpsServer.create(config.socket, 0);
        ((HttpsServer)server).setHttpsConfigurator(new HttpsConfigurator(sslContext));
        return server;
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a javaagent as
     * {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code <CONFIG>} portion.
     * @param args provided agent args
     * @param ifc default bind interface
     * @return configuration to use for our application
     */
    public static Config parseConfig(String args, String ifc) throws IOException  {
        Pattern pattern = Pattern.compile(
                "^(?:((?:[\\w.]+)|(?:\\[.+])):)?" +  // host name, or ipv4, or ipv6 address in brackets
                        "(\\d{1,5}):" +              // port
                        "(.+):" +                    // config file
                        "(.+)");                     // server config file

        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed arguments - " + args);
        }

        String givenHost = matcher.group(1);
        String givenPort = matcher.group(2);
        String givenConfigFile = matcher.group(3);
        String givenServerConfigFile = matcher.group(4);

        int port = Integer.parseInt(givenPort);

        InetSocketAddress socket;
        if (givenHost != null && !givenHost.isEmpty()) {
            socket = new InetSocketAddress(givenHost, port);
        }
        else {
            socket = new InetSocketAddress(ifc, port);
            givenHost = ifc;
        }

        Map<String, Object> serverConfig = (Map<String, Object>)new Yaml().load(
            new FileReader(givenServerConfigFile));

        boolean tlsEnabled = Boolean.parseBoolean(
            String.valueOf(serverConfig.get("serverTLSEnabled")));

        String keyStorePath = String.valueOf(serverConfig.get("serverKeyStorePath"));
        if(tlsEnabled && keyStorePath == null) {
            throw new IllegalArgumentException("serverTLS is enabled but serverKeyStorePath is missing");
        }

        char[] password;
        if (serverConfig.get("serverKeyStorePassword") == null) {
            password = new char[]{};
        } else {
            password = String.valueOf(serverConfig.get("serverKeyStorePassword")).toCharArray();
        }

        return new Config(givenHost, port, givenConfigFile, socket,
            tlsEnabled, keyStorePath, password);
    }

    static class Config {
        String host;
        int port;
        String file;
        InetSocketAddress socket;
        boolean tlsEnabled;
        String keyStorePath;
        char[] keyStorePassword;

        Config(String host, int port, String file, InetSocketAddress socket) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
        }

        Config(String host, int port, String file, InetSocketAddress socket,
            boolean tlsEnabled,
            String keyStorePath, char[] keyStorePassword) {
            this(host, port, file, socket);
            this.tlsEnabled = tlsEnabled;
            this.keyStorePath = keyStorePath;
            this.keyStorePassword = keyStorePassword;
        }
    }
}
