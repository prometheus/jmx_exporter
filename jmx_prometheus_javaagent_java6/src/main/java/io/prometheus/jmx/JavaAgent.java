package io.prometheus.jmx;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
public class JavaAgent {

    static HTTPServer server;

    public static void agentmain(String agentArgument, Instrumentation instrumentation, String[] args) throws Exception {
        premain(agentArgument, instrumentation, args);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation, String[] args) throws Exception {
        // Bind to all interfaces by default (this includes IPv6).
        String host = "0.0.0.0";

        try {
            Config config = parseConfig(agentArgument, host);

            new BuildInfoCollector().register();
            new JmxCollector(new File(config.file), JmxCollector.Mode.AGENT).register();
            DefaultExports.initialize();
            // server = new HTTPServer(config.socket, CollectorRegistry.defaultRegistry,
            // true);
            if (args[2].equals("tls"))  {
                HttpsServer httpsServer = HttpsServer.create(config.socket, 3);
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                // to handle keystore types other than jks
                String kstype = System.getProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
                KeyStore ks = KeyStore.getInstance(kstype);
                // to avoid Null pointer when password is used instead of passphrase
                char[] passphrase = System.getProperty("javax.net.ssl.keyStore.passphrase",
                        System.getProperty("javax.net.ssl.keyStorePassword")).toCharArray();
                ks.load(new FileInputStream(System.getProperty("javax.net.ssl.keyStore")), passphrase);
                kmf.init(ks, passphrase);
                sslContext.init(kmf.getKeyManagers(), null, null);
                SSLParameters sslParameters = sslContext.getDefaultSSLParameters();
                sslParameters.setProtocols(new String[] { "TLSv1.2" });
                httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
                new HTTPServer(httpsServer, CollectorRegistry.defaultRegistry, false);
            } else {
                server = new HTTPServer(config.socket, CollectorRegistry.defaultRegistry, true);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file> <[tls]>"
                    + e.getMessage());
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

        return new Config(givenHost, port, givenConfigFile, socket);
    }

    static class Config {
        String host;
        int port;
        String file;
        InetSocketAddress socket;

        Config(String host, int port, String file, InetSocketAddress socket) {
            this.host = host;
            this.port = port;
            this.file = file;
            this.socket = socket;
        }
    }
}
