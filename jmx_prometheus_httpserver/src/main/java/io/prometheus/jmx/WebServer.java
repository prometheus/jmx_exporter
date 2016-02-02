package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import java.io.FileReader;
import java.util.logging.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class WebServer {
   private static final Logger logger = Logger.getLogger(WebServer.class.getName());
   
   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       logger.severe("Usage: WebServer <port> <yaml configuration file>");
       System.exit(1);
     }

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
