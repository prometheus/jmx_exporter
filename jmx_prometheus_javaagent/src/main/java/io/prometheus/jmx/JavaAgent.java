package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import java.lang.instrument.Instrumentation;
import java.io.FileReader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JavaAgent {
   static Server server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length < 2 || args.length > 3) {
       System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=<port>:<yaml configuration file>[:<context root>]");
       System.exit(1);
     }
     int port = Integer.parseInt(args[0]);
     String configurationFile = args[1];
     String contextRoot = args.length > 2 ? args[2] : "/metrics";

     new JmxCollector(new FileReader(configurationFile)).register();
     DefaultExports.initialize();

     server = new Server(port);
     QueuedThreadPool pool = new QueuedThreadPool();
     pool.setDaemon(true);
     server.setThreadPool(pool);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), contextRoot);
     server.start();
   }
}
