package io.prometheus.jmx;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.File;
import java.net.InetSocketAddress;

public class WebServer {

   public static void main(String[] args) throws Exception {
     if (args.length < 2) {
       System.err.println("Usage: WebServer <[hostname:]port> <yaml configuration file>");
       System.exit(1);
     }
     JmxCollector jc = new JmxCollector(new File(args[1])).register();

     Server server = new Server(parseSocketAddress(args[0]));
     ServletContextHandler context = new ServletContextHandler();
     context.setContextPath("/");
     server.setHandler(context);
     context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
     server.start();
     server.join();
   }

  static InetSocketAddress parseSocketAddress(String address) {
    String[] hostAndPort = address.split(":");
    if (hostAndPort.length == 1) {
      return new InetSocketAddress(Integer.parseInt(hostAndPort[0]));
    } else if (hostAndPort.length == 2) {
      return new InetSocketAddress(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
    } else {
      throw new IllegalArgumentException("Can not parse '" + address + "', expected '[hostname:]port'");
    }
  }
}
