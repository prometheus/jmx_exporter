package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.mortbay.servlet.GzipFilter;

import java.io.File;
import java.net.InetSocketAddress;

public class WebServer {

   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
       System.exit(1);
     }

     String[] hostnamePort = args[0].split(":");
     int port;
     InetSocketAddress socket;

     if (hostnamePort.length == 2) {
       port = Integer.parseInt(hostnamePort[1]);
       socket = new InetSocketAddress(hostnamePort[0], port);
     } else {
       port = Integer.parseInt(hostnamePort[0]);
       socket = new InetSocketAddress(port);
     }

     JmxCollector jc = new JmxCollector(new File(args[1])).register();

     Server server = new Server(socket);
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     context.addFilter(GzipFilter.class, "/*", null);
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     context.addServlet(new ServletHolder(new HealthServlet()),  "/health");
     server.start();
     server.join();
   }
}
