package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import java.io.File;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {
   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <port> <yaml configuration file>");
       System.exit(1);
     }
     JmxCollector jc = new JmxCollector(new File(args[1])).register();

     int port = Integer.parseInt(args[0]);
     Server server = new Server(port);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
     server.join();
   }
}
