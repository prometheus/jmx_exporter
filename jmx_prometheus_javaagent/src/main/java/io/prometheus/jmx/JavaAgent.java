package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import java.lang.instrument.Instrumentation;
import java.io.File;
import java.net.InetSocketAddress;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.mortbay.servlet.GzipFilter;

public class JavaAgent {
   static Server server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length < 2 || args.length > 3) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file>");
       System.exit(1);
     }

     int port;
     InetSocketAddress socket;
     String file;

     if (args.length == 3) {
       port = Integer.parseInt(args[1]);
       socket = new InetSocketAddress(args[0], port);
       file = args[2];
     } else {
       port = Integer.parseInt(args[0]);
       socket = new InetSocketAddress(port);
       file = args[1];
     }

     new JmxCollector(new File(file)).register();
     DefaultExports.initialize();

     QueuedThreadPool pool = new QueuedThreadPool();
     pool.setDaemon(true);
     pool.setMaxThreads(10);
     pool.setName("jmx_exporter");
     server = new Server(pool);

     ServerConnector cnx = new ServerConnector(server);
     cnx.setReuseAddress(true);
     cnx.setHost(socket.getHostName());
     cnx.setPort(port);

     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     context.addFilter(GzipFilter.class, "/*", null);
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
   }
}
