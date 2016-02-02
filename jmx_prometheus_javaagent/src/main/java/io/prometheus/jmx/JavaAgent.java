package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.hotspot.DefaultExports;
import java.lang.instrument.Instrumentation;
import java.io.FileReader;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JavaAgent {
   private static final Logger logger = Logger.getLogger(JavaAgent.class.getName());
   static Server server;

   public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
     String[] args = agentArgument.split(":");
     if (args.length != 2) {
       logger.severe("Usage: -javaagent:/path/to/JavaAgent.jar=<port>:<yaml configuration file>");
       System.exit(1);
     }
     new JmxCollector(new FileReader(args[1])).register();
     DefaultExports.initialize();

     int port = Integer.parseInt(args[0]);
     server = new Server(port);
     QueuedThreadPool pool = new QueuedThreadPool();
     pool.setDaemon(true);
     server.setThreadPool(pool);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
   }
}
