package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

public class JavaAgent {

    static final String DEFAULT_SERVLET_PATH = "/metrics";

    static Server server;

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        String[] args = agentArgument.split(":");
        if (args.length < 2 || args.length > 4) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:[/path]:<yaml configuration file>");
            System.exit(1);
        }

        ConfigArgs config = new ConfigArgs(args);

        new JmxCollector(new File(config.file)).register();
        DefaultExports.initialize();

        server = new Server();
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setDaemon(true);
        pool.setMaxThreads(10);
        pool.setMaxQueued(10);
        pool.setName("jmx_exporter");
        server.setThreadPool(pool);
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost(config.hostname());
        connector.setPort(config.port());
        connector.setAcceptors(1);
        server.addConnector(connector);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new MetricsServlet()), config.path);
        server.start();
    }

    static class ConfigArgs {

        InetSocketAddress socket;
        String file;
        String path = DEFAULT_SERVLET_PATH;

        public ConfigArgs(String[] args) {
            int length = args.length;
            file = args[length - 1];
            if (length == 4) {
                setSocket(args[0], args[1]);
                path = args[2];
            } else if (length == 2) {
                setSocket(args[0]);
            } else if (args[1].startsWith("/")) {
                setSocket(args[0]);
                path = args[1];
            } else {
                setSocket(args[0], args[1]);
            }
        }

        String hostname() {
            return socket.getHostName();
        }

        int port() {
            return socket.getPort();
        }

        String path() {
            return path;
        }

        String file() {
            return file;
        }

        private void setSocket(String port) {
            this.socket = new InetSocketAddress(Integer.parseInt(port));
        }

        private void setSocket(String host, String port) {
            this.socket = new InetSocketAddress(host, Integer.parseInt(port));
        }

    }
}
